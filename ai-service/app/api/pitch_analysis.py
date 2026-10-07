"""Pitch deck analysis endpoint (BERT-based, Phase 8.5)."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, status

from app.schemas.pitch_analysis import PitchDeckRequest, PitchDeckResponse
from app.services.pitch_analyzer_service import analyze_pitch_deck

router = APIRouter(prefix="/api/v1/analyze", tags=["pitch-analysis"])


@router.post(
    "/pitch-deck",
    response_model=PitchDeckResponse,
    summary="Analyze a startup pitch-deck text for section completeness",
)
def analyze_pitch_deck_endpoint(request: PitchDeckRequest) -> PitchDeckResponse:
    try:
        result = analyze_pitch_deck(request.text)
    except FileNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Pitch analyzer model is unavailable.",
        ) from exc
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Pitch deck analysis failed.",
        )

    return PitchDeckResponse(
        overall_completeness_score=float(result["overall_completeness_score"]),
        sections=result["sections"],
        information_gaps=result["information_gaps"],
        summary=result["summary"],
    )