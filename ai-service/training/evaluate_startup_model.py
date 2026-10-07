"""
Phase 8.3 — Evaluate the trained startup potential & risk model on the test set.

Reports:
    Potential score : MAE, RMSE, R²
    Risk category   : Accuracy, Precision, Recall, F1, ROC-AUC, Confusion matrix

Saves:
    saved_models/confusion_matrix.png
    saved_models/training_history.png
    saved_models/evaluation_metrics.json

The test set is used ONLY here, after training is complete. No tuning is
performed on the test set.

Usage
-----
    python training/evaluate_startup_model.py
"""

from __future__ import annotations

import json
from pathlib import Path

import joblib
import matplotlib
matplotlib.use("Agg")  # headless backend for script execution
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
    mean_absolute_error,
    mean_squared_error,
    precision_score,
    r2_score,
    recall_score,
    roc_auc_score,
)
from tensorflow import keras

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

PROJECT_ROOT: Path = Path(__file__).resolve().parents[1]
PROCESSED_DIR: Path = PROJECT_ROOT / "data" / "processed"
SAVED_MODELS_DIR: Path = PROJECT_ROOT / "saved_models"

MODEL_PATH: Path = SAVED_MODELS_DIR / "startup_potential_risk_model.keras"
PREPROCESSOR_PKL: Path = PROCESSED_DIR / "preprocessor.pkl"
TEST_CSV: Path = PROCESSED_DIR / "test_with_targets.csv"
HISTORY_JSON: Path = SAVED_MODELS_DIR / "training_history.json"

CM_PATH: Path = SAVED_MODELS_DIR / "confusion_matrix.png"
HISTORY_PLOT_PATH: Path = SAVED_MODELS_DIR / "training_history.png"
METRICS_PATH: Path = SAVED_MODELS_DIR / "evaluation_metrics.json"

CLASS_NAMES = ["LOW", "MEDIUM", "HIGH"]
FORBIDDEN_FEATURE_COLUMNS = {"potential_score", "risk_category", "outcome_class",
                             "startup_id", "company_name"}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def load_test_set(preprocessor):
    df = pd.read_csv(TEST_CSV)
    y_pot = df["potential_score"].astype(float).values
    y_risk = df["risk_category"].astype(int).values

    X_df = df.drop(columns=list(FORBIDDEN_FEATURE_COLUMNS), errors="ignore")
    X = np.asarray(preprocessor.transform(X_df), dtype=np.float32)
    return X, y_pot, y_risk


def plot_confusion_matrix(cm: np.ndarray, path: Path) -> None:
    plt.figure(figsize=(6, 5))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=CLASS_NAMES, yticklabels=CLASS_NAMES)
    plt.xlabel("Predicted")
    plt.ylabel("Actual")
    plt.title("Risk Category — Confusion Matrix (Test Set)")
    plt.tight_layout()
    plt.savefig(path, dpi=120)
    plt.close()
    print(f"Confusion matrix saved to : {path}")


