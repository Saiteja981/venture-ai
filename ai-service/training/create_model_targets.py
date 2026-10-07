"""
Phase 8.3 — Target creation for the startup potential & risk model.

Reads the leakage-safe processed splits from Phase 8.2 and adds:

- `potential_score` — a deterministic continuous target in [0, 100],
  computed only from features that are legitimately available before
  prediction. It does NOT depend on `outcome_class`, `startup_id`, or
  `company_name`.
- `risk_category` — derived from the existing `outcome_class` column
  (LOW=0, MEDIUM=1, HIGH=2).

The target-generation logic is applied per-row and per-split independently.
No statistics from validation or test data influence the score.

Synthetic-data warning
----------------------
Both targets are SYNTHETIC and are intended for academic development only.
They do not represent real-world startup success, investment returns, or
financial risk.

Usage
-----
    python training/create_model_targets.py
"""

from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

RANDOM_SEED: int = 42

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
PROCESSED_DIR: Path = PROJECT_ROOT / "data" / "processed"

TRAIN_IN: Path = PROCESSED_DIR / "train.csv"
VAL_IN: Path = PROCESSED_DIR / "validation.csv"
TEST_IN: Path = PROCESSED_DIR / "test.csv"

TRAIN_OUT: Path = PROCESSED_DIR / "train_with_targets.csv"
VAL_OUT: Path = PROCESSED_DIR / "validation_with_targets.csv"
TEST_OUT: Path = PROCESSED_DIR / "test_with_targets.csv"

CLASS_TO_INDEX = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
INDEX_TO_CLASS = {v: k for k, v in CLASS_TO_INDEX.items()}

# Columns that must NOT be used to compute potential_score
FORBIDDEN_FOR_TARGET = {"potential_score", "outcome_class", "startup_id", "company_name"}


# ---------------------------------------------------------------------------
# Deterministic potential_score
# ---------------------------------------------------------------------------

def _safe_series(series: pd.Series, floor: float = 0.0) -> pd.Series:
    """Clip to a floor and replace inf with NaN."""
    s = series.replace([np.inf, -np.inf], np.nan)
    return s.clip(lower=floor)


def _log_norm(series: pd.Series) -> pd.Series:
    """Log-normalize a non-negative series to approximately [0, 1]."""
    s = _safe_series(series)
    s = np.log1p(s)
    s = s.fillna(s.median())
    # per-split min-max normalization is fine — score is used only as a target
    mn, mx = s.min(), s.max()
    if mx == mn:
        return pd.Series(0.5, index=s.index)
    return (s - mn) / (mx - mn)


def _clip_norm(series: pd.Series, low: float, high: float) -> pd.Series:
    """Clip to [low, high] then min-max normalize to [0, 1]."""
    s = _safe_series(series).clip(lower=low, upper=high).fillna(low)
    mn, mx = s.min(), s.max()
    if mx == mn:
        return pd.Series(0.5, index=s.index)
    return (s - mn) / (mx - mn)


def _invert(series: pd.Series) -> pd.Series:
    """Invert a [0, 1] series."""
    return 1.0 - series


