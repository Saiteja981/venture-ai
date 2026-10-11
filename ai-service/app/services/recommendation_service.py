"""
Investor ↔ startups recommendation service for the FastAPI layer.

Wraps the Phase 8.4 two-tower model. It does NOT rewrite the model. The
trained model and its preprocessors are loaded lazily on first request.

The two towers are symmetric:
    compatibility = dot(startup_embedding, investor_embedding) ∈ [-1, 1]

Because the Phase 8.4 trained artifacts use a fixed candidate pool from the
training split, both directions rank only those training candidates. The
caller cannot supply arbitrary startups or investors.

Phase 22 added rule-based direct matches for the investor→startup direction.
Phase 23 adds the reverse direction (startup→investors) with the same
rule-based direct-match logic, computed from the investor's stated
preferences against the startup's profile.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

_PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

from app.schemas.recommendation import InvestorProfile, StartupProfile

MODEL_VERSION = "two_tower_recommender_v1"

_EXPLANATION_NOTE_FOR_INVESTOR = (
    "Direct matches are rule-based comparisons between your stated preferences "
    "and the startup's profile. The compatibility score is a learned similarity "
    "from the two-tower model — it is not a probability of investment."
)

_EXPLANATION_NOTE_FOR_STARTUP = (
    "Direct matches are rule-based comparisons between each investor's stated "
    "preferences and your startup profile. The compatibility score is a learned "
    "similarity from the two-tower model — it is not a prediction that the "
    "investor will invest."
)


class RecommendationUnavailable(RuntimeError):
    """Raised when the recommender artifacts are missing or broken."""


_ready = False

# --- Forward direction (investor → startups) ---
_startup_ids: list[str] = []
_startup_embeddings = None
_startup_lookup: dict[str, dict[str, Any]] = {}
_investor_encoder = None  # callable

# --- Reverse direction (startups → investors) ---
_investor_ids: list[str] = []
_investor_embeddings = None
_investor_lookup: dict[str, dict[str, Any]] = {}
_startup_encoder = None  # callable


# ---------------------------------------------------------------------------
# Shared helpers
# ---------------------------------------------------------------------------

def _split_pref(raw: str | None) -> list[str]:
    """Split a comma-separated preference string, trimmed and deduped (case-preserved)."""
    if not raw:
        return []
    parts = [p.strip() for p in raw.split(",") if p.strip()]
    seen: list[str] = []
    for p in parts:
        if p not in seen:
            seen.append(p)
    return seen


def _case_insensitive_contains(haystack: list[str], needle: str | None) -> bool:
    if not needle:
        return False
    target = needle.strip().lower()
    return any(h.strip().lower() == target for h in haystack)


def _build_startup_lookup(st_unique) -> dict[str, dict[str, Any]]:
    lookup: dict[str, dict[str, Any]] = {}
    for _, row in st_unique.iterrows():
        sid = str(row.get("startup_id"))
        lookup[sid] = {
            "industry": row.get("industry"),
            "startup_stage": row.get("startup_stage"),
            "business_model": row.get("business_model"),
        }
    return lookup


def _build_investor_lookup(inv_unique) -> dict[str, dict[str, Any]]:
    lookup: dict[str, dict[str, Any]] = {}
    for _, row in inv_unique.iterrows():
        iid = str(row.get("investor_id"))
        lookup[iid] = {
            "preferred_industries": row.get("preferred_industries"),
            "preferred_stages": row.get("preferred_stages"),
            "preferred_business_models": row.get("preferred_business_models"),
        }
    return lookup


# ---------------------------------------------------------------------------
# Direct-match computation (both directions)
# ---------------------------------------------------------------------------

def _direct_matches(
    investor: InvestorProfile, startup_profile: dict[str, Any]
) -> list[dict[str, Any]]:
    """Investor→startup direction: which of the investor's preferences match the startup?"""
    out: list[dict[str, Any]] = []

    def _eval(criterion: str, investor_pref: list[str], startup_val):
        if not investor_pref:
            return
        if not startup_val:
            out.append({
                "criterion": criterion,
                "investor_preference": investor_pref,
                "startup_value": None,
                "matched": False,
                "note": "Startup profile is missing this field; match could not be evaluated.",
            })
            return
        matched = _case_insensitive_contains(investor_pref, str(startup_val))
        out.append({
            "criterion": criterion,
            "investor_preference": investor_pref,
            "startup_value": str(startup_val),
            "matched": matched,
            "note": None,
        })

    _eval("industry", _split_pref(investor.preferred_industries),
          startup_profile.get("industry"))
    _eval("startup_stage", _split_pref(investor.preferred_stages),
          startup_profile.get("startup_stage"))
    _eval("business_model", _split_pref(investor.preferred_business_models),
          startup_profile.get("business_model"))

    return out


