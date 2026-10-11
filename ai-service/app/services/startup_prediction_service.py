"""
Startup potential & risk prediction service for the FastAPI layer.

This module wraps the Phase 8.3 pipeline. It does NOT retrain or modify
anything — it simply loads the existing artifacts and reuses
`predict_startup()` from training/predict_startup.py.

Loading is lazy: the model and preprocessor are loaded on first request.

Phase 22 adds an optional approximate explanation pass (feature-substitution
sensitivity). The explanation is opt-in and never blocks the base prediction.
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
    from training.predict_startup import MODEL_PATH, PREPROCESSOR_PKL  # noqa: WPS433
    if not MODEL_PATH.exists():
        raise StartupPredictionUnavailable("Startup prediction model is unavailable.")
    if not PREPROCESSOR_PKL.exists():
        raise StartupPredictionUnavailable("Startup prediction preprocessor is unavailable.")
    _model_ready = True


def predict(features: StartupFeatures, explain: bool = False) -> dict[str, Any]:
    """Run prediction and return API-friendly fields.

    When `explain=True`, also runs a feature-substitution sensitivity pass
    and includes an `explanation` list. The pass is approximate and never
    raises into the caller — if it fails, only the explanation is omitted.
    """
    _ensure_loaded()
    from training.predict_startup import predict_startup  # noqa: WPS433

    record = features.model_dump()
    result = predict_startup(record)

    payload: dict[str, Any] = {
        "potential_score": float(result["potential_score"]),
        "risk_category": result["risk_category"],
        "model_version": MODEL_VERSION,
    }

    if explain:
        try:
            from app.explain.feature_contribution import (
                explain_instance,
                METHOD_LABEL,
            )

            payload["explanation"] = explain_instance(
                predict_fn=predict_startup,
                record=record,
                background=None,  # no reference cohort wired yet — see limitation note
                top_k=8,
            )
            payload["explanation_method"] = METHOD_LABEL
            payload["explanation_note"] = (
                "Approximate, non-causal. Values show how much the predicted "
                "potential_score would change if each feature were set to a "
                "low-end reference value. Categorical features are omitted "
                "when no reference cohort is available. This is not a "
                "confidence score."
            )
        except Exception:
            # Never fail the base prediction because explanation failed.
            import traceback
            traceback.print_exc()
            payload["explanation"] = None
            payload["explanation_method"] = None
            payload["explanation_note"] = (
                "Explanation was requested but could not be generated."
            )

    return payload