def compute_potential_score(df: pd.DataFrame, rng: np.random.Generator) -> pd.Series:
    """
    Compute a deterministic, continuous potential_score in [0, 100].

    Formula (documented in training/README.md):

        score_0_1 = 0.20 * norm(log1p(monthly_revenue))
                  + 0.15 * norm(clip(revenue_growth_rate, 0, 100))
                  + 0.10 * norm(log1p(total_users))
                  + 0.10 * norm(log1p(customer_count))
                  + 0.10 * norm(log1p(total_funding))
                  + 0.10 * norm(clip(runway_months, 0, 36))
                  + 0.05 * norm(team_size)
                  + 0.05 * norm(log1p(valuation))
                  + 0.05 * norm(revenue_per_customer)
                  + 0.05 * norm(users_per_team_member)
                  + 0.05 * norm(growth_adjusted_runway)
                  + invert(norm(funding_to_revenue)) * 0.05
                  + invert(norm(burn_to_revenue))    * 0.05

        score_0_100 = 100 * (0.85 * score_0_1 + 0.15 * noise)

    where `noise` is a deterministic uniform random value in [0, 1] drawn
    from the fixed seed. The noise term prevents the target from being a
    perfectly deterministic linear function of the features, which would
    make the regression task trivial.

    The formula does NOT use:
        - outcome_class
        - startup_id
        - company_name
        - any future information
    """
    # Guard against accidental forbidden columns
    for col in FORBIDDEN_FOR_TARGET:
        assert col not in df.columns or col in {"outcome_class", "startup_id", "company_name"}, (
            f"Column '{col}' is not permitted in target computation context."
        )

    required = [
        "monthly_revenue",
        "revenue_growth_rate",
        "total_users",
        "customer_count",
        "total_funding",
        "runway_months",
        "team_size",
        "valuation",
        "revenue_per_customer",
        "users_per_team_member",
        "growth_adjusted_runway",
        "funding_to_revenue",
        "burn_to_revenue",
    ]
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise ValueError(f"Missing columns required for potential_score: {missing}")

    # Build normalized components
    norm_revenue = _log_norm(df["monthly_revenue"])
    norm_growth = _clip_norm(df["revenue_growth_rate"], 0, 100)
    norm_users = _log_norm(df["total_users"])
    norm_customers = _log_norm(df["customer_count"])
    norm_funding = _log_norm(df["total_funding"])
    norm_runway = _clip_norm(df["runway_months"], 0, 36)
    norm_team = _clip_norm(df["team_size"], 0, 200)
    norm_valuation = _log_norm(df["valuation"])
    norm_rpc = _clip_norm(df["revenue_per_customer"], 0, 10_000)
    norm_uptm = _log_norm(df["users_per_team_member"])
    norm_gar = _clip_norm(df["growth_adjusted_runway"], 0, 200)
    norm_f2r = _clip_norm(df["funding_to_revenue"], 0, 200)
    norm_b2r = _clip_norm(df["burn_to_revenue"], 0, 20)

    score_0_1 = (
        0.20 * norm_revenue
        + 0.15 * norm_growth
        + 0.10 * norm_users
        + 0.10 * norm_customers
        + 0.10 * norm_funding
        + 0.10 * norm_runway
        + 0.05 * norm_team
        + 0.05 * norm_valuation
        + 0.05 * norm_rpc
        + 0.05 * norm_uptm
        + 0.05 * norm_gar
        + 0.05 * _invert(norm_f2r)
        + 0.05 * _invert(norm_b2r)
    )

    # Deterministic noise component in [0, 1]
    noise = pd.Series(rng.uniform(0.0, 1.0, size=len(df)), index=df.index)

    combined = 0.85 * score_0_1 + 0.15 * noise
    score_100 = (combined * 100.0).clip(lower=0.0, upper=100.0)

    return score_100.round(4)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def _process_split(in_path: Path, out_path: Path, rng: np.random.Generator) -> None:
    if not in_path.exists():
        raise FileNotFoundError(f"Missing input split: {in_path}")

    df = pd.read_csv(in_path)

    # 1. Deterministic potential score (uses only feature columns)
    df["potential_score"] = compute_potential_score(df, rng)

    # 2. Risk category from outcome_class
    if "outcome_class" not in df.columns:
        raise ValueError(f"'outcome_class' missing in {in_path}")
    df["risk_category"] = df["outcome_class"].map(CLASS_TO_INDEX)
    if df["risk_category"].isna().any():
        bad = df.loc[df["risk_category"].isna(), "outcome_class"].unique().tolist()
        raise ValueError(f"Unexpected outcome_class values: {bad}")
    df["risk_category"] = df["risk_category"].astype(int)

    # 3. Sanity: potential_score is bounded
    assert df["potential_score"].between(0.0, 100.0).all(), "potential_score out of [0, 100]"

    df.to_csv(out_path, index=False)
    print(f"{in_path.name:35s} -> {out_path.name:35s} "
          f"rows={len(df):4d}  potential_score[min={df['potential_score'].min():.2f}, "
          f"max={df['potential_score'].max():.2f}, "
          f"mean={df['potential_score'].mean():.2f}]")


def main() -> None:
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    print("=" * 70)
    print("Phase 8.3 — Create model targets")
    print("=" * 70)

    # Use separate RNG streams per split so the noise term is deterministic
    # but independent across splits.
    rng_train = np.random.default_rng(RANDOM_SEED + 1)
    rng_val = np.random.default_rng(RANDOM_SEED + 2)
    rng_test = np.random.default_rng(RANDOM_SEED + 3)

    _process_split(TRAIN_IN, TRAIN_OUT, rng_train)
    _process_split(VAL_IN, VAL_OUT, rng_val)
    _process_split(TEST_IN, TEST_OUT, rng_test)

    print()
    print("Class mapping:")
    print(json.dumps(CLASS_TO_INDEX, indent=2))
    print()
    print("Done.")
    print("=" * 70)


if __name__ == "__main__":
    main()