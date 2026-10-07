"""Investor → startups recommendation endpoint."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, status

from app.schemas.recommendation import (
    RecommendationRequest,
    RecommendationResponse,
)
from app.services import recommendation_service

router = APIRouter(prefix="/api/v1/recommendations", tags=["recommendation"])


@router.post(
    "",
    response_model=RecommendationResponse,
    summary="Recommend top-K startups for an investor",
    responses={
        503: {"description": "Model unavailable"},
    },
)
def recommend_endpoint(request: RecommendationRequest) -> RecommendationResponse:
    try:
        recs = recommendation_service.recommend_for_investor(
            request.investor, request.top_k
        )
    except recommendation_service.RecommendationUnavailable as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(exc),
        )
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Recommendation failed.",
        )

    return RecommendationResponse(
        recommendations=recs,
        count=len(recs),
        model_version=recommendation_service.MODEL_VERSION,
    )