"""Unit tests for rule-based direct-match computation on recommendations."""
from app.services.recommendation_service import (
    _case_insensitive_contains,
    _direct_matches,
    _split_pref,
)
from app.schemas.recommendation import InvestorProfile


def _profile(**overrides):
    base = {
        "investor_type": "VC_FIRM",
        "preferred_industries": "AI,SaaS",
        "preferred_stages": "SEED,SERIES_A",
        "preferred_business_models": "B2B SaaS",
        "minimum_investment": 100_000.0,
        "maximum_investment": 2_000_000.0,
        "risk_preference": "MEDIUM",
        "investment_experience_years": 8,
        "portfolio_size": 15,
        "preferred_location": "San Francisco",
    }
    base.update(overrides)
    return InvestorProfile(**base)


def test_split_pref_basic():
    assert _split_pref("AI, SaaS,AI") == ["AI", "SaaS"]


def test_split_pref_none_and_empty():
    assert _split_pref(None) == []
    assert _split_pref("") == []
    assert _split_pref("  ,  ") == []


def test_case_insensitive_contains():
    assert _case_insensitive_contains(["AI", "SaaS"], "ai") is True
    assert _case_insensitive_contains(["AI", "SaaS"], "AI") is True
    assert _case_insensitive_contains(["AI", "SaaS"], "Gaming") is False
    assert _case_insensitive_contains(["AI"], None) is False


def test_direct_matches_all_matched():
    investor = _profile()
    startup = {
        "industry": "AI",
        "startup_stage": "SEED",
        "business_model": "B2B SaaS",
    }
    matches = _direct_matches(investor, startup)
    assert len(matches) == 3
    assert all(m["matched"] for m in matches)
    assert all(m["note"] is None for m in matches)


def test_direct_matches_none_matched():
    investor = _profile()
    startup = {
        "industry": "Gaming",
        "startup_stage": "SERIES_B",
        "business_model": "B2C",
    }
    matches = _direct_matches(investor, startup)
    assert len(matches) == 3
    assert all(not m["matched"] for m in matches)


def test_direct_matches_missing_startup_field_has_note():
    investor = _profile()
    startup = {
        "industry": None,
        "startup_stage": "SEED",
        "business_model": "B2B SaaS",
    }
    matches = _direct_matches(investor, startup)
    industry = next(m for m in matches if m["criterion"] == "industry")
    assert industry["matched"] is False
    assert industry["startup_value"] is None
    assert industry["note"] is not None
    # Missing data is transparent — no fabricated match.


def test_direct_matches_skips_absent_investor_preference():
    investor = _profile(preferred_business_models=None)
    startup = {
        "industry": "AI",
        "startup_stage": "SEED",
        "business_model": "B2B SaaS",
    }
    matches = _direct_matches(investor, startup)
    criteria = {m["criterion"] for m in matches}
    assert "business_model" not in criteria
    assert "industry" in criteria
    assert "startup_stage" in criteria