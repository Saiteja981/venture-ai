"""Startup potential & risk prediction endpoint."""

from __future__ import annotations

import traceback

from fastapi import APIRouter, HTTPException, status

from app.schemas.prediction import StartupFeatures, StartupPredictionResponse
from app.services import startup_prediction_service

router = APIRouter(prefix="/api/v1/predict", tags=["prediction"])


@router.post(
    "/startup",
    response_model=StartupPredictionResponse,
    summary="Predict startup potential and risk",
    responses={
        503: {"description": "Model unavailable"},
    },
)
def predict_startup_endpoint(features: StartupFeatures) -> StartupPredictionResponse:
    try:
        result = startup_prediction_service.predict(features)
    except startup_prediction_service.StartupPredictionUnavailable as exc:
        traceback.print_exc()
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(exc),
        )
    except Exception as exc:
        traceback.print_exc()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Startup prediction failed: {type(exc).__name__}: {exc}",
        )

    return StartupPredictionResponse(**result)