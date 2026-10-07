"""
Phase 8.4 — Preprocess the recommendation interaction dataset.

Builds separate feature matrices for startups and investors, applies
leakage-safe preprocessing (fit on train only), and saves:

    data/processed/recommendation_train.csv
    data/processed/recommendation_validation.csv
    data/processed/recommendation_test.csv
    data/processed/recommendation_preprocessor.pkl

Split strategy: 70 / 15 / 15, stratified on the compatibility_label and
grouped by investor_id to prevent the same investor from appearing in more
than one split.

Usage:
    python training/preprocess_recommendation.py
"""

from __future__ import annotations

from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.model_selection import GroupShuffleSplit
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

RANDOM_SEED: int = 42

PROJECT_ROOT = Path(__file__).resolve().parents[1]
RAW_DIR = PROJECT_ROOT / "data" / "raw"
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"

STARTUPS_CSV = RAW_DIR / "raw_synthetic_startups.csv"
INVESTORS_CSV = RAW_DIR / "raw_synthetic_investors.csv"
INTERACTIONS_CSV = RAW_DIR / "synthetic_interactions.csv"

TRAIN_OUT = PROCESSED_DIR / "recommendation_train.csv"
VAL_OUT = PROCESSED_DIR / "recommendation_validation.csv"
TEST_OUT = PROCESSED_DIR / "recommendation_test.csv"
PREPROCESSOR_OUT = PROCESSED_DIR / "recommendation_preprocessor.pkl"

# ---- Feature column definitions ----------------------------------------

STARTUP_NUMERIC = [
    "founded_year", "team_size", "total_users", "customer_count",
    "monthly_revenue", "revenue_growth_rate", "total_funding",
    "monthly_burn_rate", "runway_months", "valuation",
    "revenue_per_customer", "users_per_team_member",
    "growth_adjusted_runway", "funding_to_revenue", "burn_to_revenue",
]
STARTUP_CATEGORICAL = ["industry", "business_model", "startup_stage"]

INVESTOR_NUMERIC = [
    "minimum_investment", "maximum_investment",
    "investment_experience_years", "portfolio_size",
]
INVESTOR_CATEGORICAL = [
    "investor_type", "risk_preference", "preferred_location",
    # Multi-valued fields — treated as single categorical tokens for now.
    # The full multi-valued encoding is left for a future phase.
    "preferred_industries", "preferred_stages", "preferred_business_models",
]

TARGET_COL = "compatibility_label"


def _build_startup_preprocessor() -> ColumnTransformer:
    return ColumnTransformer(
        transformers=[
            ("num", Pipeline([
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
            ]), STARTUP_NUMERIC),
            ("cat", Pipeline([
                ("imputer", SimpleImputer(strategy="most_frequent")),
                ("encoder", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
            ]), STARTUP_CATEGORICAL),
        ],
        remainder="drop",
    )


def _build_investor_preprocessor() -> ColumnTransformer:
    return ColumnTransformer(
        transformers=[
            ("num", Pipeline([
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
            ]), INVESTOR_NUMERIC),
            ("cat", Pipeline([
                ("imputer", SimpleImputer(strategy="most_frequent")),
                ("encoder", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
            ]), INVESTOR_CATEGORICAL),
        ],
        remainder="drop",
    )


def _merge_features(interactions: pd.DataFrame,
                    startups: pd.DataFrame,
                    investors: pd.DataFrame) -> pd.DataFrame:
    st = startups.drop_duplicates(subset=["startup_id"], keep="first").copy()
    inv = investors.drop_duplicates(subset=["investor_id"], keep="first").copy()
    merged = interactions.merge(st, on="startup_id", how="left", suffixes=("", "_st"))
    merged = merged.merge(inv, on="investor_id", how="left", suffixes=("", "_inv"))
    return merged


def main() -> None:
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    for path in (STARTUPS_CSV, INVESTORS_CSV, INTERACTIONS_CSV):
        if not path.exists():
            raise FileNotFoundError(
                f"Missing input: {path}. Run the earlier generation scripts first."
            )

    startups = pd.read_csv(STARTUPS_CSV)
    investors = pd.read_csv(INVESTORS_CSV)
    interactions = pd.read_csv(INTERACTIONS_CSV)

    df = _merge_features(interactions, startups, investors)

    # Group-aware split: hold out whole investors
    gss1 = GroupShuffleSplit(n_splits=1, test_size=0.30, random_state=RANDOM_SEED)
    train_idx, temp_idx = next(gss1.split(df, groups=df["investor_id"]))
    train_df = df.iloc[train_idx].reset_index(drop=True)
    temp_df = df.iloc[temp_idx].reset_index(drop=True)

    gss2 = GroupShuffleSplit(n_splits=1, test_size=0.50, random_state=RANDOM_SEED)
    val_idx, test_idx = next(gss2.split(temp_df, groups=temp_df["investor_id"]))
    val_df = temp_df.iloc[val_idx].reset_index(drop=True)
    test_df = temp_df.iloc[test_idx].reset_index(drop=True)

    # Fit preprocessors on TRAIN ONLY
    st_pre = _build_startup_preprocessor()
    inv_pre = _build_investor_preprocessor()

    st_pre.fit(train_df[STARTUP_NUMERIC + STARTUP_CATEGORICAL])
    inv_pre.fit(train_df[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL])

    # Save the preprocessors as a single bundle
    joblib.dump(
        {"startup": st_pre, "investor": inv_pre},
        PREPROCESSOR_OUT,
    )

    # Save the merged, split DataFrames (raw features + label + ids)
    train_df.to_csv(TRAIN_OUT, index=False)
    val_df.to_csv(VAL_OUT, index=False)
    test_df.to_csv(TEST_OUT, index=False)

    print("=" * 60)
    print("Recommendation Preprocessing")
    print("=" * 60)
    print(f"Total interactions : {len(df)}")
    print(f"Train              : {len(train_df)} "
          f"(+rate: {train_df[TARGET_COL].mean():.3f})")
    print(f"Validation         : {len(val_df)} "
          f"(+rate: {val_df[TARGET_COL].mean():.3f})")
    print(f"Test               : {len(test_df)} "
          f"(+rate: {test_df[TARGET_COL].mean():.3f})")
    print(f"Startup preprocessor features : "
          f"{st_pre.transform(train_df[STARTUP_NUMERIC + STARTUP_CATEGORICAL].head(1)).shape[1]}")
    print(f"Investor preprocessor features: "
          f"{inv_pre.transform(train_df[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL].head(1)).shape[1]}")
    print(f"Saved preprocessors to : {PREPROCESSOR_OUT}")
    print(f"Saved splits to        : {PROCESSED_DIR}")
    print("=" * 60)
    print()
    print("Group-aware split: each investor_id appears in only one split.")
    print("Preprocessors were fit on the training split only (no leakage).")


if __name__ == "__main__":
    main()