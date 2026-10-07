"""
Dataset summary report for the synthetic startup dataset.

Usage
-----
    python training/dataset_summary.py
"""

from __future__ import annotations

from pathlib import Path

import pandas as pd

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
RAW_CSV: Path = PROJECT_ROOT / "data" / "raw" / "raw_synthetic_startups.csv"

TARGET_COLUMN = "outcome_class"
ID_COLUMNS = ["startup_id", "company_name"]
CATEGORICAL_COLUMNS = ["industry", "business_model", "startup_stage"]


def main() -> None:
    if not RAW_CSV.exists():
        raise FileNotFoundError(
            f"Raw dataset not found at {RAW_CSV}. "
            "Run `python training/generate_dataset.py` first."
        )

    df = pd.read_csv(RAW_CSV)

    print("=" * 60)
    print("Synthetic Startup Dataset — Summary")
    print("=" * 60)

    print(f"Total records           : {len(df)}")
    print(f"Total columns           : {len(df.columns)}")

    numerical_cols = df.select_dtypes(include=["number"]).columns.tolist()
    categorical_cols = df.select_dtypes(include=["object"]).columns.tolist()

    print(f"Numerical columns       : {len(numerical_cols)}")
    print(f"  {numerical_cols}")
    print(f"Categorical columns     : {len(categorical_cols)}")
    print(f"  {categorical_cols}")

    # Missing values
    missing = df.isna().sum()
    missing_nonzero = missing[missing > 0]
    print(f"\nColumns with missing values : {len(missing_nonzero)}")
    if not missing_nonzero.empty:
        print(missing_nonzero.to_string())

    # Duplicates
    dup_total = df.duplicated().sum()
    dup_id = df.duplicated(subset=["startup_id"]).sum()
    print(f"\nDuplicate rows (full)   : {dup_total}")
    print(f"Duplicate startup_ids   : {dup_id}")

    # Target distribution
    print("\nOutcome class counts:")
    print(df[TARGET_COLUMN].value_counts().to_string())

    # Categorical distributions
    print("\nIndustry counts:")
    print(df["industry"].value_counts().to_string())

    print("\nStartup stage counts:")
    print(df["startup_stage"].value_counts().to_string())

    print("\nBusiness model counts:")
    print(df["business_model"].value_counts().to_string())

    # Numerical statistics
    print("\nNumerical statistics:")
    print(df[numerical_cols].describe().T.to_string())

    print("=" * 60)


if __name__ == "__main__":
    main()