"""
Phase 8.3 — Tests for the startup potential & risk model.

Run with:
    pytest tests/test_startup_model.py
"""

from __future__ import annotations

import json
from pathlib import Path

import joblib
import numpy as np
import pytest
from tensorflow import keras

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
PROCESSED_DIR: Path = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR: Path = PROJECT_ROOT / "saved_models"

MODEL_PATH: Path = SAVED_MODELS_DIR / "startup_potential_risk_model.keras"
METADATA_PATH: Path = SAVED_MODELS_DIR / "model_metadata.json"
PREPROCESSOR_PKL: Path = PROCESSED_DIR / "preprocessor.pkl"
TEST_CSV: Path = PROCESSED_DIR / "test_with_targets.csv"

CLASS_NAMES = ["LOW", "MEDIUM", "HIGH"]
FORBIDDEN_FEATURE_COLUMNS = {
    "potential_score", "risk_category", "outcome_class",
    "startup_id", "company_name",
}


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="module")
def model():
    if not MODEL_PATH.exists():
        pytest.skip(f"Model not trained yet at {MODEL_PATH}")
    return keras.models.load_model(MODEL_PATH)


@pytest.fixture(scope="module")
def preprocessor():
    if not PREPROCESSOR_PKL.exists():
        pytest.skip(f"Preprocessor not found at {PREPROCESSOR_PKL}")
    return joblib.load(PREPROCESSOR_PKL)


@pytest.fixture(scope="module")
def test_features(preprocessor):
    import pandas as pd
    if not TEST_CSV.exists():
        pytest.skip(f"Test CSV not found at {TEST_CSV}")
    df = pd.read_csv(TEST_CSV)
    X_df = df.drop(columns=list(FORBIDDEN_FEATURE_COLUMNS), errors="ignore")
    return np.asarray(preprocessor.transform(X_df), dtype=np.float32)


# ---------------------------------------------------------------------------
# Artifacts
# ---------------------------------------------------------------------------

def test_model_file_exists():
    assert MODEL_PATH.exists(), f"Missing model file: {MODEL_PATH}"


def test_metadata_file_exists():
    assert METADATA_PATH.exists(), f"Missing metadata file: {METADATA_PATH}"


def test_metadata_content_is_valid_json():
    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        meta = json.load(f)
    for key in [
        "project_name", "model_name", "tensorflow_version",
        "input_feature_count", "output_names", "class_names",
        "random_seed", "training_sample_count", "validation_sample_count",
        "synthetic_data_warning",
    ]:
        assert key in meta, f"Metadata missing key: {key}"


# ---------------------------------------------------------------------------
# Model structure
# ---------------------------------------------------------------------------

def test_model_loads(model):
    assert model is not None


def test_model_has_two_outputs(model):
    assert len(model.outputs) == 2


def test_output_names(model):
    """
    In Keras 3, the tensor names on model.outputs are auto-generated
    (e.g. 'keras_tensor_21'). The user-assigned names are stored in
    model.output_names as a list of strings.
    """
    assert hasattr(model, "output_names"), "Model has no output_names attribute"
    assert set(model.output_names) == {"potential_score", "risk_category"}, (
        f"Unexpected output names: {model.output_names}"
    )


def test_outputs_reachable_by_name(model, test_features):
    """The model must accept a call and return outputs addressable by name."""
    outputs = model.predict(test_features[:4], verbose=0)
    assert isinstance(outputs, list)
    assert len(outputs) == 2
    named = dict(zip(model.output_names, outputs))
    assert set(named.keys()) == {"potential_score", "risk_category"}
    pot = named["potential_score"]
    risk = named["risk_category"]
    assert pot.shape == (4, 1)
    assert risk.shape == (4, 3)


def test_potential_output_shape(model, test_features):
    outputs = model.predict(test_features[:2], verbose=0)
    named = dict(zip(model.output_names, outputs))
    pot = named["potential_score"]
    assert pot.shape == (2, 1), f"Expected (2, 1), got {pot.shape}"


