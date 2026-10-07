"""
Phase 8.4 — Train the two-tower recommendation model.

Architecture
------------
Startup Tower: 36 → 128 → BN → Dropout(0.2) → 64 → BN → Dropout(0.2) → 32 → L2Norm
Investor Tower: N → 128 → BN → Dropout(0.2) → 64 → BN → Dropout(0.2) → 32 → L2Norm

Compatibility = dot(startup_embedding, investor_embedding) ∈ [-1, 1]
Compatibility score (0-100) = 50 * (dot + 1)

Loss: binary crossentropy on sigmoid(dot * 5).

Custom layers
-------------
`L2Normalize` and `ScaleBy` are named, registered Keras layers instead of
Lambda layers. Keras 3 disallows Lambda deserialization by default
(safe_mode=True), so a custom layer is the correct way to build a savable
model that loads without unsafe flags.

Usage:
    python training/train_two_tower.py
"""

from __future__ import annotations

import json
import os
import random
import sys
from pathlib import Path

os.environ["PYTHONHASHSEED"] = "42"
os.environ["TF_DETERMINISTIC_OPS"] = "1"
os.environ["TF_CUDNN_DETERMINISTIC"] = "1"

# Make the project root importable so `from training.x import y` works
# when this script is run directly or imported by tests.
_PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(_PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_ROOT))

import joblib
import numpy as np
import pandas as pd
import tensorflow as tf

# Import the standalone Keras 3 package so `keras.saving` is available.
import keras
from keras import layers

RANDOM_SEED: int = 42
EMBEDDING_DIM: int = 32
MAX_EPOCHS: int = 100
BATCH_SIZE: int = 64
LEARNING_RATE: float = 1e-3
EARLY_STOPPING_PATIENCE: int = 10
REDUCE_LR_PATIENCE: int = 5
REDUCE_LR_FACTOR: float = 0.5
MIN_LR: float = 1e-6
SCALE_FACTOR: float = 5.0

PROJECT_ROOT = _PROJECT_ROOT
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR = PROJECT_ROOT / "saved_models"

TRAIN_CSV = PROCESSED_DIR / "recommendation_train.csv"
VAL_CSV = PROCESSED_DIR / "recommendation_validation.csv"
PREPROCESSOR_PKL = PROCESSED_DIR / "recommendation_preprocessor.pkl"

MODEL_PATH = SAVED_MODELS_DIR / "two_tower_recommender.keras"
METADATA_PATH = SAVED_MODELS_DIR / "two_tower_recommender_metadata.json"
HISTORY_PATH = SAVED_MODELS_DIR / "two_tower_history.json"

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
    "preferred_industries", "preferred_stages", "preferred_business_models",
]

TARGET_COL = "compatibility_label"


# ---------------------------------------------------------------------------
# Custom layers
# ---------------------------------------------------------------------------

@keras.saving.register_keras_serializable(package="ventureai")
class L2Normalize(layers.Layer):
    """L2-normalize each row of a batch. Replaces a Lambda layer."""

    def __init__(self, axis: int = 1, **kwargs):
        super().__init__(**kwargs)
        self.axis = axis

    def call(self, inputs):
        return tf.math.l2_normalize(inputs, axis=self.axis)

    def get_config(self):
        cfg = super().get_config()
        cfg.update({"axis": self.axis})
        return cfg


@keras.saving.register_keras_serializable(package="ventureai")
class ScaleBy(layers.Layer):
    """Multiply the input by a constant. Replaces a Lambda layer."""

    def __init__(self, factor: float = 5.0, **kwargs):
        super().__init__(**kwargs)
        self.factor = float(factor)

    def call(self, inputs):
        return inputs * self.factor

    def get_config(self):
        cfg = super().get_config()
        cfg.update({"factor": self.factor})
        return cfg


# ---------------------------------------------------------------------------
# Reproducibility
# ---------------------------------------------------------------------------

def set_global_seeds(seed: int = RANDOM_SEED) -> None:
    random.seed(seed)
    np.random.seed(seed)
    tf.random.set_seed(seed)
    try:
        tf.config.experimental.enable_op_determinism()
    except Exception:
        print("[warn] Could not enable full TF op determinism.")


# ---------------------------------------------------------------------------
# Data
# ---------------------------------------------------------------------------

def _load_split(path: Path, preprocessors) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    df = pd.read_csv(path)
    st = preprocessors["startup"].transform(df[STARTUP_NUMERIC + STARTUP_CATEGORICAL])
    inv = preprocessors["investor"].transform(df[INVESTOR_NUMERIC + INVESTOR_CATEGORICAL])
    y = df[TARGET_COL].astype(np.float32).values
    return (
        np.asarray(st, dtype=np.float32),
        np.asarray(inv, dtype=np.float32),
        y,
    )


# ---------------------------------------------------------------------------
# Model
# ---------------------------------------------------------------------------

def _tower(input_dim: int, name: str) -> keras.Model:
    inputs = keras.Input(shape=(input_dim,), name=f"{name}_input")
    x = layers.Dense(128, activation="relu", name=f"{name}_dense_1")(inputs)
    x = layers.BatchNormalization(name=f"{name}_bn_1")(x)
    x = layers.Dropout(0.2, name=f"{name}_drop_1")(x)
    x = layers.Dense(64, activation="relu", name=f"{name}_dense_2")(x)
    x = layers.BatchNormalization(name=f"{name}_bn_2")(x)
    x = layers.Dropout(0.2, name=f"{name}_drop_2")(x)
    x = layers.Dense(EMBEDDING_DIM, activation=None, name=f"{name}_embedding_raw")(x)
    x = L2Normalize(name=f"{name}_embedding")(x)
    return keras.Model(inputs=inputs, outputs=x, name=f"{name}_tower")


