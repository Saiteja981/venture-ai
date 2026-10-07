"""
Phase 8.3 — Prediction utility for the startup potential & risk model.

Loads the trained model and the Phase 8.2 preprocessor, and exposes a
single function:

    predict_startup(record: dict) -> dict

The input `record` may be any dict that contains the model features. It can
optionally contain `startup_id` / `company_name` (ignored). It must NOT
contain target columns (`potential_score`, `risk_category`, `outcome_class`).

Usage
-----
    python training/predict_startup.py
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
import tensorflow as tf
import keras                    # ← changed from: from tensorflow import keras

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
PROCESSED_DIR: Path = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR: Path = PROJECT_ROOT / "saved_models"

MODEL_PATH: Path = SAVED_MODELS_DIR / "startup_potential_risk_model.keras"
PREPROCESSOR_PKL: Path = PROCESSED_DIR / "preprocessor.pkl"

CLASS_NAMES = ["LOW", "MEDIUM", "HIGH"]

FORBIDDEN_INPUT = {"potential_score", "risk_category", "outcome_class",
                   "startup_id", "company_name"}

# Model feature columns expected by the Phase 8.2 preprocessor
MODEL_FEATURES = [
    "founded_year", "team_size", "total_users", "customer_count",
    "monthly_revenue", "revenue_growth_rate", "total_funding",
    "monthly_burn_rate", "runway_months", "valuation",
    "revenue_per_customer", "users_per_team_member",
    "growth_adjusted_runway", "funding_to_revenue", "burn_to_revenue",
    "industry", "business_model", "startup_stage",
]


# ---------------------------------------------------------------------------
# Loader (module-level, cached)
# ---------------------------------------------------------------------------

_model: keras.Model | None = None
_preprocessor = None


def _load_artifacts():
    global _model, _preprocessor
    if _model is None:
        if not MODEL_PATH.exists():
            raise FileNotFoundError(
                f"Model not found: {MODEL_PATH}. "
                "Run `python training/train_startup_model.py` first."
            )
        _model = keras.models.load_model(MODEL_PATH)
    if _preprocessor is None:
        if not PREPROCESSOR_PKL.exists():
            raise FileNotFoundError(f"Preprocessor not found: {PREPROCESSOR_PKL}")
        _preprocessor = joblib.load(PREPROCESSOR_PKL)
    return _model, _preprocessor


# ---------------------------------------------------------------------------
# Prediction
# ---------------------------------------------------------------------------

def predict_startup(record: dict[str, Any]) -> dict[str, Any]:
    """
    Predict potential_score and risk_category for a single startup record.

    Parameters
    ----------
    record : dict
        Must contain every column listed in MODEL_FEATURES.
        May contain extra columns (ignored).

    Returns
    -------
    dict with keys:
        potential_score     : float in [0, 100]
        risk_category       : one of "LOW", "MEDIUM", "HIGH"
        risk_probabilities  : dict {"LOW": ..., "MEDIUM": ..., "HIGH": ...}
    """
    for col in FORBIDDEN_INPUT:
        if col in record:
            raise ValueError(
                f"Input record must not contain target/metadata column '{col}'."
            )

    missing = [c for c in MODEL_FEATURES if c not in record]
    if missing:
        raise ValueError(f"Input record missing required features: {missing}")

    model, preprocessor = _load_artifacts()

    # Build a single-row DataFrame with the exact feature columns
    X_df = pd.DataFrame([{c: record[c] for c in MODEL_FEATURES}])
    X = np.asarray(preprocessor.transform(X_df), dtype=np.float32)

    pot_pred, risk_proba = model.predict(X, verbose=0)
    potential = float(np.clip(pot_pred.reshape(-1)[0], 0.0, 100.0))
    probs = risk_proba.reshape(-1).tolist()
    risk_idx = int(np.argmax(probs))

    return {
        "potential_score": round(potential, 4),
        "risk_category": CLASS_NAMES[risk_idx],
        "risk_probabilities": {
            CLASS_NAMES[i]: round(float(probs[i]), 6) for i in range(len(CLASS_NAMES))
        },
    }


# ---------------------------------------------------------------------------
# CLI demo
# ---------------------------------------------------------------------------

def _demo():
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
    print("=" * 60)
    print("Demo prediction")
    print("=" * 60)
    for k, v in result.items():
        print(f"{k}: {v}")


if __name__ == "__main__":
    _demo()