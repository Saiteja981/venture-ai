"""
Phase 8.5 — Tests for the pitch-deck analyzer.

Run with:
    pytest tests/test_pitch_analyzer.py
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path



_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import numpy as np
import pandas as pd
import pytest

PROJECT_ROOT = _PROJECT_ROOT
RAW_DIR = PROJECT_ROOT / "data" / "raw"
DATASET_CSV = RAW_DIR / "pitch_deck_sections.csv"
SAMPLE_COMPLETE = RAW_DIR / "sample_pitch_deck.txt"
SAMPLE_INCOMPLETE = RAW_DIR / "sample_pitch_deck_incomplete.txt"

SAVED_DIR = PROJECT_ROOT / "saved_models" / "pitch_analyzer"
MODEL_DIR = SAVED_DIR / "model"
TOKENIZER_DIR = SAVED_DIR / "tokenizer"
LABEL_MAPPING_PATH = SAVED_DIR / "label_mapping.json"
METADATA_PATH = SAVED_DIR / "metadata.json"
METRICS_PATH = SAVED_DIR / "metrics.json"

REQUIRED_SECTIONS = [
    "Problem", "Solution", "Market", "Product",
    "Business Model", "Traction", "Competition", "Team",
]


# ---------------------------------------------------------------------------
# Dataset
# ---------------------------------------------------------------------------

def test_dataset_exists():
    assert DATASET_CSV.exists()


def test_dataset_has_required_columns():
    df = pd.read_csv(DATASET_CSV)
    for c in ["sample_id", "section", "text"]:
        assert c in df.columns


def test_dataset_is_balanced():
    df = pd.read_csv(DATASET_CSV)
    counts = df["section"].value_counts()
    assert set(counts.index) == set(REQUIRED_SECTIONS)
    assert counts.max() - counts.min() <= 2


def test_dataset_has_no_empty_text():
    df = pd.read_csv(DATASET_CSV)
    assert (df["text"].astype(str).str.strip().str.len() > 0).all()


# ---------------------------------------------------------------------------
# Splits
# ---------------------------------------------------------------------------

def test_test_split_exists():
    assert (SAVED_DIR / "test_split.csv").exists()


def test_no_train_test_overlap():
    test_df = pd.read_csv(SAVED_DIR / "test_split.csv")
    full_df = pd.read_csv(DATASET_CSV)
    test_texts = set(test_df["text"])
    remainder = full_df[~full_df["text"].isin(test_texts)]
    assert test_texts.isdisjoint(set(remainder["text"]))


# ---------------------------------------------------------------------------
# Tokenizer and label mapping
# ---------------------------------------------------------------------------

def test_tokenizer_directory_exists():
    assert TOKENIZER_DIR.exists()


def test_tokenizer_loads():
    from transformers import BertTokenizer
    tok = BertTokenizer.from_pretrained(str(TOKENIZER_DIR))
    assert tok is not None


def test_label_mapping_exists():
    assert LABEL_MAPPING_PATH.exists()


def test_label_mapping_covers_all_sections():
    with open(LABEL_MAPPING_PATH) as f:
        m = json.load(f)
    assert set(m["label_to_idx"].keys()) == set(REQUIRED_SECTIONS)
    assert len(m["idx_to_label"]) == len(REQUIRED_SECTIONS)


# ---------------------------------------------------------------------------
# Model artifacts
# ---------------------------------------------------------------------------

def test_model_directory_exists():
    assert MODEL_DIR.exists()


def test_metadata_exists():
    assert METADATA_PATH.exists()


def test_metadata_has_required_fields():
    with open(METADATA_PATH) as f:
        meta = json.load(f)
    for k in ["backbone", "random_seed", "num_classes", "sections"]:
        assert k in meta


def test_model_loads():
    from transformers import TFBertForSequenceClassification
    model = TFBertForSequenceClassification.from_pretrained(str(MODEL_DIR))
    assert model is not None


def test_model_output_shape():
    from transformers import TFBertForSequenceClassification
    model = TFBertForSequenceClassification.from_pretrained(str(MODEL_DIR))
    dummy = {
        "input_ids": np.zeros((2, 128), dtype=np.int32),
        "attention_mask": np.ones((2, 128), dtype=np.int32),
        "token_type_ids": np.zeros((2, 128), dtype=np.int32),
    }
    outputs = model.predict(dummy, verbose=0)
    logits = outputs.logits if hasattr(outputs, "logits") else outputs[0]
    assert logits.shape == (2, 8)


def test_model_probabilities_sum_to_one():
    import tensorflow as tf
    from transformers import TFBertForSequenceClassification
    model = TFBertForSequenceClassification.from_pretrained(str(MODEL_DIR))
    dummy = {
        "input_ids": np.zeros((3, 128), dtype=np.int32),
        "attention_mask": np.ones((3, 128), dtype=np.int32),
        "token_type_ids": np.zeros((3, 128), dtype=np.int32),
    }
    outputs = model.predict(dummy, verbose=0)
    logits = outputs.logits if hasattr(outputs, "logits") else outputs[0]
    probs = tf.nn.softmax(logits, axis=-1).numpy()
    sums = probs.sum(axis=1)
    assert np.allclose(sums, 1.0, atol=1e-4)


# ---------------------------------------------------------------------------
# Service: analyze_pitch_deck
# ---------------------------------------------------------------------------

def test_analyze_empty_text_returns_zero_score():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    result = analyze_pitch_deck("")
    assert result["overall_completeness_score"] == 0.0
    assert result["summary"]["missing_count"] == 8
    assert result["summary"]["detected_count"] == 0


def test_analyze_whitespace_only_text():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    result = analyze_pitch_deck("     \n  \t   ")
    assert result["overall_completeness_score"] == 0.0


def test_analyze_complete_sample_runs():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    text = SAMPLE_COMPLETE.read_text(encoding="utf-8")
    result = analyze_pitch_deck(text)
    assert "overall_completeness_score" in result
    assert 0.0 <= result["overall_completeness_score"] <= 100.0
    assert set(result["sections"].keys()) == set(REQUIRED_SECTIONS)


def test_analyze_incomplete_sample_detects_missing():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    text = SAMPLE_INCOMPLETE.read_text(encoding="utf-8")
    result = analyze_pitch_deck(text)
    assert result["summary"]["missing_count"] + len(
        [g for g in result["information_gaps"] if g["severity"] == "MEDIUM"]
    ) >= 1


def test_section_confidences_in_range():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    text = SAMPLE_COMPLETE.read_text(encoding="utf-8")
    result = analyze_pitch_deck(text)
    for s, info in result["sections"].items():
        assert 0.0 <= info["confidence"] <= 1.0
        assert 0.0 <= info["completeness"] <= 100.0


def test_information_gaps_have_required_fields():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    text = SAMPLE_INCOMPLETE.read_text(encoding="utf-8")
    result = analyze_pitch_deck(text)
    for gap in result["information_gaps"]:
        assert "section" in gap
        assert "severity" in gap
        assert "message" in gap
        assert gap["section"] in REQUIRED_SECTIONS


def test_overall_score_between_0_and_100():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    text = SAMPLE_COMPLETE.read_text(encoding="utf-8")
    result = analyze_pitch_deck(text)
    s = result["overall_completeness_score"]
    assert 0.0 <= s <= 100.0


def test_analyze_does_not_crash_on_long_text():
    from app.services.pitch_analyzer_service import analyze_pitch_deck
    text = SAMPLE_COMPLETE.read_text(encoding="utf-8") * 5
    result = analyze_pitch_deck(text)
    assert 0.0 <= result["overall_completeness_score"] <= 100.0