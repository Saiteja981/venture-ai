# Demo Flow

**Duration:** 5–10 minutes
**Setup:** all four containers running (`docker compose up -d`), browser open at `http://localhost:5173`

---

## Before You Start

Confirm:

```powershell
docker compose ps
```

All four should show `running` or `running (healthy)`. If not, wait 30 s and retry.

Have a pre-registered user ready: `demo.startup@venture.ai` / `Password123!` with a completed startup profile (run through steps 3–4 once before the demo).

---

## Script

### 0:00–0:30 — Landing page

Open `http://localhost:5173`. Point out:

- The three feature cards: Startup Intelligence, Smart Matching, Analytics & Insights
- "Get Started" and "Sign In"

Say: *"This is the entry point. The platform has three roles — Startup, Investor, Admin — each with its own dashboard."*

### 0:30–1:00 — Login

Click **Sign In**. Log in as the STARTUP demo user.

Say: *"Authentication uses JWT with BCrypt-hashed passwords. The token is stored client-side and attached to every API call."*

### 1:00–2:00 — Startup profile

Sidebar → **My Startup**.

Show the profile view: company name, badges (industry / business model / stage), metrics, financials.

Say: *"The startup profile captures the financial, traction, and team data the AI model needs. Every field is stored in MySQL and tied to this user via an ownership check."*

Click **Edit Profile** briefly to show the form, then Cancel.

### 2:00–3:00 — Startup analytics

Sidebar → **Startup Analytics**.

Show the metric grid and the four charts.

Say: *"These charts visualize the same profile data — funding vs burn, users vs customers, revenue growth, runway. All live from MySQL."*

Scroll to the **AI Summary** section, click **Run AI Analysis**. Wait for the score card.

Say: *"This is the first model. It's a multi-output neural network that returns a potential score from 0 to 100 and a risk category. This call just went from the browser to the FastAPI container."*

### 3:00–4:00 — Pitch Deck Analyzer

Sidebar → **Pitch Deck**. Paste the sample text (see `docs/SCREENSHOTS.md` for the exact text). Click **Analyze Pitch Deck**.

Say: *"The second model is a fine-tuned BERT classifier. It splits the pitch text into chunks, classifies each chunk into one of eight sections, and computes how complete the deck is. Notice the per-section bars and the information gaps list."*

### 4:00–4:30 — Logout and switch role

Click the logout icon. Log in as the INVESTOR demo user.

Say: *"Switching roles gives us a different sidebar. Role-based routing prevents STARTUP users from reaching investor or admin pages."*

### 4:30–5:30 — Investor profile and recommendations

Sidebar → **My Profile** — brief view of the investor profile.

Sidebar → **Recommended Startups**. Click **Find Matches**.

Say: *"This is the third model — a two-tower recommender. It embeds the investor profile and each startup profile into a shared vector space and ranks by cosine similarity. These five ranked cards are the model's actual output."*

### 5:30–6:30 — AI Insights

Sidebar → **AI Insights**. Click **Load Insights**.

Say: *"This page aggregates the same recommendation call — it computes count, top score, and average score, and visualizes the ranking."*

### 6:30–7:30 — Admin analytics

Log out. Log in as the ADMIN user.

Sidebar → **Platform Analytics**.

Say: *"This shows live database counts — total users, startups, investors, admins — plus distribution charts by role, industry, stage, and investor type. All computed from MySQL."*

Sidebar → **AI Analytics**.

Say: *"This page shows the model metadata and evaluation metrics that were persisted at training time. Notice the prominent synthetic-data warning at the top — these are metrics on generated data, not real-world performance."*

### 7:30–8:30 — Docker architecture

Switch to a terminal. Run:

```powershell
docker compose ps
```

Say: *"All four services are containers. The frontend is served by Nginx, the backend is a Spring Boot fat JAR, the AI service is Uvicorn with TensorFlow, and MySQL persists in a named volume."*

Show `docker compose config | Select-String "published"` to point out the host port mappings.

### 8:30–9:30 — Explain models and limitations

Say: *"Three models: MLP for prediction, two-tower for recommendation, BERT for pitch analysis. All trained on synthetic development data. Metrics are shown on the AI Analytics page, but they describe fit on the generated data — not real investment performance. The recommendation direction is investor to startups. Reversing it is future work."*

### 9:30–10:00 — Wrap

Say: *"Everything you saw is reproducible with `docker compose up -d`. The README covers setup, the docs folder covers limitations, future scope, and viva preparation. Thank you."*

---

## Fallback Plan

If the AI service is down during the demo:

- The AI pages will show "AI service is currently unavailable" with a Retry button. This is a designed error state — it demonstrates robustness.
- Restart with `docker compose up -d ai-service`. It should be healthy within ~30 s.

If the backend is down:

- Login will fail. Restart with `docker compose up -d backend`.

If MySQL is down:

- Backend will fail to connect. Restart with `docker compose up -d mysql`.

## Backup: Local Dev Mode

If Docker is broken and time is short:

1. Stop all containers: `docker compose down`
2. Start local MySQL (already running on 3306)
3. `cd backend && .\mvnw.cmd spring-boot:run`
4. `cd ai-service && .\.venv\Scripts\Activate.ps1; uvicorn app.main:app --reload --port 8000`
5. `cd frontend && npm run dev`

Frontend will still call `http://localhost:8080/api` (local) instead of `8081` — no rebuild needed for local dev.