def plot_training_history(path: Path) -> None:
    if not HISTORY_JSON.exists():
        print(f"[warn] No training history found at {HISTORY_JSON}; skipping plot.")
        return

    with open(HISTORY_JSON, "r", encoding="utf-8") as f:
        history = json.load(f)

    fig, axes = plt.subplots(1, 2, figsize=(14, 5))

    # Loss
    axes[0].plot(history["loss"], label="train_loss")
    axes[0].plot(history["val_loss"], label="val_loss")
    axes[0].set_title("Total Loss")
    axes[0].set_xlabel("Epoch")
    axes[0].set_ylabel("Loss")
    axes[0].legend()
    axes[0].grid(True)

    # Risk accuracy
    if "risk_category_accuracy" in history:
        axes[1].plot(history["risk_category_accuracy"], label="train_acc")
        axes[1].plot(history["val_risk_category_accuracy"], label="val_acc")
        axes[1].set_title("Risk Category Accuracy")
        axes[1].set_xlabel("Epoch")
        axes[1].set_ylabel("Accuracy")
        axes[1].legend()
        axes[1].grid(True)

    plt.tight_layout()
    plt.savefig(path, dpi=120)
    plt.close()
    print(f"Training history plot saved to : {path}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    print("=" * 70)
    print("Startup Potential Model Evaluation")
    print("=" * 70)

    if not MODEL_PATH.exists():
        raise FileNotFoundError(
            f"Model not found: {MODEL_PATH}. "
            "Run `python training/train_startup_model.py` first."
        )

    preprocessor = joblib.load(PREPROCESSOR_PKL)
    X_test, y_pot_test, y_risk_test = load_test_set(preprocessor)
    print(f"Test X shape     : {X_test.shape}")
    print(f"Test risk counts : {dict(zip(*np.unique(y_risk_test, return_counts=True)))}")

    model = keras.models.load_model(MODEL_PATH)
    pot_pred, risk_pred_proba = model.predict(X_test, verbose=0)
    pot_pred = pot_pred.reshape(-1)
    risk_pred = np.argmax(risk_pred_proba, axis=1)

    # -------------------------------------------------------------------
    # Potential score metrics
    # -------------------------------------------------------------------
    mae = mean_absolute_error(y_pot_test, pot_pred)
    rmse = float(np.sqrt(mean_squared_error(y_pot_test, pot_pred)))
    r2 = r2_score(y_pot_test, pot_pred)

    # -------------------------------------------------------------------
    # Risk classification metrics
    # -------------------------------------------------------------------
    acc = accuracy_score(y_risk_test, risk_pred)
    prec = precision_score(y_risk_test, risk_pred, average="weighted", zero_division=0)
    rec = recall_score(y_risk_test, risk_pred, average="weighted", zero_division=0)
    f1 = f1_score(y_risk_test, risk_pred, average="weighted", zero_division=0)

    # Multiclass ROC-AUC (one-vs-rest). Requires all classes present in y_true.
    try:
        roc_auc = roc_auc_score(
            y_risk_test,
            risk_pred_proba,
            multi_class="ovr",
            average="weighted",
            labels=[0, 1, 2],
        )
    except ValueError as exc:
        roc_auc = float("nan")
        print(f"[warn] ROC-AUC unavailable: {exc}")

    cm = confusion_matrix(y_risk_test, risk_pred, labels=[0, 1, 2])

    # -------------------------------------------------------------------
    # Print report
    # -------------------------------------------------------------------
    print()
    print("Potential Score:")
    print(f"MAE:  {mae:.4f}")
    print(f"RMSE: {rmse:.4f}")
    print(f"R²:   {r2:.4f}")
    print()
    print("Risk Classification:")
    print(f"Accuracy:  {acc:.4f}")
    print(f"Precision: {prec:.4f}")
    print(f"Recall:    {rec:.4f}")
    print(f"F1:        {f1:.4f}")
    print(f"ROC-AUC:   {roc_auc:.4f}")
    print()
    print("Classification Report (per class):")
    print(classification_report(y_risk_test, risk_pred,
                                labels=[0, 1, 2], target_names=CLASS_NAMES,
                                zero_division=0))
    print("Confusion Matrix (rows=actual, cols=predicted):")
    print(pd.DataFrame(cm, index=CLASS_NAMES, columns=CLASS_NAMES).to_string())
    print()

    # -------------------------------------------------------------------
    # Save artifacts
    # -------------------------------------------------------------------
    plot_confusion_matrix(cm, CM_PATH)
    plot_training_history(HISTORY_PLOT_PATH)

    metrics = {
        "potential_score": {
            "mae": float(mae),
            "rmse": float(rmse),
            "r2": float(r2),
        },
        "risk_category": {
            "accuracy": float(acc),
            "precision_weighted": float(prec),
            "recall_weighted": float(rec),
            "f1_weighted": float(f1),
            "roc_auc_weighted_ovr": float(roc_auc) if roc_auc == roc_auc else None,
            "confusion_matrix": cm.tolist(),
            "class_names": CLASS_NAMES,
        },
        "synthetic_data_warning": (
            "Metrics computed on synthetic development data. Not representative "
            "of real-world startup success or investment returns."
        ),
    }
    with open(METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"Metrics JSON saved to : {METRICS_PATH}")
    print("=" * 70)


if __name__ == "__main__":
    main()