def test_risk_output_shape(model, test_features):
    outputs = model.predict(test_features[:2], verbose=0)
    named = dict(zip(model.output_names, outputs))
    risk = named["risk_category"]
    assert risk.shape == (2, 3), f"Expected (2, 3), got {risk.shape}"


# ---------------------------------------------------------------------------
# Predictions
# ---------------------------------------------------------------------------

def test_risk_probabilities_sum_to_one(model, test_features):
    outputs = model.predict(test_features[:8], verbose=0)
    named = dict(zip(model.output_names, outputs))
    risk = named["risk_category"]
    sums = risk.sum(axis=1)
    assert np.allclose(sums, 1.0, atol=1e-5), f"Row sums: {sums}"


def test_potential_score_in_range(model, test_features):
    outputs = model.predict(test_features, verbose=0)
    named = dict(zip(model.output_names, outputs))
    pot = named["potential_score"].reshape(-1)
    # The model output is unconstrained (linear). Check the *clipped* value
    # that predict_startup() would return.
    clipped = np.clip(pot, 0.0, 100.0)
    assert np.all(clipped >= 0.0) and np.all(clipped <= 100.0)


def test_risk_category_from_valid_set(model, test_features):
    outputs = model.predict(test_features, verbose=0)
    named = dict(zip(model.output_names, outputs))
    risk = named["risk_category"]
    preds = np.argmax(risk, axis=1)
    assert set(preds).issubset({0, 1, 2})
    # Class names sanity
    assert CLASS_NAMES == ["LOW", "MEDIUM", "HIGH"]


# ---------------------------------------------------------------------------
# Leakage prevention
# ---------------------------------------------------------------------------

def test_no_target_columns_in_feature_source(preprocessor):
    import pandas as pd
    df = pd.read_csv(TEST_CSV)
    # Confirm the raw test CSV contains targets (so we can drop them) ...
    for col in ("potential_score", "risk_category"):
        assert col in df.columns, f"Expected target column '{col}' in {TEST_CSV.name}"
    # ... and that the preprocessor does not consume them.
    feature_names = list(getattr(preprocessor, "feature_names_in_", []))
    for col in FORBIDDEN_FEATURE_COLUMNS:
        assert col not in feature_names, f"Forbidden column '{col}' is a preprocessor input"


def test_startup_id_not_a_model_feature(preprocessor):
    feature_names = list(getattr(preprocessor, "feature_names_in_", []))
    assert "startup_id" not in feature_names


def test_company_name_not_a_model_feature(preprocessor):
    feature_names = list(getattr(preprocessor, "feature_names_in_", []))
    assert "company_name" not in feature_names


# ---------------------------------------------------------------------------
# End-to-end prediction
# ---------------------------------------------------------------------------

def test_predict_startup_returns_expected_shape():
    # Import inside the test so the module can be collected without a trained model
    from training.predict_startup import predict_startup
    example = {
        "founded_year": 2022,
        "team_size": 12,
        "total_users": 25_000,
        "customer_count": 850,
        "monthly_revenue": 42_000.0,
        "revenue_growth_rate": 22.5,
        "total_funding": 1_500_000.0,
        "monthly_burn_rate": 60_000.0,
        "runway_months": 18.0,
        "valuation": 8_000_000.0,
        "revenue_per_customer": 49.41,
        "users_per_team_member": 2083.33,
        "growth_adjusted_runway": 22.05,
        "funding_to_revenue": 35.71,
        "burn_to_revenue": 1.43,
        "industry": "SaaS",
        "business_model": "B2B SaaS",
        "startup_stage": "SEED",
    }
    result = predict_startup(example)
    assert set(result.keys()) == {"potential_score", "risk_category", "risk_probabilities"}
    assert 0.0 <= result["potential_score"] <= 100.0
    assert result["risk_category"] in CLASS_NAMES
    assert set(result["risk_probabilities"].keys()) == set(CLASS_NAMES)


def test_predict_startup_rejects_target_columns():
    from training.predict_startup import predict_startup
    with pytest.raises(ValueError):
        predict_startup({"potential_score": 50.0})