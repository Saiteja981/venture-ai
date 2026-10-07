# AI-Powered Venture Capital and Startup Collaboration Platform

## 1. Project Overview

This repository contains the **FastAPI AI microservice** for the AI-Powered
Venture Capital and Startup Collaboration Platform.

The service provides AI functionality to the Spring Boot backend, including
deep learning, NLP, and recommendation capabilities. It is intentionally
decoupled from the database — all persistent data flows through the Spring
Boot backend via HTTP.

At this stage, the service exposes only the foundation:
- Service identity endpoint
- Health check endpoint
- Interactive API documentation

No ML models, no database connectivity, and no authentication are implemented yet.

---

## 2. Architecture

React Frontend
↓
Spring Boot Backend
↓
FastAPI AI Service
↓
Deep Learning / NLP Models

---

## 3. Current Status

The current implementation is the **FastAPI foundation only**.

Future modules will include:

- Startup potential prediction (deep learning)
- Risk prediction (classification)
- Investor-startup recommendation (Two-Tower model)
- Pitch deck analysis (BERT / Transformers)
- Data analytics (Pandas, NumPy, scikit-learn, Matplotlib/Seaborn)

None of these are implemented at this stage. Dependencies for them will be
added during the corresponding phases.

---

## 4. Setup

Requires **Python 3.10**.

### Create a virtual environment

```bash
python -m venv .venv

# Phase 8.3 — Startup Potential & Risk Model

## 1. Model Objective

Train a multi-output MLP that predicts, from a startup's features:

1. **Potential Score** — a continuous value in `[0, 100]`
2. **Risk Category** — one of `LOW`, `MEDIUM`, `HIGH`

Both outputs are **synthetic development targets**. The model is a proof of
concept for the AI-Powered Venture Capital and Startup Collaboration Platform.

## 2. Synthetic Target Construction

### `potential_score`

A deterministic, continuous score in `[0, 100]`, computed in
`training/create_model_targets.py`. It uses only features that are
legitimately available *before* prediction:
