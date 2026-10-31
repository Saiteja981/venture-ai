# Venture AI — AI-Powered Venture Capital Platform

**Version:** 1.0.0  
**Stack:** React + Spring Boot 3.3 + FastAPI + MySQL  
**Status:** Production Ready (Phase 29 Certified)

## Architecture
1. **Frontend (React/Vite):** SPA with role-based dashboards (Startup, Investor, Admin).
2. **Backend (Spring Boot):** REST API, JWT Security, Google OAuth, MySQL persistence, PDF/CSV generation.
3. **AI Service (FastAPI):** 3 Keras/TensorFlow models (Potential/Risk MLP, Two-Tower Recommender, BERT Pitch Analyzer).

## How to Run Locally
1. MySQL: Ensure `venture_ai_db` exists.
2. Backend: `cd backend && .\mvnw.cmd spring-boot:run` (Port 8082)
3. AI Service: `cd ai-service && .\.venv\Scripts\Activate.ps1 && uvicorn app.main:app --port 8000`
4. Frontend: `cd frontend && npm run dev` (Port 5173)

## How to Run Tests
- **Backend:** `cd backend && .\mvnw.cmd clean test` (187 tests)
- **AI Service:** `cd ai-service && pytest tests/` (120 tests)
- **Frontend Build:** `cd frontend && npm run build` (2637 modules)
- **E2E Integration:** `.\api-e2e.ps1` (Requires all 3 services running)
- **Browser E2E:** `cd frontend && npx playwright test`

## API Documentation
Interactive Swagger UI available at `http://localhost:8082/swagger-ui.html` when the backend is running.