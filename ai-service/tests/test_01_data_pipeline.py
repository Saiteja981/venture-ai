"""
Tests for the synthetic data generation and preprocessing pipeline.

Run with:
    pytest
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pandas as pd
import pytest

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
RAW_CSV: Path = PROJECT_ROOT / "data" / "raw" / "raw_synthetic_startups.csv"
TRAIN_CSV: Path = PROJECT_ROOT / "data" / "processed" / "train.csv"
VAL_CSV: Path = PROJECT_ROOT / "data" / "processed" / "validation.csv"
TEST_CSV: Path = PROJECT_ROOT / "data" / "processed" / "test.csv"
PREPROCESSOR_PKL: Path = PROJECT_ROOT / "data" / "processed" / "preprocessor.pkl"

TARGET_COLUMN = "outcome_class"
VALID_CLASSES = {"HIGH", "MEDIUM", "LOW"}


def _run(script: str) -> None:
    """Run a training script and fail on non-zero exit."""
    result = subprocess.run(
        [sys.executable, script],
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"Script {script} failed:\nSTDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
        )


@pytest.fixture(scope="module", autouse=True)
def _prepare_pipeline():
    """Regenerate dataset and preprocessing outputs once for the test module."""
    _run("training/generate_dataset.py")
    _run("training/preprocess.py")


# ---------------------------------------------------------------------------
# Dataset generation
# ---------------------------------------------------------------------------

def test_raw_dataset_has_303_rows():
    """Raw CSV must contain 300 unique + 3 intentional duplicates = 303 rows."""
    df = pd.read_csv(RAW_CSV)
    assert len(df) == 303, f"Expected 303 raw rows, got {len(df)}"


def test_raw_dataset_has_exactly_300_unique_startup_ids():
    """After deduplication by startup_id, exactly 300 unique records remain."""
    df = pd.read_csv(RAW_CSV)
    unique_ids = df["startup_id"].nunique()
    assert unique_ids == 300, f"Expected 300 unique startup_ids, got {unique_ids}"


def test_raw_dataset_has_exactly_3_duplicate_rows():
    """Exactly 3 duplicate rows must be present for cleaning demonstration."""
    df = pd.read_csv(RAW_CSV)
    dupes = df.duplicated(subset=["startup_id"], keep="first").sum()
    assert dupes == 3, f"Expected 3 duplicate rows, got {dupes}"


def test_required_columns_exist():
    df = pd.read_csv(RAW_CSV)
    expected = {
        "startup_id", "company_name", "industry", "business_model",
        "startup_stage", "founded_year", "team_size", "total_users",
        "customer_count", "monthly_revenue", "revenue_growth_rate",
        "total_funding", "monthly_burn_rate", "runway_months", "valuation",
        "revenue_per_customer", "users_per_team_member",
        "growth_adjusted_runway", "funding_to_revenue", "burn_to_revenue",
        TARGET_COLUMN,
    }
    assert expected.issubset(set(df.columns)), (
        f"Missing: {expected - set(df.columns)}"
    )


def test_outcome_classes_are_valid():
    df = pd.read_csv(RAW_CSV)
    assert set(df[TARGET_COLUMN].unique()).issubset(VALID_CLASSES)


# ---------------------------------------------------------------------------
# Split sizes
# ---------------------------------------------------------------------------

def test_split_sizes_are_approximately_70_15_15():
    train = pd.read_csv(TRAIN_CSV)
    val = pd.read_csv(VAL_CSV)
    test = pd.read_csv(TEST_CSV)

    total = len(train) + len(val) + len(test)
    assert total == 300, f"Expected 300 cleaned records total, got {total}"

    train_pct = len(train) / total
    val_pct = len(val) / total
    test_pct = len(test) / total

    assert 0.65 <= train_pct <= 0.75, f"Train pct {train_pct}"
    assert 0.10 <= val_pct <= 0.20, f"Val pct {val_pct}"
    assert 0.10 <= test_pct <= 0.20, f"Test pct {test_pct}"


# ---------------------------------------------------------------------------
# Split overlap (exact, by startup_id)
# ---------------------------------------------------------------------------

def test_startup_id_present_in_processed_splits():
    """startup_id must be retained in processed CSVs for traceability."""
    train = pd.read_csv(TRAIN_CSV)
    val = pd.read_csv(VAL_CSV)
    test = pd.read_csv(TEST_CSV)
    assert "startup_id" in train.columns
    assert "startup_id" in val.columns
    assert "startup_id" in test.columns


def test_no_train_test_id_overlap():
    """No startup_id may appear in both train and test."""
    train = pd.read_csv(TRAIN_CSV)
    test = pd.read_csv(TEST_CSV)
    train_ids = set(train["startup_id"])
    test_ids = set(test["startup_id"])
    overlap = train_ids & test_ids
    assert not overlap, f"Overlap between train and test: {overlap}"


def test_no_train_validation_id_overlap():
    """No startup_id may appear in both train and validation."""
    train = pd.read_csv(TRAIN_CSV)
    val = pd.read_csv(VAL_CSV)
    overlap = set(train["startup_id"]) & set(val["startup_id"])
    assert not overlap, f"Overlap between train and validation: {overlap}"


def test_no_validation_test_id_overlap():
    """No startup_id may appear in both validation and test."""
    val = pd.read_csv(VAL_CSV)
    test = pd.read_csv(TEST_CSV)
    overlap = set(val["startup_id"]) & set(test["startup_id"])
    assert not overlap, f"Overlap between validation and test: {overlap}"


def test_all_300_unique_ids_are_present_across_splits():
    """The union of the three splits must cover all 300 unique startup_ids."""
    train = pd.read_csv(TRAIN_CSV)
    val = pd.read_csv(VAL_CSV)
    test = pd.read_csv(TEST_CSV)
    all_ids = set(train["startup_id"]) | set(val["startup_id"]) | set(test["startup_id"])
    assert len(all_ids) == 300, f"Expected 300 unique ids across splits, got {len(all_ids)}"


# ---------------------------------------------------------------------------
# Artifacts
# ---------------------------------------------------------------------------

def test_preprocessor_file_is_created():
    assert PREPROCESSOR_PKL.exists(), "preprocessor.pkl was not created"


def test_processed_files_are_created():
    assert TRAIN_CSV.exists()
    assert VAL_CSV.exists()
    assert TEST_CSV.exists()


# ---------------------------------------------------------------------------
# Reproducibility
# ---------------------------------------------------------------------------

def test_pipeline_is_reproducible():
    """Re-running the pipeline with the same seed yields identical outputs."""
    train_before = pd.read_csv(TRAIN_CSV)

    _run("training/generate_dataset.py")
    _run("training/preprocess.py")

    train_after = pd.read_csv(TRAIN_CSV)

    pd.testing.assert_frame_equal(train_before, train_after)