def _startup_to_investor_matches(
    startup_profile: dict[str, Any], investor_row: dict[str, Any]
) -> list[dict[str, Any]]:
    """Startup→investor direction: which of the investor's preferences match this startup?"""
    out: list[dict[str, Any]] = []

    def _eval(criterion: str, pref_raw, startup_val):
        prefs = _split_pref(pref_raw)
        if not prefs:
            return
        if not startup_val:
            out.append({
                "criterion": criterion,
                "startup_value": None,
                "investor_preference": prefs,
                "matched": False,
                "note": "Startup profile is missing this field; match could not be evaluated.",
            })
            return
        matched = _case_insensitive_contains(prefs, str(startup_val))
        out.append({
            "criterion": criterion,
            "startup_value": str(startup_val),
            "investor_preference": prefs,
            "matched": matched,
            "note": None,
        })

    _eval("industry", investor_row.get("preferred_industries"),
          startup_profile.get("industry"))
    _eval("startup_stage", investor_row.get("preferred_stages"),
          startup_profile.get("startup_stage"))
    _eval("business_model", investor_row.get("preferred_business_models"),
          startup_profile.get("business_model"))

    return out


# ---------------------------------------------------------------------------
# Lazy model loading
# ---------------------------------------------------------------------------

def _ensure_loaded() -> None:
    global _ready
    global _startup_ids, _startup_embeddings, _startup_lookup, _investor_encoder
    global _investor_ids, _investor_embeddings, _investor_lookup, _startup_encoder

    if _ready:
        return

    import joblib
    import numpy as np
    import keras

    from training.train_two_tower import (  # noqa: WPS433
        L2Normalize, ScaleBy,  # noqa: F401
    )

    from training.recommend import (  # noqa: WPS433
        MODEL_PATH,
        PREPROCESSOR_PKL,
        TRAIN_CSV,
        STARTUP_NUMERIC,
        STARTUP_CATEGORICAL,
        INVESTOR_NUMERIC,
        INVESTOR_CATEGORICAL,
    )
    import pandas as pd

    if not MODEL_PATH.exists() or not PREPROCESSOR_PKL.exists() or not TRAIN_CSV.exists():
        raise RecommendationUnavailable(
            "Recommendation model artifacts are unavailable."
        )

    model = keras.models.load_model(MODEL_PATH)
    preprocessors = joblib.load(PREPROCESSOR_PKL)
    train_df = pd.read_csv(TRAIN_CSV)

    startup_tower = keras.Model(
        inputs=model.get_layer("startup_tower").input,
        outputs=model.get_layer("startup_tower").output,
    )
    investor_tower = keras.Model(
        inputs=model.get_layer("investor_tower").input,
        outputs=model.get_layer("investor_tower").output,
    )

    # -------- Startup candidate pool (forward direction) --------
    st_unique = (
        train_df.drop_duplicates(subset=["startup_id"], keep="first")
        .reset_index(drop=True)
    )
    st_features = preprocessors["startup"].transform(
        st_unique[STARTUP_NUMERIC + STARTUP_CATEGORICAL]
    ).astype(np.float32)

    _startup_ids = st_unique["startup_id"].tolist()
    _startup_embeddings = startup_tower.predict(st_features, verbose=0)
    _startup_lookup = _build_startup_lookup(st_unique)

    # -------- Investor candidate pool (reverse direction) --------
    inv_unique = (
        train_df.drop_duplicates(subset=["investor_id"], keep="first")
        .reset_index(drop=True)
    )
    inv_features = preprocessors["investor"].transform(
        inv_unique[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL]
    ).astype(np.float32)

    _investor_ids = inv_unique["investor_id"].tolist()
    _investor_embeddings = investor_tower.predict(inv_features, verbose=0)
    _investor_lookup = _build_investor_lookup(inv_unique)

    # -------- Encoder closures --------

    def _encode_investor(profile: InvestorProfile):
        inv_df = pd.DataFrame([{
            "minimum_investment": profile.minimum_investment,
            "maximum_investment": profile.maximum_investment,
            "investment_experience_years": profile.investment_experience_years,
            "portfolio_size": profile.portfolio_size if profile.portfolio_size is not None else 0,
            "investor_type": profile.investor_type,
            "risk_preference": profile.risk_preference,
            "preferred_location": profile.preferred_location or "",
            "preferred_industries": profile.preferred_industries,
            "preferred_stages": profile.preferred_stages,
            "preferred_business_models": profile.preferred_business_models or "",
        }])
        X = preprocessors["investor"].transform(
            inv_df[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL]
        ).astype(np.float32)
        return investor_tower.predict(X, verbose=0)

    def _encode_startup(profile: StartupProfile):
        st_df = pd.DataFrame([{
            "founded_year": profile.founded_year,
            "team_size": profile.team_size,
            "total_users": profile.total_users,
            "customer_count": profile.customer_count,
            "monthly_revenue": profile.monthly_revenue,
            "revenue_growth_rate": profile.revenue_growth_rate,
            "total_funding": profile.total_funding,
            "monthly_burn_rate": profile.monthly_burn_rate,
            "runway_months": profile.runway_months,
            "valuation": profile.valuation,
            "revenue_per_customer": profile.revenue_per_customer,
            "users_per_team_member": profile.users_per_team_member,
            "growth_adjusted_runway": profile.growth_adjusted_runway,
            "funding_to_revenue": profile.funding_to_revenue,
            "burn_to_revenue": profile.burn_to_revenue,
            "industry": profile.industry,
            "business_model": profile.business_model,
            "startup_stage": profile.startup_stage,
        }])
        X = preprocessors["startup"].transform(
            st_df[STARTUP_NUMERIC + STARTUP_CATEGORICAL]
        ).astype(np.float32)
        return startup_tower.predict(X, verbose=0)

    _investor_encoder = _encode_investor
    _startup_encoder = _encode_startup
    _ready = True


