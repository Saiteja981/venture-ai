# Venture AI

**AI-Powered Venture Capital and Startup Collaboration Platform Using Deep Learning and Data Analytics**

---

## 1. Project Overview

### Problem

Early-stage founders and investors face three structural problems:

- **Discovery friction.** Founders cannot find investors whose thesis, stage, and risk profile match their company. Investors cannot efficiently screen the long tail of early-stage startups.
- **Shallow evaluation.** Screening decisions rely on gut feel and a small set of surface metrics. Deeper signals — team composition, growth efficiency, runway quality — are not consistently evaluated.
- **Pitch quality opacity.** Founders rarely learn *why* their pitch deck fails to resonate. Investors have no structured way to compare completeness across decks.

### Solution

Venture AI is a full-stack platform that combines traditional collaboration workflows with three deep-learning models:

1. **Startup Potential & Risk** — predicts a 0–100 potential score and LOW/MEDIUM/HIGH risk category from a startup's financial, traction, and team profile.
2. **Two-Tower Recommendation** — ranks startups for an investor using learned embeddings and cosine similarity.
3. **BERT Pitch Deck Analyzer** — classifies pitch-deck text into 8 sections (Problem, Solution, Market, Product, Business Model, Traction, Competition, Team), reports per-section completeness, and flags information gaps.

### Target Users

| Role | Uses the platform to |
|---|---|
| **Startup** | Create a profile, view analytics, run AI prediction, analyze pitch deck |
| **Investor** | Maintain a preference profile, receive AI-ranked startup recommendations, view insights |
| **Admin** | View platform-level analytics and AI model metadata |

### AI Capabilities

- Deep-learning potential and risk prediction (multi-output neural network)
- Two-tower recommendation model (embedding-based similarity)
- BERT-based section classification and completeness scoring
- Structured information-gap detection

### Analytics Capabilities

- Startup analytics (financial metrics, growth, runway, valuation — with charts)
- Investor insights (recommendation analytics from live model output)
- Admin platform analytics (user, startup, investor distributions)
- Admin AI analytics (model metadata, evaluation metrics)

---

## 2. Key Features

### Startup

- Register / login (JWT)
- Create and manage startup profile
- Startup analytics dashboard with charts (recharts)
- AI Potential & Risk prediction (live model output)
- Pitch Deck Analyzer (live model output)
- Investor Matches — **placeholder** (see Limitations §2)

### Investor

- Register / login (JWT)
- Create and manage investor profile
- Discover Startups — placeholder
- Recommended Startups (live two-tower model output)
- AI Insights (aggregated recommendation analytics)

### Admin

- Admin dashboard
- Users / Startups / Investors — placeholders
- AI Analytics (persisted model metadata + evaluation metrics)
- Platform Analytics (live database counts and distributions)

### Platform

- JWT authentication with role-based authorization
- REST APIs (Spring Boot + FastAPI)
- MySQL persistence
- Docker Compose deployment
- Modular AI microservice

---

## 3. System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                       Browser                           │
│  React 19 + Vite 8 + Tailwind 3 + recharts + Axios      │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
           │ REST (JWT)               │ REST (public)
           ▼                          ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│   Spring Boot 3.3.4  │   │  FastAPI (Python 3.10)       │
