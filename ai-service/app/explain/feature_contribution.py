"""
Approximate sensitivity analysis for a single startup prediction.

Method
------
For each input feature, replace its value with a baseline (median from a
reference cohort if provided, otherwise the pydantic minimum) and observe
how much the model's potential_score changes. Larger absolute change
indicates more influence on THIS prediction.

This is NOT:
- a causal explanation
- a global model-level feature importance chart
- a calibrated confidence interval

It IS:
- a per-prediction, model-agnostic, approximate attribution
- labeled as such in the API response
"""
from __future__ import annotations

from typing import Any, Callable, Sequence

NUMERIC_FIELDS: Sequence[str] = (
    "founded_year",
    "team_size",
    "total_users",
    "customer_count",
    "monthly_revenue",
    "revenue_growth_rate",
    "total_funding",
    "monthly_burn_rate",
    "runway_months",
    "valuation",
    "revenue_per_customer",
    "users_per_team_member",
    "growth_adjusted_runway",
    "funding_to_revenue",
    "burn_to_revenue",
)

CATEGORICAL_FIELDS: Sequence[str] = (
    "industry",
    "business_model",
    "startup_stage",
)

# Fallback baselines used when no reference cohort is supplied.
# Chosen to sit at the low end of the observed ranges and are documented
# as approximate. Categorical fields are skipped in this fallback because
# there is no defensible "neutral" category without training data.
FALLBACK_BASELINE: dict[str, Any] = {
    "founded_year": 2020,
    "team_size": 1,
    "total_users": 0,
    "customer_count": 0,
    "monthly_revenue": 0.0,
    "revenue_growth_rate": 0.0,
    "total_funding": 0.0,
    "monthly_burn_rate": 0.0,
    "runway_months": 0.0,
    "valuation": 0.0,
    "revenue_per_customer": 0.0,
    "users_per_team_member": 0.0,
    "growth_adjusted_runway": 0.0,
    "funding_to_revenue": 0.0,
    "burn_to_revenue": 0.0,
}

METHOD_LABEL = "approximate sensitivity analysis (feature-substitution)"


def _predict_score(predict_fn: Callable[[dict], dict], record: dict) -> float:
    out = predict_fn(record)
    return float(out["potential_score"])


def _numeric_baseline(field: str, background: list[dict] | None) -> float:
    if background:
        vals = [r.get(field) for r in background if r.get(field) is not None]
        if vals:
            # Median avoids outlier domination.
            sorted_vals = sorted(float(v) for v in vals)
            n = len(sorted_vals)
            mid = n // 2
            return sorted_vals[mid] if n % 2 else (sorted_vals[mid - 1] + sorted_vals[mid]) / 2.0
    return float(FALLBACK_BASELINE.get(field, 0.0))


def _categorical_baseline(field: str, background: list[dict] | None) -> str | None:
    """Return the modal value from a reference cohort, or None if unavailable."""
    if not background:
        return None
    vals = [r.get(field) for r in background if r.get(field)]
    if not vals:
        return None
    # Most common — ties broken by first appearance.
    counts: dict[str, int] = {}
    for v in vals:
        counts[v] = counts.get(v, 0) + 1
    return max(counts, key=counts.get)


def explain_instance(
    predict_fn: Callable[[dict], dict],
    record: dict,
    background: list[dict] | None = None,
    top_k: int = 8,
) -> list[dict[str, Any]]:
    """
    Compute per-feature contributions for a single prediction.

    Parameters
    ----------
    predict_fn : callable
        Must take a dict of features and return a dict with at least
        {"potential_score": float}. This is the existing `predict_startup`.
    record : dict
        The feature dictionary for the instance being explained.
    background : list[dict], optional
        Reference cohort. If omitted, numeric baselines fall back to a
        documented low-end constant and categorical fields are skipped.
    top_k : int
        Cap the returned contributions to the top-k by |delta|.

    Returns
    -------
    list of {"feature", "original_value", "baseline_value", "delta",
             "direction"} dicts, sorted by descending |delta|.
    """
    original_score = _predict_score(predict_fn, record)
    contributions: list[dict[str, Any]] = []

    for field in (*NUMERIC_FIELDS, *CATEGORICAL_FIELDS):
        if field not in record:
            continue

        if field in NUMERIC_FIELDS:
            baseline = _numeric_baseline(field, background)
        else:
            baseline = _categorical_baseline(field, background)
            if baseline is None:
                # No defensible baseline available without training data —
                # skip rather than fabricate.
                continue

        perturbed = dict(record)
        perturbed[field] = baseline
        new_score = _predict_score(predict_fn, perturbed)
        delta = new_score - original_score

        if abs(delta) < 1e-6:
            direction = "neutral"
        elif delta > 0:
            direction = "increases"
        else:
            direction = "decreases"

        contributions.append({
            "feature": field,
            "original_value": record[field],
            "baseline_value": baseline,
            "delta": round(delta, 4),
            "direction": direction,
        })

    contributions.sort(key=lambda c: abs(c["delta"]), reverse=True)
    return contributions[:top_k]