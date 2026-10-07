"""
Investor → startups recommendation service for the FastAPI layer.

Wraps the Phase 8.4 two-tower model. It does NOT rewrite the model. The
trained model and its preprocessors are loaded lazily on first request.

Because the Phase 8.4 trained artifacts use a fixed candidate pool of
startups from the training split, this service ranks those candidates
for the supplied investor. The caller cannot supply arbitrary startups.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

_PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

from app.schemas.recommendation import InvestorProfile

MODEL_VERSION = "two_tower_recommender_v1"


class RecommendationUnavailable(RuntimeError):
    """Raised when the recommender artifacts are missing or broken."""


_ready = False
_startup_ids: list[str] = []
_startup_embeddings = None
_investor_encoder = None  # callable


def _ensure_loaded() -> None:
    global _ready, _startup_ids, _startup_embeddings, _investor_encoder
    if _ready:
        return

    import joblib
    import numpy as np
    import keras

    from training.train_two_tower import (  # noqa: WPS433
        L2Normalize, ScaleBy,  # noqa: F401  (needed for deserialization)
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

    st_unique = (
        train_df.drop_duplicates(subset=["startup_id"], keep="first")
        .reset_index(drop=True)
    )
    st_features = preprocessors["startup"].transform(
        st_unique[STARTUP_NUMERIC + STARTUP_CATEGORICAL]
    ).astype(np.float32)

    _startup_ids = st_unique["startup_id"].tolist()
    _startup_embeddings = startup_tower.predict(st_features, verbose=0)

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

    _investor_encoder = _encode_investor
    _ready = True


def recommend_for_investor(profile: InvestorProfile, top_k: int) -> list[dict[str, Any]]:
    _ensure_loaded()
    import numpy as np

    inv_emb = _investor_encoder(profile)          # (1, 32)
    sims = _startup_embeddings @ inv_emb.T        # (N, 1)
    sims = sims.reshape(-1)

    order = np.argsort(-sims)[:top_k]

    results: list[dict[str, Any]] = []
    for rank, idx in enumerate(order, start=1):
        # Cosine similarity in [-1, 1] → 0..100
        score = float(np.clip((sims[idx] + 1.0) * 50.0, 0.0, 100.0))
        results.append({
            "startup_id": _startup_ids[int(idx)],
            "compatibility_score": round(score, 2),
            "rank": rank,
        })
    return results