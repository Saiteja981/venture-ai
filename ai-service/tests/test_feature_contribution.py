"""
Unit tests for approximate feature-substitution explanations.

Uses a synthetic predictor so no ML model needs to load.
"""
from app.explain.feature_contribution import (
    METHOD_LABEL,
    NUMERIC_FIELDS,
    CATEGORICAL_FIELDS,
    _numeric_baseline,
    explain_instance,
)


def _dummy_predict(record):
    """Deterministic linear scorer mimicking predict_startup's output shape."""
    return {
        "potential_score": (
            10.0
            + 0.001 * float(record.get("monthly_revenue") or 0)
            + 0.5 * float(record.get("team_size") or 0)
        )
    }


def _base_record():
    return {
        "founded_year": 2020,
        "team_size": 10,
        "total_users": 1000,
        "customer_count": 100,
        "monthly_revenue": 50000.0,
        "revenue_growth_rate": 20.0,
        "total_funding": 1_000_000.0,
        "monthly_burn_rate": 20_000.0,
        "runway_months": 18.0,
        "valuation": 5_000_000.0,
        "revenue_per_customer": 500.0,
        "users_per_team_member": 100.0,
        "growth_adjusted_runway": 20.0,
        "funding_to_revenue": 20.0,
        "burn_to_revenue": 0.4,
        "industry": "SaaS",
        "business_model": "B2B SaaS",
        "startup_stage": "SEED",
    }


def test_explain_instance_returns_top_k():
    contribs = explain_instance(_dummy_predict, _base_record(), top_k=5)
    assert len(contribs) == 5


def test_explain_instance_only_numeric_without_background():
    # top_k must exceed the number of features to prove categorical fields are
    # absent from the output — not merely truncated by top-k ranking.
    contribs = explain_instance(_dummy_predict, _base_record(), top_k=100)
    features = {c["feature"] for c in contribs}
    for cat in CATEGORICAL_FIELDS:
        assert cat not in features
    for num in NUMERIC_FIELDS:
        assert num in features


def test_explain_instance_includes_categorical_with_background():
    bg = [_base_record() for _ in range(3)]
    contribs = explain_instance(
        _dummy_predict, _base_record(), background=bg, top_k=100
    )
    features = {c["feature"] for c in contribs}
    for cat in CATEGORICAL_FIELDS:
        assert cat in features


def test_explain_instance_direction_is_valid():
    contribs = explain_instance(_dummy_predict, _base_record())
    for c in contribs:
        assert c["direction"] in ("increases", "decreases", "neutral")


def test_explain_instance_delta_matches_actual_predictor_change():
    """The reported delta must equal the true change in the model output."""
    rec = _base_record()
    original = _dummy_predict(rec)["potential_score"]
    contribs = explain_instance(_dummy_predict, rec)
    for c in contribs:
        perturbed = dict(rec)
        perturbed[c["feature"]] = c["baseline_value"]
        new_score = _dummy_predict(perturbed)["potential_score"]
        assert abs((new_score - original) - c["delta"]) < 1e-6


def test_explain_instance_sorted_by_absolute_delta():
    contribs = explain_instance(_dummy_predict, _base_record())
    abs_deltas = [abs(c["delta"]) for c in contribs]
    assert abs_deltas == sorted(abs_deltas, reverse=True)


def test_numeric_baseline_is_median_of_background():
    bg = [
        {"monthly_revenue": 1000.0},
        {"monthly_revenue": 3000.0},
        {"monthly_revenue": 5000.0},
    ]
    assert _numeric_baseline("monthly_revenue", bg) == 3000.0


def test_numeric_baseline_fallback_when_background_empty():
    # Documented low-end fallback; must be non-None
    val = _numeric_baseline("monthly_revenue", [])
    assert val == 0.0


def test_method_label_is_labelled_approximate():
    assert METHOD_LABEL
    assert "approximate" in METHOD_LABEL.lower()