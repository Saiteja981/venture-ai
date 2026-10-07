"""
Preprocessing pipeline for the synthetic startup dataset.

Responsibilities
----------------
1.  Load the raw CSV.
2.  Validate required columns.
3.  Inspect data types.
4.  Detect and remove duplicate records (by startup_id).
5.  Handle invalid numeric values.
6.  Handle missing values.
7.  Handle extreme outliers using a documented approach.
8.  Separate features and target.
9.  Stratified train/validation/test split (70/15/15).
10. Encode categorical variables.
11. Scale numerical variables.
12. Save the fitted preprocessing pipeline.
13. Save processed datasets (with startup_id retained as metadata).

Row-count contract
------------------
Raw CSV           : 303 rows (300 unique + 3 intentional duplicates)
After dedup       : 300 unique rows
Train / Val / Test: 210 / 45 / 45

startup_id is retained in the processed CSVs as METADATA only. It is NOT
used as a model feature. This allows exact split-overlap verification.

LEAKAGE PREVENTION
------------------
All imputers, encoders, scalers, and outlier thresholds are fit ONLY on the
training set. The fitted transformations are then applied to the validation
and test sets. No statistics from validation or test data influence the
fitted objects.

Reproducibility
---------------
Fixed random state. Re-running this script produces identical outputs.

Usage
-----
    python training/preprocess.py
"""

from __future__ import annotations

from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

RANDOM_STATE: int = 42

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
RAW_CSV: Path = PROJECT_ROOT / "data" / "raw" / "raw_synthetic_startups.csv"
PROCESSED_DIR: Path = PROJECT_ROOT / "data" / "processed"

TRAIN_CSV: Path = PROCESSED_DIR / "train.csv"
VALIDATION_CSV: Path = PROCESSED_DIR / "validation.csv"
TEST_CSV: Path = PROCESSED_DIR / "test.csv"
PREPROCESSOR_PKL: Path = PROCESSED_DIR / "preprocessor.pkl"

# Columns that must exist in the raw file
REQUIRED_COLUMNS = [
    "startup_id",
    "company_name",
    "industry",
    "business_model",
    "startup_stage",
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
    "outcome_class",
]

# Metadata columns retained in processed CSVs for traceability / testing.
# These are NOT used as model features.
METADATA_COLUMNS = ["startup_id", "company_name"]

# Target
TARGET_COLUMN = "outcome_class"

# Categorical model features
CATEGORICAL_FEATURES = ["industry", "business_model", "startup_stage"]

# Numerical model features
NUMERICAL_FEATURES = [
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
]

# Valid target classes
VALID_CLASSES = {"HIGH", "MEDIUM", "LOW"}


# ---------------------------------------------------------------------------
# Cleaning helpers
# ---------------------------------------------------------------------------

def _clean_invalid_numerics(df: pd.DataFrame) -> pd.DataFrame:
    """
    Replace invalid numeric values with NaN so the imputer can handle them.

    Rule:
        - monthly_burn_rate < 0  -> NaN (burn rate cannot be negative)
        - Any negative value in strictly non-negative columns -> NaN
    """
    df = df.copy()

    if "monthly_burn_rate" in df.columns:
        df.loc[df["monthly_burn_rate"] < 0, "monthly_burn_rate"] = np.nan

    non_negative_cols = [
        "team_size",
        "total_users",
        "customer_count",
        "monthly_revenue",
        "total_funding",
        "runway_months",
        "valuation",
        "revenue_per_customer",
        "users_per_team_member",
        "funding_to_revenue",
        "burn_to_revenue",
    ]
    for col in non_negative_cols:
        if col in df.columns:
            df.loc[df[col] < 0, col] = np.nan

    return df


# ---------------------------------------------------------------------------
# Pipeline construction
# ---------------------------------------------------------------------------