# ---------------------------------------------------------------------------
# Public entry points
# ---------------------------------------------------------------------------

def recommend_for_investor(
    profile: InvestorProfile, top_k: int, explain: bool = True
) -> list[dict[str, Any]]:
    """Top-K startups for an investor."""
    _ensure_loaded()
    import numpy as np

    inv_emb = _investor_encoder(profile)
    sims = _startup_embeddings @ inv_emb.T
    sims = sims.reshape(-1)

    order = np.argsort(-sims)[:top_k]

    results: list[dict[str, Any]] = []
    for rank, idx in enumerate(order, start=1):
        sid = _startup_ids[int(idx)]
        score = float(np.clip((sims[idx] + 1.0) * 50.0, 0.0, 100.0))
        item: dict[str, Any] = {
            "startup_id": sid,
            "compatibility_score": round(score, 2),
            "rank": rank,
        }
        if explain:
            profile_row = _startup_lookup.get(sid, {})
            matches = _direct_matches(profile, profile_row)
            item["direct_matches"] = matches
            item["direct_match_count"] = sum(1 for m in matches if m["matched"])
        results.append(item)

    return results


def recommend_for_startup(
    profile: StartupProfile, top_k: int, explain: bool = True
) -> list[dict[str, Any]]:
    """Top-K investors for a startup."""
    _ensure_loaded()
    import numpy as np

    st_emb = _startup_encoder(profile)
    sims = _investor_embeddings @ st_emb.T
    sims = sims.reshape(-1)

    order = np.argsort(-sims)[:top_k]

    startup_record = profile.model_dump()

    results: list[dict[str, Any]] = []
    for rank, idx in enumerate(order, start=1):
        iid = _investor_ids[int(idx)]
        score = float(np.clip((sims[idx] + 1.0) * 50.0, 0.0, 100.0))
        item: dict[str, Any] = {
            "investor_id": iid,
            "compatibility_score": round(score, 2),
            "rank": rank,
        }
        if explain:
            inv_row = _investor_lookup.get(iid, {})
            matches = _startup_to_investor_matches(startup_record, inv_row)
            item["direct_matches"] = matches
            item["direct_match_count"] = sum(1 for m in matches if m["matched"])
        results.append(item)

    return results