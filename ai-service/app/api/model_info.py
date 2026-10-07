"""Read-only endpoint exposing persisted model metadata and evaluation metrics."""

from __future__ import annotations

import json
from pathlib import Path

from fastapi import APIRouter, HTTPException, status

from app.schemas.model_info import ModelInfoResponse

router = APIRouter(prefix="/api/v1", tags=["model-info"])

_PROJECT_ROOT = Path(__file__).resolve().parents[2]
_SAVED_MODELS = _PROJECT_ROOT / "saved_models"


def _read_json(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Malformed JSON in {path.name}: {exc}",
        )


@router.get(
    "/model-info",
    response_model=ModelInfoResponse,
    summary="Return persisted model metadata and evaluation metrics",
)
def get_model_info() -> ModelInfoResponse:
    return ModelInfoResponse(
        startup_prediction={
            "metadata": _read_json(_SAVED_MODELS / "model_metadata.json"),
            "evaluation": _read_json(_SAVED_MODELS / "evaluation_metrics.json"),
            "training_history": _read_json(_SAVED_MODELS / "training_history.json"),
        },
        two_tower_recommender={
            "metadata": _read_json(_SAVED_MODELS / "two_tower_recommender_metadata.json"),
            "evaluation": _read_json(_SAVED_MODELS / "two_tower_recommender_metrics.json"),
            "training_history": _read_json(_SAVED_MODELS / "two_tower_history.json"),
        },
        pitch_analyzer={
            "metadata": _read_json(_SAVED_MODELS / "pitch_analyzer" / "metadata.json"),
            "evaluation": _read_json(_SAVED_MODELS / "pitch_analyzer" / "metrics.json"),
            "labels": _read_json(_SAVED_MODELS / "pitch_analyzer" / "label_mapping.json"),
        },
        note=(
            "All metrics shown are training/evaluation metrics on synthetic "
            "development data. They do not represent real-world investment performance."
        ),
    )