│   Java 17            │   │                              │
│   Spring Security    │   │  • Startup Potential & Risk  │
│   Spring Data JPA    │   │  • Two-Tower Recommender     │
│   JWT (JJWT 0.12.6)  │   │  • BERT Pitch Analyzer       │
└──────────┬───────────┘   └──────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│   MySQL 8.0          │
│   venture_ai_db      │
└──────────────────────┘
```

### Layer Responsibilities

| Layer | Responsibility |
|---|---|
| **React Frontend** | UI, routing, auth state, chart rendering, form handling |
| **Spring Boot Backend** | REST API, JWT issuance and validation, role authorization, CRUD, aggregation endpoints |
| **MySQL** | Persists users, startups, investors |
| **FastAPI AI Service** | Loads trained models, exposes three prediction endpoints, returns metadata |
| **Deep Learning Models** | Trained offline (Phase 8); loaded from disk at inference time |

---

## 4. Technology Stack

### Frontend

| Package | Version |
|---|---|
| React | 19.x |
| Vite | 8.x |
| Tailwind CSS | 3.4.x |
| React Router | 7.x |
| Axios | 1.20.x |
| recharts | 3.x |
| lucide-react | 1.52.x |

### Backend

| Component | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.4 |
| Spring Security | 6.x (managed) |
| JJWT | 0.12.6 |
| Spring Data JPA / Hibernate | 6.5.x |
| MySQL Connector/J | managed |
| Lombok | managed |

### AI Service

| Component | Version |
|---|---|
| Python | 3.10.11 |
| FastAPI | latest |
| Uvicorn | latest |
| TensorFlow | 2.20.0 |
| Transformers | 4.46.3 |
| scikit-learn | latest |
| pandas / numpy | latest |
| tf-keras | required by transformers |

### Deployment

- Docker
- Docker Compose v2

---

## 5. AI Models

> **All three models were trained on synthetic development data and must not be interpreted as real investment advice.**

### 5.1 Startup Potential & Risk Model

| Property | Value |
|---|---|
| Type | Multi-output Keras MLP |
| Input features | 36 (18 base + 5 engineered + 13 categorical encodings) |
| Outputs | `potential_score` (0–100 regression), `risk_category` (LOW/MEDIUM/HIGH 3-class) |
| Model version | `startup_potential_risk_v1` |
| Training samples | 210 |
| Validation samples | 45 |
| Epochs run | 92 |
| Potential score MAE | 4.95 |
| Potential score R² | 0.546 |
| Risk accuracy | 64.4% |
| Risk weighted ROC AUC | 0.780 |

The model produces a numeric potential score and a categorical risk band. Both are surfaced on `/startup/ai-potential` and are computed live — never hardcoded.

### 5.2 Two-Tower Recommendation Model

| Property | Value |
|---|---|
| Type | Two-tower neural network with L2-normalized embeddings |
| Embedding dimension | 32 |
| Startup input dim | 36 |
| Investor input dim | 183 |
| Compatibility scoring | `50 * (cosine_similarity + 1)` → range 0–100 |
| Model version | `two_tower_recommender_v1` |
| Training samples | 4200 |
| Validation samples | 900 |
| Epochs run | 21 |
| Precision@5 | 0.019 |
| Recall@5 | 0.017 |
| NDCG@10 | 0.026 |
| Hit Rate@10 | 0.238 |

**Direction:** investor → startups. Given an investor profile, the model ranks the most compatible startups.

### 5.3 BERT Pitch Deck Analyzer

| Property | Value |
|---|---|
| Backbone | `bert-base-uncased` fine-tuned |
| Task | 8-class sentence classification (multi-chunk) |
| Sections | Problem, Solution, Market, Product, Business Model, Traction, Competition, Team |
| Model version | `pitch_analyzer` |
| Max sequence length | 128 |
| Train / val / test | 315 / 68 / 68 |
| Test accuracy | 1.00 (on templated synthetic data — see Limitations) |

The analyzer chunks input text, classifies each chunk, aggregates per-section confidence, computes an overall completeness score, and emits a structured list of information gaps.

---

## 6. Dataset

| Property | Value |
|---|---|
| Type | **Synthetic development data** |
| Source | Generated by scripts in `ai-service/training/` |
| Size | ~300 unique startup records (303 rows, 3 duplicates detected) |
| Train / validation / test | 70% / 15% / 15% |
| Features | Financial, traction, team, funding, stage, industry, business model |
| Engineered features | revenue_per_customer, users_per_team_member, growth_adjusted_runway, funding_to_revenue, burn_to_revenue |

### ⚠️ Critical Disclaimer

> **The dataset is synthetic development data and does not represent real startups, investors, or investment outcomes.**

All metrics shown in this README — MAE, R², ROC AUC, Precision@5, NDCG@10 — are computed on synthetic development splits. They measure model fit on generated data. They say nothing about real-world predictive power, real return on investment, or real investor-startup matching quality.

---

## 7. Backend API Documentation

### Authentication (Spring Boot, public)

| Method | Endpoint | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{name, email, password, role}` | `{token, userId, name, email, role}` |
| POST | `/api/auth/login` | `{email, password}` | `{token, userId, name, email, role}` |

### Startup (Spring Boot, STARTUP role, ownership enforced)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/startups` | Create startup profile |
| GET | `/api/startups/my` | List own startup profiles |
| GET | `/api/startups/{id}` | Read own startup by id (404 if not owned) |
| PUT | `/api/startups/{id}` | Update own startup |
| DELETE | `/api/startups/{id}` | Delete own startup |

### Investor (Spring Boot, INVESTOR role)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/investors` | Create investor profile |
| GET | `/api/investors/me` | Read own investor profile |
| PUT | `/api/investors/me` | Update own investor profile |
| DELETE | `/api/investors/me` | Delete own investor profile |

### Admin (Spring Boot, ADMIN role only)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/stats` | Platform analytics |

### AI Service (FastAPI, public)

| Method | Endpoint | Body | Response |
|---|---|---|---|
| GET | `/health` | — | `{status, service, model_artifacts}` |
| POST | `/api/v1/predict/startup` | 18 features | `{status, potential_score, risk_category, model_version, disclaimer}` |
| POST | `/api/v1/recommendations` | `{investor, top_k}` | `{recommendations[], count, model_version, disclaimer}` |
| POST | `/api/v1/analyze/pitch-deck` | `{text}` | `{overall_completeness_score, sections{}, information_gaps[], summary}` |
| GET | `/api/v1/model-info` | — | `{startup_prediction{}, two_tower_recommender{}, pitch_analyzer{}, note}` |

---

## 8. Security

