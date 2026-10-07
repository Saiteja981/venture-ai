from __future__ import annotations
from typing import Any
from pydantic import BaseModel


class ModelInfoResponse(BaseModel):
    startup_prediction: dict[str, Any]
    two_tower_recommender: dict[str, Any]
    pitch_analyzer: dict[str, Any]
    note: str