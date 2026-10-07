"""
FastAPI entry point for the Venture AI Service.

The three ML models from Phases 8.3–8.5 are exposed through versioned
/api/v1/* endpoints. Models are loaded lazily on first request so the
service starts quickly.
"""

from __future__ import annotations

import os

# Pin Keras 3 in sys.modules BEFORE any router import can trigger transformers.
# transformers (used by the BERT pitch analyzer) forces TF_USE_LEGACY_KERAS=1,
# which would otherwise shadow Keras 3 with tf-keras and break loading of the
# Phase 8.3 `.keras` model.
os.environ.pop("TF_USE_LEGACY_KERAS", None)
import keras  # noqa: F401,E402

from pathlib import Path  # noqa: E402

from dotenv import load_dotenv  # noqa: E402
from fastapi import FastAPI  # noqa: E402
from fastapi.middleware.cors import CORSMiddleware  # noqa: E402

from app.api.model_info import router as model_info_router  # noqa: E402
from app.api.pitch_analysis import router as pitch_router  # noqa: E402
from app.api.prediction import router as prediction_router  # noqa: E402
from app.api.recommendation import router as recommendation_router  # noqa: E402

load_dotenv()

APP_NAME = os.getenv("APP_NAME", "venture-ai-service")
APP_VERSION = os.getenv("APP_VERSION", "1.0.0")

_default_origins = "http://localhost:5173,http://localhost:8080"
CORS_ORIGINS = [
    origin.strip()
    for origin in os.getenv("CORS_ORIGINS", _default_origins).split(",")
    if origin.strip()
]

_PROJECT_ROOT = Path(__file__).resolve().parents[1]

app = FastAPI(
    title="Venture AI Service",
    description=(
        "AI microservice for the AI-Powered Venture Capital and Startup "
        "Collaboration Platform. Exposes three trained models:\n"
        "- Startup potential & risk prediction (Phase 8.3)\n"
        "- Investor–startup two-tower recommendation (Phase 8.4)\n"
        "- BERT-based pitch-deck analysis (Phase 8.5)\n\n"
        "All models are trained on synthetic development data and do not "
        "represent real-world investment advice."
    ),
    version=APP_VERSION,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(prediction_router)
app.include_router(recommendation_router)
app.include_router(pitch_router)
app.include_router(model_info_router)


def _model_artifacts_present() -> dict[str, bool]:
    """Check whether trained artifacts exist on disk (no loading)."""
    return {
        "startup_prediction": (
            _PROJECT_ROOT / "saved_models" / "startup_potential_risk_model.keras"
        ).exists(),
        "two_tower_recommender": (
            _PROJECT_ROOT / "saved_models" / "two_tower_recommender.keras"
        ).exists(),
        "pitch_analyzer": (
            _PROJECT_ROOT / "saved_models" / "pitch_analyzer" / "model"
        ).exists(),
    }


@app.get("/", tags=["meta"], summary="Service identity")
async def root() -> dict:
    return {
        "service": APP_NAME,
        "status": "running",
    }


@app.get("/health", tags=["meta"], summary="Health check")
async def health() -> dict:
    artifacts = _model_artifacts_present()
    return {
        "status": "healthy",
        "service": APP_NAME,
        "model_artifacts": artifacts,
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=os.getenv("APP_HOST", "0.0.0.0"),
        port=int(os.getenv("APP_PORT", "8000")),
        reload=True,
    )