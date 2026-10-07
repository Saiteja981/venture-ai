"""
Phase 8.4 — Evaluate the two-tower recommender on the test set.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import joblib
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import keras
from training.train_two_tower import L2Normalize, ScaleBy  # noqa: F401

PROJECT_ROOT = _PROJECT_ROOT
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR = PROJECT_ROOT / "saved_models"
PLOTS_DIR = SAVED_MODELS_DIR / "plots"

MODEL_PATH = SAVED_MODELS_DIR / "two_tower_recommender.keras"
PREPROCESSOR_PKL = PROCESSED_DIR / "recommendation_preprocessor.pkl"
TEST_CSV = PROCESSED_DIR / "recommendation_test.csv"
TRAIN_CSV = PROCESSED_DIR / "recommendation_train.csv"
HISTORY_JSON = SAVED_MODELS_DIR / "two_tower_history.json"
METRICS_PATH = SAVED_MODELS_DIR / "two_tower_recommender_metrics.json"

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
TARGET_COL = "compatibility_label"


def _dcg(relevances: np.ndarray) -> float:
    return float(np.sum(relevances / np.log2(np.arange(2, len(relevances) + 2))))


def main() -> None:
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"Model not found: {MODEL_PATH}. Train first.")

    preprocessors = joblib.load(PREPROCESSOR_PKL)
    model = keras.models.load_model(MODEL_PATH)

    test_df = pd.read_csv(TEST_CSV)
    train_df = pd.read_csv(TRAIN_CSV)

    candidate_startup_ids = train_df["startup_id"].unique().tolist()

    startup_feature_df = (
        train_df.drop_duplicates(subset=["startup_id"], keep="first")
        .set_index("startup_id")
        .loc[candidate_startup_ids]
    )
    st_features = preprocessors["startup"].transform(
        startup_feature_df[STARTUP_NUMERIC + STARTUP_CATEGORICAL]
    ).astype(np.float32)

    startup_tower = keras.Model(
        inputs=model.get_layer("startup_tower").input,
        outputs=model.get_layer("startup_tower").output,
    )
    investor_tower = keras.Model(
        inputs=model.get_layer("investor_tower").input,
        outputs=model.get_layer("investor_tower").output,
    )

    st_embeddings = startup_tower.predict(st_features, verbose=0)

    K_VALUES = [5, 10]
    precisions = {k: [] for k in K_VALUES}
    recalls = {k: [] for k in K_VALUES}
    hit_rates = {k: [] for k in K_VALUES}
    ndcgs = {k: [] for k in K_VALUES}
    all_scores = []

    test_investors = test_df["investor_id"].unique()

    for inv_id in test_investors:
        inv_rows = test_df[test_df["investor_id"] == inv_id]
        if inv_rows.empty:
            continue

        inv_feature_df = inv_rows.iloc[[0]]
        inv_features = preprocessors["investor"].transform(
            inv_feature_df[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL]
        ).astype(np.float32)
        inv_emb = investor_tower.predict(inv_features, verbose=0)

        sims = (st_embeddings @ inv_emb.T).reshape(-1)
        all_scores.extend(sims.tolist())

        pos_ids = set(inv_rows[inv_rows[TARGET_COL] == 1]["startup_id"])

        order = np.argsort(-sims)
        ranked_ids = [candidate_startup_ids[i] for i in order]

        for k in K_VALUES:
            top_k = ranked_ids[:k]
            hits = sum(1 for sid in top_k if sid in pos_ids)

            if pos_ids:
                precisions[k].append(hits / k)
                recalls[k].append(hits / len(pos_ids))
                hit_rates[k].append(1.0 if hits > 0 else 0.0)
                rels = np.array([1.0 if sid in pos_ids else 0.0 for sid in top_k])
                ideal = np.ones(min(len(pos_ids), k))
                idcg = _dcg(ideal)
                ndcgs[k].append(_dcg(rels) / idcg if idcg > 0 else 0.0)

    def _mean(xs: list[float]) -> float:
        return float(np.mean(xs)) if xs else 0.0

    metrics = {
        "precision_at_5": _mean(precisions[5]),
        "recall_at_5": _mean(recalls[5]),
        "ndcg_at_10": _mean(ndcgs[10]),
        "hit_rate_at_10": _mean(hit_rates[10]),
        "n_evaluated_investors": len(precisions[5]),
        "candidate_pool_size": len(candidate_startup_ids),
        "assumptions": [
            "Relevance is binary (compatibility_label == 1).",
            "Candidate pool per investor = unique startups in the training split.",
            "Investors without positives in the test split are skipped.",
            "Metrics are computed on synthetic data; they do not reflect real-world performance.",
        ],
        "synthetic_data_warning": (
            "All metrics are computed on synthetic development data and must "
            "not be interpreted as real-world recommendation quality."
        ),
    }
    with open(METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print("=" * 60)
    print("Two-Tower Recommender — Test Evaluation")
    print("=" * 60)
    print(f"Evaluated investors : {metrics['n_evaluated_investors']}")
    print(f"Candidate pool size : {metrics['candidate_pool_size']}")
    print()
    print(f"Precision@5 : {metrics['precision_at_5']:.4f}")
    print(f"Recall@5    : {metrics['recall_at_5']:.4f}")
    print(f"NDCG@10     : {metrics['ndcg_at_10']:.4f}")
    print(f"HitRate@10  : {metrics['hit_rate_at_10']:.4f}")
    print()

    if HISTORY_JSON.exists():
        with open(HISTORY_JSON, "r", encoding="utf-8") as f:
            hist = json.load(f)
        plt.figure(figsize=(8, 5))
        plt.plot(hist["loss"], label="train_loss")
        plt.plot(hist["val_loss"], label="val_loss")
        plt.xlabel("Epoch")
        plt.ylabel("Loss")
        plt.title("Two-Tower Training Loss")
        plt.legend()
        plt.grid(True)
        plt.tight_layout()
        plt.savefig(PLOTS_DIR / "two_tower_loss.png", dpi=120)
        plt.close()
        print(f"Loss curve saved to : {PLOTS_DIR / 'two_tower_loss.png'}")

    plt.figure(figsize=(8, 5))
    plt.hist(all_scores, bins=40, alpha=0.85, edgecolor="black")
    plt.xlabel("Cosine similarity")
    plt.ylabel("Count")
    plt.title("Distribution of compatibility scores (test pairs)")
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(PLOTS_DIR / "score_distribution.png", dpi=120)
    plt.close()
    print(f"Score distribution saved to : {PLOTS_DIR / 'score_distribution.png'}")
    print()
    print(f"Metrics saved to : {METRICS_PATH}")
    print("=" * 60)


if __name__ == "__main__":
    main()