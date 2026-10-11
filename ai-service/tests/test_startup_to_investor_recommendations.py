"""
Unit tests for the startup → investors recommendation direction (Phase 23).

Only tests pure helper logic + schema behavior. The model is not loaded.
"""
import pytest
from pydantic import ValidationError

from app.schemas.recommendation import (
    InvestorDirectMatch,
    RecommendedInvestor,
    StartupProfile,
    StartupRecommendationRequest,
    StartupRecommendationResponse,
)
from app.services.recommendation_service import (
    _build_investor_lookup,
    _startup_to_investor_matches,
)


def _startup(**overrides):
    base = {
        "founded_year": 2022,
        "team_size": 12,
        "total_users": 25000,
        "customer_count": 850,
        "monthly_revenue": 42000.0,
        "revenue_growth_rate": 22.5,
        "total_funding": 1_500_000.0,
        "monthly_burn_rate": 60000.0,
        "runway_months": 18.0,
        "valuation": 8_000_000.0,
        "revenue_per_customer": 49.41,
        "users_per_team_member": 2083.33,
        "growth_adjusted_runway": 22.05,
        "funding_to_revenue": 35.71,
        "burn_to_revenue": 1.43,
        "industry": "SaaS",
        "business_model": "B2B SaaS",
        "startup_stage": "SEED",
    }
    base.update(overrides)
    return base


def test_startup_profile_accepts_valid_payload():
    p = StartupProfile(**_startup())
    assert p.industry == "SaaS"


def test_startup_profile_rejects_missing_required_field():
    payload = _startup()
    del payload["industry"]
    with pytest.raises(ValidationError):
        StartupProfile(**payload)


def test_startup_recommendation_request_validates_top_k():
    req = StartupRecommendationRequest(startup=_startup(), top_k=5)
    assert req.top_k == 5
    with pytest.raises(ValidationError):
        StartupRecommendationRequest(startup=_startup(), top_k=0)
    with pytest.raises(ValidationError):
        StartupRecommendationRequest(startup=_startup(), top_k=21)


def test_startup_to_investor_matches_all_aligned():
    startup = _startup(industry="AI", startup_stage="SEED", business_model="B2B SaaS")
    investor_row = {
        "preferred_industries": "AI,SaaS",
        "preferred_stages": "SEED,SERIES_A",
        "preferred_business_models": "B2B SaaS",
    }
    matches = _startup_to_investor_matches(startup, investor_row)
    assert len(matches) == 3
    assert all(m["matched"] for m in matches)


def test_startup_to_investor_matches_none_aligned():
    startup = _startup(industry="Gaming", startup_stage="SERIES_B", business_model="B2C")
    investor_row = {
        "preferred_industries": "AI,SaaS",
        "preferred_stages": "SEED",
        "preferred_business_models": "B2B SaaS",
    }
    matches = _startup_to_investor_matches(startup, investor_row)
    assert len(matches) == 3
    assert not any(m["matched"] for m in matches)


def test_startup_to_investor_matches_skips_absent_investor_preference():
    startup = _startup()
    investor_row = {
        "preferred_industries": "AI,SaaS",
        "preferred_stages": None,          # investor didn't state a stage preference
        "preferred_business_models": "B2B SaaS",
    }
    matches = _startup_to_investor_matches(startup, investor_row)
    criteria = {m["criterion"] for m in matches}
    assert "startup_stage" not in criteria
    assert "industry" in criteria
    assert "business_model" in criteria


def test_startup_to_investor_matches_missing_startup_field_has_note():
    startup = _startup(industry=None)
    investor_row = {
        "preferred_industries": "AI",
        "preferred_stages": "SEED",
        "preferred_business_models": "B2B SaaS",
    }
    matches = _startup_to_investor_matches(startup, investor_row)
    industry = next(m for m in matches if m["criterion"] == "industry")
    assert industry["matched"] is False
    assert industry["startup_value"] is None
    assert industry["note"] is not None


def test_build_investor_lookup_shape():
    import pandas as pd
    df = pd.DataFrame([
        {
            "investor_id": "I1",
            "preferred_industries": "AI,SaaS",
            "preferred_stages": "SEED",
            "preferred_business_models": "B2B SaaS",
        },
        {
            "investor_id": "I2",
            "preferred_industries": "FinTech",
            "preferred_stages": "SERIES_A",
            "preferred_business_models": "Enterprise",
        },
    ])
    lookup = _build_investor_lookup(df)
    assert set(lookup.keys()) == {"I1", "I2"}
    assert lookup["I1"]["preferred_industries"] == "AI,SaaS"


def test_recommended_investor_model_accepts_optional_matches():
    r = RecommendedInvestor(
        investor_id="I1",
        compatibility_score=72.5,
        rank=1,
    )
    assert r.direct_matches is None
    assert r.direct_match_count is None


def test_startup_recommendation_response_shape():
    resp = StartupRecommendationResponse(
        recommendations=[
            RecommendedInvestor(
                investor_id="I1", compatibility_score=72.5, rank=1,
                direct_matches=[
                    InvestorDirectMatch(
                        criterion="industry",
                        startup_value="SaaS",
                        investor_preference=["AI", "SaaS"],
                        matched=True,
                    )
                ],
                direct_match_count=1,
            )
        ],
        count=1,
        model_version="two_tower_recommender_v1",
    )
    assert resp.status == "success"
    assert resp.recommendations[0].rank == 1
    assert resp.disclaimer  # must be non-empty