"""
Pydantic schemas for the investor → startups recommendation endpoint.
"""

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class InvestorProfile(BaseModel):
    """Investor features required by the Phase 8.4 investor preprocessor."""

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "investor_type": "VC_FIRM",
                "preferred_industries": "AI,SaaS",
                "preferred_stages": "SEED,SERIES_A",
                "preferred_business_models": "B2B SaaS,Enterprise",
                "minimum_investment": 100000.0,
                "maximum_investment": 2000000.0,
                "risk_preference": "MEDIUM",
                "investment_experience_years": 8,
                "portfolio_size": 15,
                "preferred_location": "San Francisco",
            }
        }
    )

    investor_type: str = Field(..., min_length=1, max_length=50)
    preferred_industries: str = Field(..., min_length=1, max_length=1000)
    preferred_stages: str = Field(..., min_length=1, max_length=500)
    preferred_business_models: str | None = Field(None, max_length=500)
    minimum_investment: float = Field(..., ge=0.0)
    maximum_investment: float = Field(..., ge=0.0)
    risk_preference: str = Field(..., min_length=1, max_length=30)
    investment_experience_years: int = Field(..., ge=0, le=100)
    portfolio_size: int | None = Field(None, ge=0)
    preferred_location: str | None = Field(None, max_length=150)


class RecommendationRequest(BaseModel):
    investor: InvestorProfile
    top_k: int = Field(..., ge=1, le=20)


class RecommendedStartup(BaseModel):
    startup_id: str
    compatibility_score: float = Field(..., ge=0.0, le=100.0)
    rank: int = Field(..., ge=1)


class RecommendationResponse(BaseModel):
    status: Literal["success"] = "success"
    recommendations: list[RecommendedStartup]
    count: int
    model_version: str
    disclaimer: str = (
        "Recommendations are produced by a model trained on synthetic development "
        "data and do not represent real investment guidance."
    )