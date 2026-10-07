"""
Phase 8.4 — Top-K recommendation service.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import joblib
import numpy as np
import pandas as pd

import keras
from training.train_two_tower import L2Normalize, ScaleBy  # noqa: F401

PROJECT_ROOT = _PROJECT_ROOT
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR = PROJECT_ROOT / "saved_models"

MODEL_PATH = SAVED_MODELS_DIR / "two_tower_recommender.keras"
PREPROCESSOR_PKL = PROCESSED_DIR / "recommendation_preprocessor.pkl"
TRAIN_CSV = PROCESSED_DIR / "recommendation_train.csv"

STARTUP_NUMERIC = [
    "founded_year", "team_size", "total_users", "customer_count",
    "monthly_revenue", "revenue_growth_rate", "total_funding",
    "monthly_burn_rate", "runway_months", "valuation",
    "revenue_per_customer", "users_per_team_member",
    "growth_adjusted_runway", "funding_to_revenue", "burn_to_revenue",
]
STARTUP_CATEGORICAL = ["industry", "business_model", "startup_stage"]
INVESTOR_NUMERIC = [
    "minimum_investment", "maximum_investment",
    "investment_experience_years", "portfolio_size",
]
INVESTOR_CATEGORICAL = [
    "investor_type", "risk_preference", "preferred_location",
    "preferred_industries", "preferred_stages", "preferred_business_models",
]


_model = None
_preprocessors = None
_startup_tower = None
_investor_tower = None
_train_df = None
_st_embeddings = None
_inv_embeddings = None
_st_unique_df = None
_inv_unique_df = None


def _load_all() -> None:
    global _model, _preprocessors, _startup_tower, _investor_tower
    global _train_df, _st_embeddings, _inv_embeddings
    global _st_unique_df, _inv_unique_df

    if _model is not None:
        return

    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"Model not found: {MODEL_PATH}. Train first.")
    if not PREPROCESSOR_PKL.exists():
        raise FileNotFoundError(f"Preprocessors not found: {PREPROCESSOR_PKL}.")

    _model = keras.models.load_model(MODEL_PATH)
    _preprocessors = joblib.load(PREPROCESSOR_PKL)
    _train_df = pd.read_csv(TRAIN_CSV)

    _startup_tower = keras.Model(
        inputs=_model.get_layer("startup_tower").input,
        outputs=_model.get_layer("startup_tower").output,
    )
    _investor_tower = keras.Model(
        inputs=_model.get_layer("investor_tower").input,
        outputs=_model.get_layer("investor_tower").output,
    )

    _st_unique_df = (
        _train_df.drop_duplicates(subset=["startup_id"], keep="first")
        .reset_index(drop=True)
    )
    _inv_unique_df = (
        _train_df.drop_duplicates(subset=["investor_id"], keep="first")
        .reset_index(drop=True)
    )

    st_features = _preprocessors["startup"].transform(
        _st_unique_df[STARTUP_NUMERIC + STARTUP_CATEGORICAL]
    ).astype(np.float32)
    inv_features = _preprocessors["investor"].transform(
        _inv_unique_df[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL]
    ).astype(np.float32)

    _st_embeddings = _startup_tower.predict(st_features, verbose=0)
    _inv_embeddings = _investor_tower.predict(inv_features, verbose=0)


def _score_to_100(sim: float) -> float:
    return float(np.clip((sim + 1.0) * 50.0, 0.0, 100.0))


def recommend_startups_for_investor(investor_id: str, k: int = 5) -> list[dict]:
    _load_all()
    matches = _inv_unique_df.index[_inv_unique_df["investor_id"] == investor_id].tolist()
    if not matches:
        return []
    inv_idx = matches[0]
    inv_emb = _inv_embeddings[inv_idx]

    sims = _st_embeddings @ inv_emb
    order = np.argsort(-sims)[:k]

    results = []
    for rank, idx in enumerate(order, start=1):
        results.append({
            "startup_id": _st_unique_df.iloc[idx]["startup_id"],
            "compatibility_score": round(_score_to_100(float(sims[idx])), 2),
            "rank": rank,
        })
    return results


def recommend_investors_for_startup(startup_id: str, k: int = 5) -> list[dict]:
    _load_all()
    matches = _st_unique_df.index[_st_unique_df["startup_id"] == startup_id].tolist()
    if not matches:
        return []
    st_idx = matches[0]
    st_emb = _st_embeddings[st_idx]

    sims = _inv_embeddings @ st_emb
    order = np.argsort(-sims)[:k]

    results = []
    for rank, idx in enumerate(order, start=1):
        results.append({
            "investor_id": _inv_unique_df.iloc[idx]["investor_id"],
            "compatibility_score": round(_score_to_100(float(sims[idx])), 2),
            "rank": rank,
        })
    return results


def _demo() -> None:
    _load_all()
    sample_inv = _inv_unique_df.iloc[0]["investor_id"]
    sample_st = _st_unique_df.iloc[0]["startup_id"]

    print("=" * 60)
    print(f"Top-5 startups for investor {sample_inv}")
    print("=" * 60)
    for rec in recommend_startups_for_investor(sample_inv, k=5):
        print(rec)

    print()
    print("=" * 60)
    print(f"Top-5 investors for startup {sample_st}")
    print("=" * 60)
    for rec in recommend_investors_for_startup(sample_st, k=5):
        print(rec)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--investor", type=str, default=None)
    parser.add_argument("--startup", type=str, default=None)
    parser.add_argument("-k", type=int, default=5)
    args = parser.parse_args()

    if args.investor:
        recs = recommend_startups_for_investor(args.investor, k=args.k)
        print(f"Top-{args.k} startups for {args.investor}:")
        for r in recs:
            print(r)
    elif args.startup:
        recs = recommend_investors_for_startup(args.startup, k=args.k)
        print(f"Top-{args.k} investors for {args.startup}:")
        for r in recs:
            print(r)
    else:
        _demo()


if __name__ == "__main__":
    main()