def _build_preprocessor() -> ColumnTransformer:
    """
    Build the ColumnTransformer.

    - Numerical: median imputation + standard scaling.
    - Categorical: most-frequent imputation + one-hot encoding.

    Only NUMERICAL_FEATURES and CATEGORICAL_FEATURES are included.
    METADATA_COLUMNS are explicitly excluded from the transformer, so they
    cannot leak into the model.
    """
    numeric_pipeline = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
        ]
    )

    categorical_pipeline = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            (
                "encoder",
                OneHotEncoder(handle_unknown="ignore", sparse_output=False),
            ),
        ]
    )

    preprocessor = ColumnTransformer(
        transformers=[
            ("num", numeric_pipeline, NUMERICAL_FEATURES),
            ("cat", categorical_pipeline, CATEGORICAL_FEATURES),
        ],
        remainder="drop",  # drops metadata automatically
    )

    return preprocessor


# ---------------------------------------------------------------------------
# Main pipeline
# ---------------------------------------------------------------------------

def main() -> None:
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    print("=" * 60)
    print("Preprocessing Pipeline")
    print("=" * 60)

    # 1. Load
    if not RAW_CSV.exists():
        raise FileNotFoundError(
            f"Raw dataset not found at {RAW_CSV}. "
            "Run `python training/generate_dataset.py` first."
        )
    df = pd.read_csv(RAW_CSV)
    print(f"Loaded raw dataset      : shape={df.shape}")

    # 2. Validate required columns
    missing_cols = [c for c in REQUIRED_COLUMNS if c not in df.columns]
    if missing_cols:
        raise ValueError(f"Missing required columns: {missing_cols}")
    print("Required columns present: OK")

    # 3. Inspect data types
    print("\nData types (head):")
    print(df.dtypes.head(10).to_string())

    # 4. Detect duplicates
    n_before = len(df)
    duplicates = df.duplicated(subset=["startup_id"], keep="first").sum()
    print(f"\nDuplicate startup_ids   : {duplicates}")

    # 5. Remove duplicates by startup_id (keep first occurrence)
    df = df.drop_duplicates(subset=["startup_id"], keep="first").reset_index(drop=True)
    n_unique = len(df)
    print(f"Rows after dedup        : {n_unique} (removed {n_before - n_unique})")
    print(f"Raw rows                : {n_before}")
    print(f"Clean unique rows       : {n_unique}")

    # 6. Handle invalid numerics (negative values -> NaN)
    df = _clean_invalid_numerics(df)
    print("Invalid numerics        : cleaned (negative values -> NaN)")

    # 7. Handle missing values (deferred to the pipeline — imputers)
    missing_before = df[NUMERICAL_FEATURES + CATEGORICAL_FEATURES].isna().sum().sum()
    print(f"Missing values (before) : {missing_before}")

    # 8. Separate metadata, features, and target
    metadata = df[METADATA_COLUMNS].copy()
    y = df[TARGET_COLUMN].copy()
    X = df[NUMERICAL_FEATURES + CATEGORICAL_FEATURES].copy()

    # Validate target classes
    invalid_classes = set(y.unique()) - VALID_CLASSES
    if invalid_classes:
        raise ValueError(f"Invalid outcome_class values: {invalid_classes}")
    print(f"Target classes          : {sorted(y.unique())}")

    # 9. Train / Validation / Test split (stratified, 70/15/15)
    #    First split: 70% train, 30% temp
    (
        X_train, X_temp,
        y_train, y_temp,
        meta_train, meta_temp,
    ) = train_test_split(
        X, y, metadata,
        test_size=0.30,
        random_state=RANDOM_STATE,
        stratify=y,
    )
    #    Second split: split temp 50/50 -> 15% val, 15% test
    (
        X_val, X_test,
        y_val, y_test,
        meta_val, meta_test,
    ) = train_test_split(
        X_temp, y_temp, meta_temp,
        test_size=0.50,
        random_state=RANDOM_STATE,
        stratify=y_temp,
    )

    print(f"\nSplit sizes:")
    print(f"  Train      : {len(X_train)} ({len(X_train)/len(X)*100:.1f}%)")
    print(f"  Validation : {len(X_val)} ({len(X_val)/len(X)*100:.1f}%)")
    print(f"  Test       : {len(X_test)} ({len(X_test)/len(X)*100:.1f}%)")

    # Verify no startup_id overlap between splits (sanity check before saving)
    train_ids = set(meta_train["startup_id"])
    val_ids = set(meta_val["startup_id"])
    test_ids = set(meta_test["startup_id"])
    assert train_ids.isdisjoint(val_ids), "startup_id overlap: train vs val"
    assert train_ids.isdisjoint(test_ids), "startup_id overlap: train vs test"
    assert val_ids.isdisjoint(test_ids), "startup_id overlap: val vs test"
    print("Split ID overlap check  : PASSED (no overlap between any splits)")

    # 10. Outlier clipping using training-fitted bounds
    X_train_clean = X_train.copy()
    X_val_clean = X_val.copy()
    X_test_clean = X_test.copy()

    for col in NUMERICAL_FEATURES:
        q1 = X_train_clean[col].quantile(0.25)
        q3 = X_train_clean[col].quantile(0.75)
        iqr = q3 - q1
        if iqr == 0:
            continue
        lower = q1 - 3.0 * iqr
        upper = q3 + 3.0 * iqr
        X_train_clean[col] = X_train_clean[col].clip(lower=lower, upper=upper)
        X_val_clean[col] = X_val_clean[col].clip(lower=lower, upper=upper)
        X_test_clean[col] = X_test_clean[col].clip(lower=lower, upper=upper)

    print("\nOutlier clipping        : applied (IQR, k=3.0, fit on train only)")

    # 11. Build preprocessor
    preprocessor = _build_preprocessor()

    # 12. Fit ONLY on training data
    print("\nFitting preprocessor on training data only...")
    X_train_transformed = preprocessor.fit_transform(X_train_clean)

    # 13. Transform validation and test using the SAME fitted preprocessor
    X_val_transformed = preprocessor.transform(X_val_clean)
    X_test_transformed = preprocessor.transform(X_test_clean)

    print(f"Transformed train shape : {X_train_transformed.shape}")
    print(f"Transformed val shape   : {X_val_transformed.shape}")
    print(f"Transformed test shape  : {X_test_transformed.shape}")

    # 14. Save fitted preprocessor
    joblib.dump(preprocessor, PREPROCESSOR_PKL)
    print(f"\nPreprocessor saved to   : {PREPROCESSOR_PKL}")

    # 15. Save processed datasets (metadata + features + target)
    train_out = pd.concat([meta_train.reset_index(drop=True),
                           X_train_clean.reset_index(drop=True)], axis=1)
    train_out[TARGET_COLUMN] = y_train.reset_index(drop=True)

    val_out = pd.concat([meta_val.reset_index(drop=True),
                         X_val_clean.reset_index(drop=True)], axis=1)
    val_out[TARGET_COLUMN] = y_val.reset_index(drop=True)

    test_out = pd.concat([meta_test.reset_index(drop=True),
                          X_test_clean.reset_index(drop=True)], axis=1)
    test_out[TARGET_COLUMN] = y_test.reset_index(drop=True)

    train_out.to_csv(TRAIN_CSV, index=False)
    val_out.to_csv(VALIDATION_CSV, index=False)
    test_out.to_csv(TEST_CSV, index=False)

    print(f"Train saved to          : {TRAIN_CSV}")
    print(f"Validation saved to     : {VALIDATION_CSV}")
    print(f"Test saved to           : {TEST_CSV}")
    print()
    print("Metadata note           : startup_id and company_name are retained")
    print("                          in the split CSVs for traceability and")
    print("                          split-overlap testing, but are NOT used")
    print("                          as model features (excluded from the")
    print("                          ColumnTransformer).")
    print("=" * 60)


if __name__ == "__main__":
    main()