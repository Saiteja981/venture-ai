"""
Phase 8.4 — Tests for the two-tower recommender.

Run with:
    pytest tests/test_recommender.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import joblib
import numpy as np
import pandas as pd
import pytest

import keras
from training.train_two_tower import L2Normalize, ScaleBy  # noqa: F401

PROJECT_ROOT = _PROJECT_ROOT
RAW_DIR = PROJECT_ROOT / "data" / "raw"
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR = PROJECT_ROOT / "saved_models"

INVESTORS_CSV = RAW_DIR / "raw_synthetic_investors.csv"
INTERACTIONS_CSV = RAW_DIR / "synthetic_interactions.csv"
TRAIN_CSV = PROCESSED_DIR / "recommendation_train.csv"
VAL_CSV = PROCESSED_DIR / "recommendation_validation.csv"
TEST_CSV = PROCESSED_DIR / "recommendation_test.csv"
PREPROCESSOR_PKL = PROCESSED_DIR / "recommendation_preprocessor.pkl"

MODEL_PATH = SAVED_MODELS_DIR / "two_tower_recommender.keras"
METADATA_PATH = SAVED_MODELS_DIR / "two_tower_recommender_metadata.json"
METRICS_PATH = SAVED_MODELS_DIR / "two_tower_recommender_metrics.json"

TARGET_COL = "compatibility_label"


@pytest.fixture(scope="module")
def model():
    if not MODEL_PATH.exists():
        pytest.skip(f"Model not trained yet at {MODEL_PATH}")
    return keras.models.load_model(MODEL_PATH)


@pytest.fixture(scope="module")
def preprocessors():
    if not PREPROCESSOR_PKL.exists():
        pytest.skip(f"Preprocessors not found at {PREPROCESSOR_PKL}")
    return joblib.load(PREPROCESSOR_PKL)


# ---------------------------------------------------------------------------
# Dataset existence
# ---------------------------------------------------------------------------

def test_investor_dataset_exists():
    assert INVESTORS_CSV.exists()


def test_interactions_dataset_exists():
    assert INTERACTIONS_CSV.exists()


def test_train_validation_test_files_exist():
    assert TRAIN_CSV.exists()
    assert VAL_CSV.exists()
    assert TEST_CSV.exists()


def test_interactions_non_empty():
    df = pd.read_csv(INTERACTIONS_CSV)
    assert len(df) > 0


def test_interactions_required_columns():
    df = pd.read_csv(INTERACTIONS_CSV)
    for col in ["startup_id", "investor_id", TARGET_COL]:
        assert col in df.columns, f"Missing column: {col}"


def test_interactions_labels_are_binary():
    df = pd.read_csv(INTERACTIONS_CSV)
    assert set(df[TARGET_COL].unique()).issubset({0, 1})


# ---------------------------------------------------------------------------
# Artifacts
# ---------------------------------------------------------------------------

def test_model_file_exists():
    assert MODEL_PATH.exists()


def test_metadata_exists():
    assert METADATA_PATH.exists()


def test_metadata_is_valid_json():
    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        meta = json.load(f)
    for key in ["embedding_dim", "startup_input_dim", "investor_input_dim",
                "training_samples", "random_seed"]:
        assert key in meta


def test_metrics_exists():
    assert METRICS_PATH.exists()


# ---------------------------------------------------------------------------
# Model structure
# ---------------------------------------------------------------------------

def test_model_loads(model):
    assert model is not None


def test_model_has_two_inputs(model):
    assert len(model.inputs) == 2


def test_startup_tower_exists(model):
    assert model.get_layer("startup_tower") is not None


def test_investor_tower_exists(model):
    assert model.get_layer("investor_tower") is not None


# ---------------------------------------------------------------------------
# Embeddings
#
# NOTE: These tests access module-level state via `import training.recommend as rec`
# and then read `rec._st_embeddings` AFTER calling `rec._load_all()`. Using
# `from training.recommend import _st_embeddings` would bind the value at
# import time (None) and would not see the reassignment performed inside
# `_load_all()`.
# ---------------------------------------------------------------------------

def test_startup_embedding_shape():
    import training.recommend as rec
    rec._load_all()
    assert rec._st_embeddings is not None
    assert rec._st_embeddings.shape[1] == 32


def test_investor_embedding_shape():
    import training.recommend as rec
    rec._load_all()
    assert rec._inv_embeddings is not None
    assert rec._inv_embeddings.shape[1] == 32


def test_embeddings_have_no_nan():
    import training.recommend as rec
    rec._load_all()
    assert not np.isnan(rec._st_embeddings).any()
    assert not np.isnan(rec._inv_embeddings).any()


def test_embeddings_are_l2_normalized():
    import training.recommend as rec
    rec._load_all()
    st_norms = np.linalg.norm(rec._st_embeddings, axis=1)
    inv_norms = np.linalg.norm(rec._inv_embeddings, axis=1)
    assert np.allclose(st_norms, 1.0, atol=1e-3)
    assert np.allclose(inv_norms, 1.0, atol=1e-3)


# ---------------------------------------------------------------------------
# Recommendations
# ---------------------------------------------------------------------------

def test_recommend_startups_for_investor_returns_results():
    import training.recommend as rec
    rec._load_all()
    inv_id = rec._inv_unique_df.iloc[0]["investor_id"]
    recs = rec.recommend_startups_for_investor(inv_id, k=5)
    assert len(recs) == 5


def test_recommend_investors_for_startup_returns_results():
    import training.recommend as rec
    rec._load_all()
    st_id = rec._st_unique_df.iloc[0]["startup_id"]
    recs = rec.recommend_investors_for_startup(st_id, k=5)
    assert len(recs) == 5


def test_recommendations_are_sorted_descending():
    import training.recommend as rec
    rec._load_all()
    inv_id = rec._inv_unique_df.iloc[0]["investor_id"]
    recs = rec.recommend_startups_for_investor(inv_id, k=5)
    scores = [r["compatibility_score"] for r in recs]
    assert scores == sorted(scores, reverse=True)


def test_recommendation_fields():
    import training.recommend as rec
    rec._load_all()
    inv_id = rec._inv_unique_df.iloc[0]["investor_id"]
    recs = rec.recommend_startups_for_investor(inv_id, k=3)
    for r in recs:
        assert set(r.keys()) == {"startup_id", "compatibility_score", "rank"}
        assert 0.0 <= r["compatibility_score"] <= 100.0
        assert 1 <= r["rank"] <= 3


def test_recommendation_deterministic():
    import training.recommend as rec
    rec._load_all()
    inv_id = rec._inv_unique_df.iloc[0]["investor_id"]
    r1 = rec.recommend_startups_for_investor(inv_id, k=5)
    r2 = rec.recommend_startups_for_investor(inv_id, k=5)
    assert r1 == r2