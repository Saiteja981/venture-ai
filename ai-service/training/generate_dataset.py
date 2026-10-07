"""
Synthetic startup dataset generator for the AI-Powered Venture Capital
and Startup Collaboration Platform.

IMPORTANT
---------
This script produces a SYNTHETIC dataset intended only for development and
academic demonstration. The outcome_class label is a rule-derived
DEVELOPMENT-ONLY target and does NOT represent actual startup success,
investment returns, or real-world VC performance.

Row-count semantics
-------------------
- The generator produces exactly 300 UNIQUE startup records.
- It then intentionally appends 3 DUPLICATE rows (same startup_id as 3 of the
  unique records) so the preprocessing pipeline can demonstrate duplicate
  detection and removal.
- Therefore the raw CSV contains 303 rows:
    303 raw rows = 300 unique + 3 intentional duplicates
- After duplicate removal in preprocessing, the cleaned dataset contains
  exactly 300 unique records.

Reproducibility
---------------
The generator uses a fixed random seed. Running this script multiple times
will produce the exact same CSV file.

Usage
-----
    python training/generate_dataset.py
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

RANDOM_SEED: int = 42
N_UNIQUE_RECORDS: int = 300
N_INTENTIONAL_DUPLICATES: int = 3
EXPECTED_RAW_ROWS: int = N_UNIQUE_RECORDS + N_INTENTIONAL_DUPLICATES  # 303

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
RAW_DIR: Path = PROJECT_ROOT / "data" / "raw"
OUTPUT_CSV: Path = RAW_DIR / "raw_synthetic_startups.csv"

# Categories used for synthetic generation
INDUSTRIES = [
    "Artificial Intelligence",
    "FinTech",
    "HealthTech",
    "EdTech",
    "E-Commerce",
    "SaaS",
    "Cybersecurity",
    "CleanTech",
    "Logistics",
    "Gaming",
]

BUSINESS_MODELS = [
    "B2B SaaS",
    "B2C SaaS",
    "Marketplace",
    "Subscription",
    "Freemium",
    "Enterprise",
    "Transactional",
]

STARTUP_STAGES = ["PRE_SEED", "SEED", "SERIES_A", "SERIES_B"]

COMPANY_PREFIXES = [
    "Nova", "Apex", "Lumen", "Vertex", "Quanta", "Orbit", "Helix", "Zeta",
    "Pulse", "Forge", "Nimbus", "Cortex", "Flux", "Atlas", "Aether",
]
COMPANY_SUFFIXES = [
    "Labs", "AI", "Tech", "Systems", "Works", "Dynamics", "Analytics",
    "Solutions", "Ventures", "Cloud",
]


# ---------------------------------------------------------------------------
# Generation helpers
# ---------------------------------------------------------------------------

def _generate_company_name(idx: int) -> str:
    """Deterministic but varied company name."""
    prefix = COMPANY_PREFIXES[idx % len(COMPANY_PREFIXES)]
    suffix = COMPANY_SUFFIXES[(idx * 3) % len(COMPANY_SUFFIXES)]
    return f"{prefix}{suffix}"


def _generate_rows(rng: np.random.Generator) -> pd.DataFrame:
    """Generate exactly N_UNIQUE_RECORDS unique synthetic records."""
    records = []

    for i in range(N_UNIQUE_RECORDS):
        industry = INDUSTRIES[i % len(INDUSTRIES)]
        business_model = BUSINESS_MODELS[i % len(BUSINESS_MODELS)]
        stage = STARTUP_STAGES[i % len(STARTUP_STAGES)]

        founded_year = int(rng.integers(2015, 2025))
        team_size = int(rng.integers(2, 60))

        # Traction
        total_users = int(rng.integers(50, 500_000))
        customer_count = int(max(1, total_users * rng.uniform(0.005, 0.05)))
        monthly_revenue = float(rng.uniform(500, 400_000))
        revenue_growth_rate = float(rng.uniform(-10, 60))  # percentage

        # Financials
        total_funding = float(rng.uniform(10_000, 15_000_000))
        monthly_burn_rate = float(rng.uniform(2_000, 250_000))
        runway_months = float(rng.uniform(1, 36))

        valuation = float(
            total_funding * rng.uniform(1.5, 8.0)
            + monthly_revenue * rng.uniform(2.0, 12.0)
        )

        records.append(
            {
                "startup_id": f"S{1000 + i}",
                "company_name": _generate_company_name(i),
                "industry": industry,
                "business_model": business_model,
                "startup_stage": stage,
                "founded_year": founded_year,
                "team_size": team_size,
                "total_users": total_users,
                "customer_count": customer_count,
                "monthly_revenue": round(monthly_revenue, 2),
                "revenue_growth_rate": round(revenue_growth_rate, 2),
                "total_funding": round(total_funding, 2),
                "monthly_burn_rate": round(monthly_burn_rate, 2),
                "runway_months": round(runway_months, 2),
                "valuation": round(valuation, 2),
            }
        )

    return pd.DataFrame(records)


# ---------------------------------------------------------------------------
# Engineered features
# ---------------------------------------------------------------------------

def _engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    """Add explainable engineered features."""
    df = df.copy()

    df["revenue_per_customer"] = (
        df["monthly_revenue"] / df["customer_count"].replace(0, np.nan)
    )
    df["users_per_team_member"] = (
        df["total_users"] / df["team_size"].replace(0, np.nan)
    )
    df["growth_adjusted_runway"] = (
        df["runway_months"] * (1 + df["revenue_growth_rate"] / 100.0)
    )
    df["funding_to_revenue"] = (
        df["total_funding"] / df["monthly_revenue"].replace(0, np.nan)
    )
    df["burn_to_revenue"] = (
        df["monthly_burn_rate"] / df["monthly_revenue"].replace(0, np.nan)
    )

    return df


# ---------------------------------------------------------------------------
# Synthetic development target
# ---------------------------------------------------------------------------

def _compute_outcome_class(df: pd.DataFrame) -> pd.Series:
    """
    Compute the DEVELOPMENT-ONLY synthetic target.

    This label is a transparent rule-based heuristic. It is NOT a proxy for
    real-world startup success or investment returns.

    Rule (documented in data/README.md):

        score = 0.35 * norm(log1p(monthly_revenue))
              + 0.25 * norm(clip(revenue_growth_rate, 0, 100))
              + 0.20 * norm(clip(runway_months, 0, 36))
              + 0.20 * norm(invert(clip(funding_to_revenue, 0, 50)))
              + 0.10 * norm(invert(clip(burn_to_revenue, 0, 5)))

    Classification:
        score >= 0.66  -> HIGH
        score >= 0.33  -> MEDIUM
        otherwise      -> LOW
    """
    import numpy as np

    revenue = np.log1p(df["monthly_revenue"].clip(lower=0))
    growth = df["revenue_growth_rate"].clip(lower=0, upper=100)
    runway = df["runway_months"].clip(lower=0, upper=36)

    f2r = df["funding_to_revenue"].replace([np.inf, -np.inf], np.nan)
    f2r = f2r.clip(lower=0, upper=50)
    f2r_inv = 50 - f2r.fillna(f2r.median())

    b2r = df["burn_to_revenue"].replace([np.inf, -np.inf], np.nan)
    b2r = b2r.clip(lower=0, upper=5)
    b2r_inv = 5 - b2r.fillna(b2r.median())

    def _norm(series: pd.Series) -> pd.Series:
        mn, mx = series.min(), series.max()
        if mx == mn:
            return pd.Series(0.5, index=series.index)
        return (series - mn) / (mx - mn)

    score = (
        0.35 * _norm(revenue)
        + 0.25 * _norm(growth)
        + 0.20 * _norm(runway)
        + 0.20 * _norm(f2r_inv)
        + 0.10 * _norm(b2r_inv)
    )

    def _classify(s: float) -> str:
        if s >= 0.66:
            return "HIGH"
        if s >= 0.33:
            return "MEDIUM"
        return "LOW"

    return score.apply(_classify)


# ---------------------------------------------------------------------------
# Intentional data-quality issues
# ---------------------------------------------------------------------------

def _inject_quality_issues(df: pd.DataFrame, rng: np.random.Generator) -> pd.DataFrame:
    """
    Introduce a small, documented set of realistic data-quality issues so the
    preprocessing pipeline can demonstrate cleaning.

    Issues injected:
    1. Missing values in `monthly_revenue` (5 rows) and `team_size` (5 rows).
    2. Duplicate rows: 3 rows appended (same startup_id as 3 unique records).
       Raw rows become 300 + 3 = 303.
    3. Invalid numeric values: negative `monthly_burn_rate` (3 rows).
    4. Extreme outliers: `monthly_revenue` multiplied by 100 for 3 rows.
    """
    df = df.copy()

    # 1. Missing values
    missing_revenue_idx = rng.choice(df.index, size=5, replace=False)
    df.loc[missing_revenue_idx, "monthly_revenue"] = np.nan

    missing_team_idx = rng.choice(df.index, size=5, replace=False)
    df.loc[missing_team_idx, "team_size"] = np.nan

    # 2. Duplicate rows (same startup_id as 3 of the unique records)
    dup_idx = rng.choice(df.index, size=N_INTENTIONAL_DUPLICATES, replace=False)
    duplicates = df.loc[dup_idx].copy()
    df = pd.concat([df, duplicates], ignore_index=True)

    # 3. Invalid numeric values
    invalid_idx = rng.choice(df.index, size=3, replace=False)
    df.loc[invalid_idx, "monthly_burn_rate"] = -abs(
        df.loc[invalid_idx, "monthly_burn_rate"].fillna(1000)
    )

    # 4. Extreme outliers
    outlier_idx = rng.choice(df.index, size=3, replace=False)
    df.loc[outlier_idx, "monthly_revenue"] = (
        df.loc[outlier_idx, "monthly_revenue"].fillna(1000) * 100
    )

    return df


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    rng = np.random.default_rng(RANDOM_SEED)

    RAW_DIR.mkdir(parents=True, exist_ok=True)

    # Step 1: Generate N_UNIQUE_RECORDS unique records
    df = _generate_rows(rng)
    n_unique = len(df)

    # Step 2: Engineer features (on clean numeric columns)
    df = _engineer_features(df)

    # Step 3: Compute synthetic target (before quality issues)
    df["outcome_class"] = _compute_outcome_class(df)

    # Step 4: Inject documented data-quality issues (adds 3 duplicate rows)
    df = _inject_quality_issues(df, rng)

    # Step 5: Save
    df.to_csv(OUTPUT_CSV, index=False)

    # Step 6: Report
    n_raw = len(df)
    n_dupes = n_raw - n_unique
    print("=" * 60)
    print("Synthetic Startup Dataset Generator")
    print("=" * 60)
    print(f"Random seed                    : {RANDOM_SEED}")
    print(f"Unique records generated       : {n_unique}")
    print(f"Intentional duplicate rows     : {n_dupes}")
    print(f"Raw rows written to CSV        : {n_raw}")
    print()
    print("Note:")
    print("  303 raw rows = 300 unique records + 3 intentional duplicates")
    print("  After duplicate removal, the cleaned dataset has 300 unique records.")
    print()
    print(f"Final raw dataset shape        : {df.shape}")
    print(f"Total columns                  : {len(df.columns)}")
    print()
    print("Class distribution (outcome_class):")
    print(df["outcome_class"].value_counts().to_string())
    print()
    print(f"Output written to              : {OUTPUT_CSV}")
    print("=" * 60)


if __name__ == "__main__":
    main()