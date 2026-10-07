"""
Startup potential & risk prediction service for the FastAPI layer.

This module wraps the Phase 8.3 pipeline. It does NOT retrain or modify
anything — it simply loads the existing artifacts and reuses
`predict_startup()` from training/predict_startup.py.

Loading is lazy: the model and preprocessor are loaded on first request.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

_PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

from app.schemas.prediction import StartupFeatures

MODEL_VERSION = "startup_potential_risk_v1"


class StartupPredictionUnavailable(RuntimeError):
    """Raised when the underlying model artifacts are missing or broken."""


_model_ready = False


def _ensure_loaded() -> None:
    global _model_ready
    if _model_ready:
        return
    # The predict_startup module loads the model lazily on first call.
    # We import it here and check that the files it needs exist.
    from training.predict_startup import MODEL_PATH, PREPROCESSOR_PKL  # noqa: WPS433
    if not MODEL_PATH.exists():
        raise StartupPredictionUnavailable(
            "Startup prediction model is unavailable."
        )
    if not PREPROCESSOR_PKL.exists():
        raise StartupPredictionUnavailable(
            "Startup prediction preprocessor is unavailable."
        )
    _model_ready = True


def predict(features: StartupFeatures) -> dict[str, Any]:
    """Run prediction and return API-friendly fields."""
    _ensure_loaded()
    from training.predict_startup import predict_startup  # noqa: WPS433

    record = features.model_dump()
    result = predict_startup(record)

    return {
        "potential_score": float(result["potential_score"]),
        "risk_category": result["risk_category"],
        "model_version": MODEL_VERSION,
    }