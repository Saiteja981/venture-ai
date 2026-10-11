"""
Model-agnostic permutation importance.

Approximate feature attribution for a single prediction:
- For each feature, replace its value with a baseline (mean or median)
  and observe how much the model output changes.
- Larger change = more influential feature for THIS prediction.
- This is an individual-prediction explanation, not a global importance chart.

Caveat: features with strong pairwise correlations can be under- or over-weighted
because only one feature is permuted at a time. Label results accordingly.

No external dependencies beyond numpy.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Sequence

import numpy as np


@dataclass
class FeatureContribution:
    feature: str
    baseline_value: float          # the value substituted for permutation
    original_value: float          # the actual value for this instance
    delta: float                   # model_output(baseline) - model_output(original)
    direction: str                 # "increases" | "decreases" | "neutral"


def explain_instance(
    predict_fn: Callable[[np.ndarray], np.ndarray],
    instance: np.ndarray,
    feature_names: Sequence[str],
    baseline: str = "mean",
    background: np.ndarray | None = None,
    top_k: int | None = None,
) -> list[FeatureContribution]:
    """
    Compute per-feature contributions for one instance.

    Parameters
    ----------
    predict_fn : callable
        Takes an (n_samples, n_features) float array and returns (n_samples,)
        or (n_samples, n_classes) probabilities. If 2D, column 1 is used
        (positive-class probability).
    instance : np.ndarray
        Shape (n_features,).
    feature_names : sequence of str
        Must have the same length as `instance`.
    baseline : "mean" | "median"
        Substitution value for each feature when it is permuted.
    background : np.ndarray, optional
        Shape (n_samples, n_features). Required if baseline is computed
        from a reference set; otherwise baseline is inferred from `instance`
        (single-row mean == instance value, which makes all deltas zero —
        so you should always pass `background` in practice).
    top_k : int, optional
        If provided, only the top-k features by |delta| are returned.

    Returns
    -------
    list[FeatureContribution], sorted by descending |delta|.
    """
    instance = np.asarray(instance, dtype=float).reshape(-1)
    n_features = instance.shape[0]
    if len(feature_names) != n_features:
        raise ValueError(
            f"feature_names length ({len(feature_names)}) does not match "
            f"instance length ({n_features})"
        )

    original_out = _scalar(predict_fn(instance.reshape(1, -1)))

    if background is None:
        # Fall back to the instance itself (produces zero deltas — documented).
        bg = instance.reshape(1, -1)
    else:
        bg = np.asarray(background, dtype=float)
        if bg.ndim != 2 or bg.shape[1] != n_features:
            raise ValueError(
                f"background must be (n_samples, {n_features}), got {bg.shape}"
            )

    if baseline == "mean":
        sub_values = bg.mean(axis=0)
    elif baseline == "median":
        sub_values = np.median(bg, axis=0)
    else:
        raise ValueError(f"unknown baseline '{baseline}'")

    contributions: list[FeatureContribution] = []
    for i, name in enumerate(feature_names):
        perturbed = instance.copy()
        perturbed[i] = sub_values[i]
        new_out = _scalar(predict_fn(perturbed.reshape(1, -1)))
        delta = new_out - original_out
        if abs(delta) < 1e-9:
            direction = "neutral"
        elif delta > 0:
            direction = "increases"
        else:
            direction = "decreases"
        contributions.append(
            FeatureContribution(
                feature=name,
                baseline_value=float(sub_values[i]),
                original_value=float(instance[i]),
                delta=float(delta),
                direction=direction,
            )
        )

    contributions.sort(key=lambda c: abs(c.delta), reverse=True)
    if top_k is not None:
        contributions = contributions[:top_k]
    return contributions


def _scalar(pred) -> float:
    """Extract a scalar output from a model that may return (n,), (n, k), or a list."""
    arr = np.asarray(pred)
    if arr.ndim == 2:
        # Assume column 1 is the positive-class probability.
        return float(arr[0, 1])
    return float(arr.reshape(-1)[0])