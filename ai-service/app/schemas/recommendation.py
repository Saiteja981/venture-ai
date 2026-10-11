"""
Pydantic schemas for the investor ↔ startups recommendation endpoints.

Phase 22 added rule-based direct matches for the investor→startup direction.
Phase 23 adds the reverse direction (startup→investors) with the same
rule-based direct-match logic.
"""

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


# =========================================================================
# Investor → Startups (Phase 8.4 + Phase 22)
# =========================================================================


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


class DirectMatch(BaseModel):
    """One rule-based match between an investor preference and a startup field."""

    criterion: Literal["industry", "startup_stage", "business_model"]
    investor_preference: list[str]
    startup_value: str | None = Field(
        None, description="Null when the startup record is missing this field."
    )
    matched: bool
    note: str | None = Field(
        None,
        description="Populated when the match cannot be evaluated (missing data).",
    )


class RecommendedStartup(BaseModel):
    startup_id: str
    compatibility_score: float = Field(..., ge=0.0, le=100.0)
    rank: int = Field(..., ge=1)

    # Phase 22 additions (optional, backward-compatible)
    direct_matches: list[DirectMatch] | None = Field(
        None,
        description="Rule-based matches between investor preferences and the "
                    "startup's profile. Independent of the model score.",
    )
    direct_match_count: int | None = Field(
        None,
        description="Number of criteria that matched. Not a probability.",
    )


class RecommendationResponse(BaseModel):
    status: Literal["success"] = "success"
    recommendations: list[RecommendedStartup]
    count: int
    model_version: str
    disclaimer: str = (
        "Recommendations are produced by a model trained on synthetic development "
        "data and do not represent real investment guidance."
    )
    explanation_note: str | None = Field(
        None,
        description="Clarifies the difference between the learned compatibility "
                    "score and the rule-based direct matches.",
    )


# =========================================================================
# Startups → Investors (Phase 23)
# =========================================================================


class StartupProfile(BaseModel):
    """
    Startup features required by the Phase 8.4 startup preprocessor.

    Field names and order intentionally mirror StartupFeatures in
    app.schemas.prediction — but defined locally so each endpoint's
    contract can evolve independently.
    """

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "founded_year": 2022,
                "team_size": 12,
                "total_users": 25000,
                "customer_count": 850,
                "monthly_revenue": 42000.0,
                "revenue_growth_rate": 22.5,
                "total_funding": 1500000.0,
                "monthly_burn_rate": 60000.0,
                "runway_months": 18.0,
                "valuation": 8000000.0,
                "revenue_per_customer": 49.41,
                "users_per_team_member": 2083.33,
                "growth_adjusted_runway": 22.05,
                "funding_to_revenue": 35.71,
                "burn_to_revenue": 1.43,
                "industry": "SaaS",
                "business_model": "B2B SaaS",
                "startup_stage": "SEED",
            }
        }
    )

    founded_year: int = Field(..., ge=1900, le=2100)
    team_size: int = Field(..., ge=1, le=100_000)
    total_users: int = Field(..., ge=0)
    customer_count: int = Field(..., ge=0)
    monthly_revenue: float = Field(..., ge=0.0)
    revenue_growth_rate: float = Field(..., ge=-100.0, le=1000.0)
    total_funding: float = Field(..., ge=0.0)
    monthly_burn_rate: float = Field(..., ge=0.0)
    runway_months: float = Field(..., ge=0.0)
    valuation: float = Field(..., ge=0.0)
    revenue_per_customer: float = Field(..., ge=0.0)
    users_per_team_member: float = Field(..., ge=0.0)
    growth_adjusted_runway: float = Field(..., ge=0.0)
    funding_to_revenue: float = Field(..., ge=0.0)
    burn_to_revenue: float = Field(..., ge=0.0)
    industry: str = Field(..., min_length=1, max_length=100)
    business_model: str = Field(..., min_length=1, max_length=100)
    startup_stage: str = Field(..., min_length=1, max_length=50)


class InvestorDirectMatch(BaseModel):
    """One rule-based match between a startup field and an investor's preference."""

    criterion: Literal["industry", "startup_stage", "business_model"]
    startup_value: str | None = Field(
        None, description="The startup's value for this criterion."
    )
    investor_preference: list[str] = Field(
        ..., description="The investor's stated preferences for this criterion."
    )
    matched: bool
    note: str | None = Field(
        None,
        description="Populated when the match cannot be evaluated (missing data).",
    )


class RecommendedInvestor(BaseModel):
    investor_id: str
    compatibility_score: float = Field(..., ge=0.0, le=100.0)
    rank: int = Field(..., ge=1)
    direct_matches: list[InvestorDirectMatch] | None = None
    direct_match_count: int | None = None


class StartupRecommendationRequest(BaseModel):
    startup: StartupProfile
    top_k: int = Field(..., ge=1, le=20)


class StartupRecommendationResponse(BaseModel):
    status: Literal["success"] = "success"
    recommendations: list[RecommendedInvestor]
    count: int
    model_version: str
    disclaimer: str = (
        "Investor matches are produced by a model trained on synthetic development "
        "data. They reflect model-computed similarity, not real investor interest, "
        "and do not represent investment commitments or guidance."
    )
    explanation_note: str | None = Field(
        None,
        description="Clarifies the difference between the learned compatibility "
                    "score and the rule-based preference matches.",
    )