"""
Phase 8.5 — Fine-tune BERT for pitch-deck section classification.

Backbone
--------
bert-base-uncased, wrapped in HuggingFace's `TFBertForSequenceClassification`.

Why HuggingFace's TF class instead of a raw Keras functional model?
-------------------------------------------------------------------
Transformers' TF models depend on the legacy `tf-keras` API, which is not
compatible with Keras 3 that ships with TensorFlow 2.20. Installing the
`tf-keras` package provides the required backend. HuggingFace's own
`TFBertForSequenceClassification` is the canonical, well-tested way to
fine-tune BERT under this setup and saves into a portable directory format.

Outputs
-------
saved_models/pitch_analyzer/
├── model/                        HuggingFace model directory
├── tokenizer/                    HF tokenizer
├── label_mapping.json            {"Problem": 0, ...}
├── metadata.json                 training config
└── test_split.csv                test split used by evaluate_pitch_analyzer.py

Usage
-----
    python training/train_pitch_analyzer.py
    python training/train_pitch_analyzer.py --trainable-layers 12 --epochs 8
"""

from __future__ import annotations

import argparse
import json
import os
import random
import sys
from pathlib import Path

os.environ["PYTHONHASHSEED"] = "42"
os.environ["TF_DETERMINISTIC_OPS"] = "1"
os.environ["TF_CUDNN_DETERMINISTIC"] = "1"
 # make TF-BERT use tf-keras

_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import numpy as np
import pandas as pd
import tensorflow as tf
import tf_keras as keras
from sklearn.model_selection import train_test_split
from transformers import (
    BertTokenizer,
    TFBertForSequenceClassification,
    create_optimizer,
)

RANDOM_SEED: int = 42
PROJECT_ROOT = _PROJECT_ROOT
RAW_DIR = PROJECT_ROOT / "data" / "raw"
DATASET_CSV = RAW_DIR / "pitch_deck_sections.csv"

SAVED_DIR = PROJECT_ROOT / "saved_models" / "pitch_analyzer"
MODEL_DIR = SAVED_DIR / "model"
TOKENIZER_DIR = SAVED_DIR / "tokenizer"
LABEL_MAPPING_PATH = SAVED_DIR / "label_mapping.json"
METADATA_PATH = SAVED_DIR / "metadata.json"
TEST_SPLIT_PATH = SAVED_DIR / "test_split.csv"

MODEL_NAME = "bert-base-uncased"
MAX_SEQ_LEN = 128
BATCH_SIZE = 8
MAX_EPOCHS = 10
LEARNING_RATE = 2e-5
EARLY_STOP_PATIENCE = 3

SECTIONS = [
    "Problem", "Solution", "Market", "Product",
    "Business Model", "Traction", "Competition", "Team",
]

LABEL_TO_IDX = {s: i for i, s in enumerate(SECTIONS)}
IDX_TO_LABEL = {i: s for s, i in LABEL_TO_IDX.items()}


def set_global_seeds(seed: int = RANDOM_SEED) -> None:
    random.seed(seed)
    np.random.seed(seed)
    tf.random.set_seed(seed)


def load_dataset() -> pd.DataFrame:
    if not DATASET_CSV.exists():
        raise FileNotFoundError(
            f"Dataset not found: {DATASET_CSV}. "
            "Run training/generate_pitch_deck_dataset.py first."
        )
    df = pd.read_csv(DATASET_CSV)
    df = df.dropna(subset=["section", "text"]).copy()
    df["text"] = df["text"].astype(str).str.strip()
    df = df[df["text"].str.len() > 0]
    df["section"] = df["section"].astype(str).str.strip()
    invalid = set(df["section"].unique()) - set(SECTIONS)
    if invalid:
        raise ValueError(f"Unexpected section labels: {invalid}")
    df = df.drop_duplicates(subset=["text"], keep="first").reset_index(drop=True)
    return df


def split_dataset(df: pd.DataFrame):
    train_df, temp_df = train_test_split(
        df, test_size=0.30, stratify=df["section"], random_state=RANDOM_SEED,
    )
    val_df, test_df = train_test_split(
        temp_df, test_size=0.50, stratify=temp_df["section"],
        random_state=RANDOM_SEED,
    )
    return (
        train_df.reset_index(drop=True),
        val_df.reset_index(drop=True),
        test_df.reset_index(drop=True),
    )


def encode(df: pd.DataFrame, tokenizer: BertTokenizer):
    enc = tokenizer(
        list(df["text"]),
        padding="max_length",
        truncation=True,
        max_length=MAX_SEQ_LEN,
        return_tensors="np",
    )
    return {
        "input_ids": enc["input_ids"].astype(np.int32),
        "attention_mask": enc["attention_mask"].astype(np.int32),
        "token_type_ids": enc["token_type_ids"].astype(np.int32),
    }