def build_two_tower(startup_dim: int, investor_dim: int) -> keras.Model:
    startup_input = keras.Input(shape=(startup_dim,), name="startup_features")
    investor_input = keras.Input(shape=(investor_dim,), name="investor_features")

    startup_tower = _tower(startup_dim, "startup")
    investor_tower = _tower(investor_dim, "investor")

    st_emb = startup_tower(startup_input)
    inv_emb = investor_tower(investor_input)

    dot = layers.Dot(axes=1, normalize=False, name="dot_product")([st_emb, inv_emb])
    scaled = ScaleBy(factor=SCALE_FACTOR, name="scaled_dot")(dot)
    prob = layers.Activation("sigmoid", name="compatibility_prob")(scaled)

    model = keras.Model(
        inputs=[startup_input, investor_input],
        outputs=prob,
        name="two_tower_recommender",
    )
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=LEARNING_RATE),
        loss="binary_crossentropy",
        metrics=["accuracy", keras.metrics.AUC(name="auc")],
    )
    return model


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    SAVED_MODELS_DIR.mkdir(parents=True, exist_ok=True)
    set_global_seeds(RANDOM_SEED)

    print("=" * 70)
    print("Phase 8.4 — Two-Tower Recommender Training")
    print("=" * 70)
    print(f"TensorFlow version : {tf.__version__}")
    print(f"Keras version      : {keras.__version__}")

    if not PREPROCESSOR_PKL.exists():
        raise FileNotFoundError(
            f"Preprocessors not found: {PREPROCESSOR_PKL}. "
            "Run training/preprocess_recommendation.py first."
        )
    preprocessors = joblib.load(PREPROCESSOR_PKL)

    X_st_train, X_inv_train, y_train = _load_split(TRAIN_CSV, preprocessors)
    X_st_val, X_inv_val, y_val = _load_split(VAL_CSV, preprocessors)

    print(f"Startup feature dim  : {X_st_train.shape[1]}")
    print(f"Investor feature dim : {X_inv_train.shape[1]}")
    print(f"Train samples        : {len(y_train)}")
    print(f"Val samples          : {len(y_val)}")
    print(f"Train positive rate  : {y_train.mean():.3f}")

    model = build_two_tower(X_st_train.shape[1], X_inv_train.shape[1])
    model.summary()

    callbacks = [
        keras.callbacks.EarlyStopping(
            monitor="val_loss", patience=EARLY_STOPPING_PATIENCE,
            restore_best_weights=True, verbose=1,
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss", factor=REDUCE_LR_FACTOR,
            patience=REDUCE_LR_PATIENCE, min_lr=MIN_LR, verbose=1,
        ),
    ]

    history = model.fit(
        {"startup_features": X_st_train, "investor_features": X_inv_train},
        y_train,
        validation_data=(
            {"startup_features": X_st_val, "investor_features": X_inv_val},
            y_val,
        ),
        epochs=MAX_EPOCHS,
        batch_size=BATCH_SIZE,
        callbacks=callbacks,
        verbose=2,
    )

    model.save(MODEL_PATH)
    print(f"\nModel saved to     : {MODEL_PATH}")

    metadata = {
        "project_name": "AI-Powered Venture Capital and Startup Collaboration Platform",
        "model_name": "two_tower_recommender",
        "tensorflow_version": tf.__version__,
        "keras_version": keras.__version__,
        "python_version": sys.version,
        "random_seed": RANDOM_SEED,
        "embedding_dim": EMBEDDING_DIM,
        "startup_input_dim": int(X_st_train.shape[1]),
        "investor_input_dim": int(X_inv_train.shape[1]),
        "training_samples": int(len(y_train)),
        "validation_samples": int(len(y_val)),
        "training_config": {
            "max_epochs": MAX_EPOCHS,
            "batch_size": BATCH_SIZE,
            "learning_rate": LEARNING_RATE,
            "optimizer": "Adam",
            "loss": "binary_crossentropy(sigmoid(5 * dot))",
            "custom_layers": ["L2Normalize", "ScaleBy"],
            "callbacks": [
                f"EarlyStopping(patience={EARLY_STOPPING_PATIENCE}, monitor=val_loss)",
                f"ReduceLROnPlateau(factor={REDUCE_LR_FACTOR}, patience={REDUCE_LR_PATIENCE})",
            ],
        },
        "startup_feature_names": STARTUP_NUMERIC + STARTUP_CATEGORICAL,
        "investor_feature_names": INVESTOR_NUMERIC + INVESTOR_CATEGORICAL,
        "compatibility_scaling": (
            "compatibility_score_0_100 = 50 * (dot(l2norm(st_emb), l2norm(inv_emb)) + 1)"
        ),
        "synthetic_data_warning": (
            "This model is trained on synthetic development data. The "
            "compatibility score is a learned similarity signal and does NOT "
            "represent real investment probability, financial advice, or "
            "guaranteed investor-startup matching."
        ),
        "epochs_run": int(len(history.history["loss"])),
        "final_train_loss": float(history.history["loss"][-1]),
        "final_val_loss": float(history.history["val_loss"][-1]),
        "best_val_loss": float(min(history.history["val_loss"])),
    }
    with open(METADATA_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Metadata saved to  : {METADATA_PATH}")

    history_serializable = {k: [float(v) for v in vals]
                            for k, vals in history.history.items()}
    with open(HISTORY_PATH, "w", encoding="utf-8") as f:
        json.dump(history_serializable, f, indent=2)
    print(f"History saved to   : {HISTORY_PATH}")
    print("=" * 70)


if __name__ == "__main__":
    main()