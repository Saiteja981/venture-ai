"""Investor ↔ startups recommendation endpoints."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, Query, status

from app.schemas.recommendation import (
    RecommendationRequest,
    RecommendationResponse,
    StartupRecommendationRequest,
    StartupRecommendationResponse,
)
from app.services import recommendation_service

router = APIRouter(prefix="/api/v1/recommendations", tags=["recommendation"])


@router.post(
    "",
    response_model=RecommendationResponse,
    summary="Recommend top-K startups for an investor",
    responses={503: {"description": "Model unavailable"}},
)
def recommend_endpoint(
    request: RecommendationRequest,
    explain: bool = Query(
        True,
        description="When true, include rule-based direct matches (industry, "
                    "stage, business model) alongside the learned score.",
    ),
) -> RecommendationResponse:
    try:
        recs = recommendation_service.recommend_for_investor(
            request.investor, request.top_k, explain=explain
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
        explanation_note=(
            recommendation_service._EXPLANATION_NOTE_FOR_INVESTOR if explain else None
        ),
    )


@router.post(
    "/for-startup",
    response_model=StartupRecommendationResponse,
    summary="Recommend top-K investors for a startup",
    responses={503: {"description": "Model unavailable"}},
)
def recommend_for_startup_endpoint(
    request: StartupRecommendationRequest,
    explain: bool = Query(
        True,
        description="When true, include rule-based direct matches (industry, "
                    "stage, business model) alongside the learned score.",
    ),
) -> StartupRecommendationResponse:
    try:
        recs = recommendation_service.recommend_for_startup(
            request.startup, request.top_k, explain=explain
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

    return StartupRecommendationResponse(
        recommendations=recs,
        count=len(recs),
        model_version=recommendation_service.MODEL_VERSION,
        explanation_note=(
            recommendation_service._EXPLANATION_NOTE_FOR_STARTUP if explain else None
        ),
    )