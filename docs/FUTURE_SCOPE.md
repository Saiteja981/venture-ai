# Future Scope

The following items are **proposed** and are **not implemented** in the current version of Venture AI. They are organized by area.

## 1. Data

- Replace the synthetic dataset with real (anonymized) startup and investor records
- Increase dataset size to tens of thousands of records
- Add data validation and provenance tracking
- Introduce time-aware splits for realistic backtesting

## 2. AI & Machine Learning

- Explainable AI: SHAP or LIME explanations for the potential score
- Uncertainty quantification (Bayesian or conformal prediction)
- Reverse recommendation: startup → investors
- Real-time embedding updates as new profiles are created
- Advanced pitch analysis:
    - Full-document summarization
    - Sentiment and tone detection
    - Deck-to-competitor comparison
- Multi-modal pitch analysis (slides as images + text)
- Model retraining pipeline with versioning and rollback
- MLOps: automated retraining on drift detection

## 3. Platform Features

- Real-time notifications (WebSocket or SSE)
- In-platform messaging between investors and founders
- Investment workflow: interest tracking → term sheet → deal status
- Portfolio tracking for investors
- Advanced search and filtering on Discover Startups
- "Saved for later" lists
- Document upload and storage (pitch decks, financials)

## 4. Backend & Infrastructure

- Add Spring Boot Actuator for proper health checks
- Add Flyway or Liquibase for schema migrations
- Add rate limiting and API throttling
- Add request tracing (OpenTelemetry)
- Add centralized logging (ELK or similar)
- Add caching layer (Redis) for repeated AI calls

## 5. Frontend

- Server-side rendering (SSR) via Next.js or Vite SSR for SEO
- Internationalization (i18n)
- Dark mode
- Accessibility audit (WCAG AA)
- Progressive web app (PWA) support
- Real-time charts that update as data changes

## 6. Deployment & Operations

- Cloud deployment (AWS, GCP, Azure, or a managed container platform)
- CI/CD pipeline (GitHub Actions) with automated tests
- Blue/green or canary deployments
- Autoscaling for the AI service
- Secrets management (Vault, AWS Secrets Manager)
- HTTPS with managed certificates
- Domain name and DNS configuration
- Production monitoring and alerting

## 7. Security & Compliance

- Move JWT storage to httpOnly cookies + CSRF tokens
- Add multi-factor authentication
- Add refresh tokens with rotation
- Add security headers (CSP, HSTS, X-Frame-Options)
- Conduct a formal security audit
- Implement GDPR data subject rights
- Add fairness and bias analysis for the models

## 8. Product

- Mobile apps (React Native or native)
- Team collaboration: multiple users per startup account
- Investment analytics for investors (portfolio performance)
- Benchmarking against anonymized peers
- Case studies and success stories

## 9. Research Directions

- Comparing two-tower against graph neural network recommenders
- Domain-adaptive BERT fine-tuning on public pitch decks
- Multi-task learning: predict potential, risk, and time-to-next-round jointly
- Causal inference on startup success factors

None of the above is implemented. All are labeled for future work.