| Control | Implementation |
|---|---|
| Password hashing | BCrypt (`BCryptPasswordEncoder`, strength 10) |
| Authentication | Stateless JWT signed with HS256 |
| Token subject | User email |
| Token claims | `userId`, `role` |
| Token lifetime | 24 hours (`jwt.expiration=86400000`) |
| Authorization | Role-based (`STARTUP` / `INVESTOR` / `ADMIN`) |
| Ownership checks | `findByIdAndUserId` — cross-owner reads return 404 |
| Protected paths | Everything except `/api/auth/**` requires authentication |
| Admin paths | `/api/admin/**` requires `hasRole('ADMIN')` (defense in depth: `@PreAuthorize` + filter chain) |
| CORS | Restricted to `http://localhost:5173` (Spring) and configurable origins (FastAPI) |
| Error responses | JSON only — no stack traces leak to clients |
| Unauthorized | 401 |
| Forbidden | 403 |
| Cross-owner access | 404 |

Secrets are provided via environment variables (`SPRING_DATASOURCE_*`, `JWT_SECRET`). No real secret is committed to this repository.

---

## 9. Docker Deployment

### Prerequisites

- Docker Desktop 4.20+ (or Docker Engine + Compose v2)
- 6 GB free RAM, ~4 GB free disk

### Commands

```bash
# Build all images (first build: 10–15 min — TensorFlow + BERT copy)
docker compose build

# Start the stack in detached mode
docker compose up -d

# Check service health
docker compose ps

# Tail logs
docker compose logs -f backend
docker compose logs -f ai-service

# Stop all services (volume survives)
docker compose down

# ⚠️ Destructive: also removes the MySQL volume
docker compose down -v
```

### Containers

| Service | Host port | Container port | Notes |
|---|---|---|---|
| `frontend` | 5173 | 80 | Nginx serving Vite build |
| `backend` | 8081 | 8080 | Spring Boot fat JAR |
| `ai-service` | 8000 | 8000 | Uvicorn + TensorFlow |
| `mysql` | 3308 | 3306 | MySQL 8.0, persistent volume |

**Note:** host ports `8081` and `3308` are used to avoid conflicts with local developer services (Spring Boot on 8080, MySQL on 3306). Inside the Docker network, the backend reaches MySQL at `mysql:3306` — the internal architecture is standard.

### Networking

```
Browser ──► localhost:5173 (frontend)
       ├──► localhost:8081 (backend)
       └──► localhost:8000 (ai-service)

backend container ──► mysql container     (mysql:3306)
backend container ──► ai-service container (ai-service:8000)
```

### Persistent Volume

MySQL data lives in the named volume `mysql_data`.

- `docker compose down` → volume survives
- `docker compose up -d` → same data reloaded
- `docker compose down -v` → volume **deleted**

### Environment Variables

All Docker-related variables are documented in `.env.example`. Copy it to `.env` and set the two passwords and `JWT_SECRET`.

---

## 10. Local Development

### Frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

### Backend

```bash
cd backend
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS / Linux
# → http://localhost:8080
```

### AI Service

```bash
cd ai-service
.\.venv\Scripts\Activate.ps1    # Windows
source .venv/bin/activate       # macOS / Linux
uvicorn app.main:app --reload --port 8000
# → http://localhost:8000/health
```

### Local MySQL

Runs on the host at port 3306. The local backend's `application.properties` points to `localhost:3306`.

### Local vs Docker Ports

| Service | Local dev | Docker (host) |
|---|---|---|
| Frontend | 5173 | 5173 |
| Backend | **8080** | **8081** |
| AI service | 8000 | 8000 |
| MySQL | 3306 | 3308 |

Local development and Docker are **mutually exclusive on the same port**. Stop one before starting the other.

---

## 11. Testing

### Backend (`.\mvnw.cmd clean test`)

```
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### AI Service (`pytest -v`)

```
86 passed, 2 xpassed, 1 warning in 83.33s
```

The two xpasses are tests marked `xfail` in Phase 8 that now pass because the Keras 3 / tf-keras import conflict was resolved in Phase 11C.

### Frontend (`npm run build`)

```
✓ 2606 modules transformed.
✓ built in 4.04s
```

### Docker

- All four containers reach `running (healthy)` after `docker compose up -d`
- Frontend reachable at `http://localhost:5173`
- Backend returns 401 on `/` (expected — auth required)
- AI service returns `{"status":"healthy","model_artifacts":{...all true...}}`
- End-to-end register → login → AI prediction verified through the Docker stack

---

## 12. Screenshots / Demo Section

Recommended screenshots for the final report — see `docs/SCREENSHOTS.md` for capture instructions.

1. Landing page
2. Login page
3. Startup dashboard
4. Startup profile
5. Startup analytics
6. AI Potential & Risk
7. Pitch Deck Analyzer
8. Investor dashboard
9. Investor profile
10. Recommended Startups
11. AI Insights
12. Admin dashboard
13. AI Analytics
14. Platform Analytics
15. `docker compose ps` output

---

## Project Layout

```
venture-ai/
├── frontend/           React + Vite + Tailwind
├── backend/            Spring Boot + Java 17
├── ai-service/         FastAPI + TensorFlow + Transformers
├── database/           (empty — schema created by Hibernate)
├── docs/               Phase 15 documentation
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md           (this file)
```

---

## License

Academic project. Not licensed for commercial use.

## Acknowledgments

Built as a demonstration of deep learning applied to venture capital workflows. Uses synthetic data only.