def build_tf_dataset(enc: dict, labels: np.ndarray, shuffle: bool, seed: int = RANDOM_SEED):
    ds = tf.data.Dataset.from_tensor_slices((enc, labels))
    if shuffle:
        ds = ds.shuffle(buffer_size=len(labels), seed=seed, reshuffle_each_iteration=True)
    ds = ds.batch(BATCH_SIZE)
    return ds


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--trainable-layers", type=int, default=4,
                        help="Number of final BERT encoder layers to fine-tune.")
    parser.add_argument("--epochs", type=int, default=MAX_EPOCHS)
    args = parser.parse_args()

    SAVED_DIR.mkdir(parents=True, exist_ok=True)
    set_global_seeds(RANDOM_SEED)

    print("=" * 70)
    print("Phase 8.5 — BERT Pitch-Deck Section Classifier")
    print("=" * 70)
    print(f"TensorFlow     : {tf.__version__}")
    print(f"tf-keras       : {keras.__version__}")
    print(f"Backbone       : {MODEL_NAME}")
    print(f"Trainable layers: {args.trainable_layers}")

    df = load_dataset()
    print(f"Loaded dataset : {len(df)} records")

    train_df, val_df, test_df = split_dataset(df)
    print(f"Train / Val / Test: {len(train_df)} / {len(val_df)} / {len(test_df)}")

    # Save test split for evaluation
    test_df.to_csv(TEST_SPLIT_PATH, index=False)

    print("Loading tokenizer ...")
    tokenizer = BertTokenizer.from_pretrained(MODEL_NAME)
    tokenizer.save_pretrained(str(TOKENIZER_DIR))
    print(f"Tokenizer saved to: {TOKENIZER_DIR}")

    X_train, y_train = encode(train_df, tokenizer), np.array(
        [LABEL_TO_IDX[s] for s in train_df["section"]], dtype=np.int32)
    X_val, y_val = encode(val_df, tokenizer), np.array(
        [LABEL_TO_IDX[s] for s in val_df["section"]], dtype=np.int32)

    train_ds = build_tf_dataset(X_train, y_train, shuffle=True)
    val_ds = build_tf_dataset(X_val, y_val, shuffle=False)

    print("Loading pretrained BERT ...")
    # Load native TF weights (tf_model.h5) from the local cache. No PyTorch required.
    model = TFBertForSequenceClassification.from_pretrained(
        MODEL_NAME, num_labels=len(SECTIONS),
    )

    # Optionally freeze all but the last N encoder layers
    total_layers = 12
    unfreeze_from = max(0, total_layers - args.trainable_layers)
    for i, layer in enumerate(model.bert.encoder.layer):
        layer.trainable = i >= unfreeze_from
    # Keep embeddings frozen to save compute; pooler is trained
    model.bert.embeddings.trainable = False
    model.bert.pooler.trainable = True

    # Optimizer with linear warmup
    steps_per_epoch = max(1, len(train_df) // BATCH_SIZE)
    total_steps = steps_per_epoch * args.epochs
    optimizer, lr_schedule = create_optimizer(
        init_lr=LEARNING_RATE,
        num_train_steps=total_steps,
        num_warmup_steps=int(0.1 * total_steps),
        weight_decay_rate=0.01,
    )

    loss = keras.losses.SparseCategoricalCrossentropy(from_logits=True)
    model.compile(
        optimizer=optimizer,
        loss=loss,
        metrics=["accuracy"],
    )

    callbacks = [
        keras.callbacks.EarlyStopping(
            monitor="val_loss", patience=EARLY_STOP_PATIENCE,
            restore_best_weights=True, verbose=1,
        ),
    ]

    history = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=args.epochs,
        callbacks=callbacks,
        verbose=2,
    )

    # Save model, tokenizer, mapping, metadata
    model.save_pretrained(str(MODEL_DIR))
    tokenizer.save_pretrained(str(TOKENIZER_DIR))
    print(f"\nModel saved to : {MODEL_DIR}")

    with open(LABEL_MAPPING_PATH, "w", encoding="utf-8") as f:
        json.dump({"label_to_idx": LABEL_TO_IDX, "idx_to_label": IDX_TO_LABEL},
                  f, indent=2)
    print(f"Label mapping saved to: {LABEL_MAPPING_PATH}")

    # Extract history as plain floats
    hist = {k: [float(v) for v in vals] for k, vals in history.history.items()}

    metadata = {
        "project": "AI-Powered Venture Capital and Startup Collaboration Platform",
        "model_name": "pitch_analyzer",
        "backbone": MODEL_NAME,
        "model_class": "TFBertForSequenceClassification",
        "tensorflow_version": tf.__version__,
        "tf_keras_version": keras.__version__,
        "random_seed": RANDOM_SEED,
        "max_seq_len": MAX_SEQ_LEN,
        "batch_size": BATCH_SIZE,
        "learning_rate": LEARNING_RATE,
        "epochs_run": len(hist["loss"]),
        "trainable_layers": args.trainable_layers,
        "num_classes": len(SECTIONS),
        "sections": SECTIONS,
        "train_samples": int(len(train_df)),
        "val_samples": int(len(val_df)),
        "test_samples": int(len(test_df)),
        "final_train_loss": hist["loss"][-1],
        "final_val_loss": hist["val_loss"][-1],
        "final_val_accuracy": hist["val_accuracy"][-1],
        "best_val_loss": float(min(hist["val_loss"])),
        "synthetic_data_warning": (
            "This model was trained on synthetic development-only pitch-deck "
            "data. It must not be used to evaluate real startups or make "
            "investment decisions."
        ),
    }
    with open(METADATA_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Metadata saved to: {METADATA_PATH}")
    print("=" * 70)


if __name__ == "__main__":
    main()