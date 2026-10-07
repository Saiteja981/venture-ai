"""
Phase 8.5 — Evaluate the BERT pitch-deck section classifier on the test split.

Outputs:
    saved_models/pitch_analyzer/metrics.json
    saved_models/pitch_analyzer/plots/confusion_matrix.png

Usage:
    python training/evaluate_pitch_analyzer.py
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path



_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import tensorflow as tf
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
)
from transformers import BertTokenizer, TFBertForSequenceClassification

PROJECT_ROOT = _PROJECT_ROOT
SAVED_DIR = PROJECT_ROOT / "saved_models" / "pitch_analyzer"
MODEL_DIR = SAVED_DIR / "model"
TOKENIZER_DIR = SAVED_DIR / "tokenizer"
LABEL_MAPPING_PATH = SAVED_DIR / "label_mapping.json"
TEST_SPLIT_PATH = SAVED_DIR / "test_split.csv"
METRICS_PATH = SAVED_DIR / "metrics.json"
PLOTS_DIR = SAVED_DIR / "plots"
CONFUSION_PLOT_PATH = PLOTS_DIR / "confusion_matrix.png"

MAX_SEQ_LEN = 128
BATCH_SIZE = 8


def main() -> None:
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    for p in (MODEL_DIR, TOKENIZER_DIR, LABEL_MAPPING_PATH, TEST_SPLIT_PATH):
        if not Path(p).exists():
            raise FileNotFoundError(f"Missing required artifact: {p}")

    with open(LABEL_MAPPING_PATH, "r", encoding="utf-8") as f:
        mapping = json.load(f)
    idx_to_label = {int(k): v for k, v in mapping["idx_to_label"].items()}
    section_names = [idx_to_label[i] for i in sorted(idx_to_label.keys())]

    test_df = pd.read_csv(TEST_SPLIT_PATH)
    tokenizer = BertTokenizer.from_pretrained(str(TOKENIZER_DIR))
    model = TFBertForSequenceClassification.from_pretrained(str(MODEL_DIR))

    enc = tokenizer(
        list(test_df["text"]),
        padding="max_length",
        truncation=True,
        max_length=MAX_SEQ_LEN,
        return_tensors="np",
    )
    X = {
        "input_ids": enc["input_ids"].astype(np.int32),
        "attention_mask": enc["attention_mask"].astype(np.int32),
        "token_type_ids": enc["token_type_ids"].astype(np.int32),
    }
    y_true = np.array(
        [mapping["label_to_idx"][s] for s in test_df["section"]],
        dtype=np.int32,
    )

    outputs = model.predict(X, batch_size=BATCH_SIZE, verbose=0)
    logits = outputs.logits if hasattr(outputs, "logits") else outputs[0]
    probs = tf.nn.softmax(logits, axis=-1).numpy()
    y_pred = np.argmax(probs, axis=1)

    acc = accuracy_score(y_true, y_pred)
    prec = precision_score(y_true, y_pred, average="weighted", zero_division=0)
    rec = recall_score(y_true, y_pred, average="weighted", zero_division=0)
    f1 = f1_score(y_true, y_pred, average="weighted", zero_division=0)
    cm = confusion_matrix(y_true, y_pred, labels=list(range(len(section_names))))

    print("=" * 60)
    print("Pitch Analyzer — Test Evaluation")
    print("=" * 60)
    print(f"Test samples : {len(test_df)}")
    print(f"Accuracy     : {acc:.4f}")
    print(f"Precision    : {prec:.4f}")
    print(f"Recall       : {rec:.4f}")
    print(f"F1           : {f1:.4f}")
    print()
    print(classification_report(y_true, y_pred,
                                labels=list(range(len(section_names))),
                                target_names=section_names,
                                zero_division=0))

    plt.figure(figsize=(9, 7))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=section_names, yticklabels=section_names)
    plt.xlabel("Predicted")
    plt.ylabel("Actual")
    plt.title("Pitch-Deck Section Classifier — Confusion Matrix (Test Set)")
    plt.tight_layout()
    plt.savefig(CONFUSION_PLOT_PATH, dpi=120)
    plt.close()

    metrics = {
        "accuracy": float(acc),
        "precision_weighted": float(prec),
        "recall_weighted": float(rec),
        "f1_weighted": float(f1),
        "confusion_matrix": cm.tolist(),
        "class_names": section_names,
        "test_samples": int(len(test_df)),
        "synthetic_data_warning": (
            "All metrics are computed on synthetic development-only data and "
            "do not represent real-world startup or investment performance."
        ),
    }
    with open(METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"Metrics saved to: {METRICS_PATH}")
    print(f"Confusion matrix saved to: {CONFUSION_PLOT_PATH}")
    print("=" * 60)


if __name__ == "__main__":
    main()