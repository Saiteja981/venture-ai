# Screenshots Checklist

Recommended screenshots for the final academic report / demo. Capture at 1920×1080 or wider.

Save them to `docs/screenshots/` (create the folder). Use clear filenames.

---

## Setup Before Capturing

1. Start the stack:

   ```powershell
   cd "C:\Users\SAI TEJA\OneDrive\Desktop\Projects\AD\venture-ai"
   docker compose up -d
   docker compose ps
   ```

2. Wait for all containers to be `running` or `running (healthy)`.

3. Log in as each role in sequence and complete the required setup (profile, etc.) **before** capturing.

4. Set browser zoom to 100%. Use a clean incognito window so no extension bars appear.

---

## Required Screenshots

| # | Filename | Route | Login as | Notes |
|---|---|---|---|---|
| 1 | `01-landing.png` | `/` | — | Landing page, hero + 3 cards visible |
| 2 | `02-login.png` | `/login` | — | Clean form, no autofill artifacts |
| 3 | `03-startup-dashboard.png` | `/startup/dashboard` | STARTUP | Show metric cards + profile card |
| 4 | `04-startup-profile.png` | `/startup/profile` | STARTUP | Profile view with badges + metrics |
| 5 | `05-startup-analytics.png` | `/startup/analytics` | STARTUP | Metric grid + at least 2 charts visible |
| 6 | `06-ai-potential.png` | `/startup/ai-potential` | STARTUP | **After clicking Analyze Startup.** Show the score card + risk badge |
| 7 | `07-pitch-deck.png` | `/startup/pitch-deck` | STARTUP | **After clicking Analyze Pitch Deck.** Show overall completeness + at least 3 section bars + info gaps |
| 8 | `08-investor-dashboard.png` | `/investor/dashboard` | INVESTOR | Profile card + metric cards |
| 9 | `09-investor-profile.png` | `/investor/profile` | INVESTOR | Preference chips visible |
| 10 | `10-recommended.png` | `/investor/recommended` | INVESTOR | **After clicking Find Matches.** 5 recommendation cards visible |
| 11 | `11-ai-insights.png` | `/investor/insights` | INVESTOR | **After clicking Load Insights.** Metric cards + chart visible |
| 12 | `12-admin-dashboard.png` | `/admin/dashboard` | ADMIN | Simple dashboard view |
| 13 | `13-ai-analytics.png` | `/admin/ai-analytics` | ADMIN | Amber synthetic-data banner + at least one model card |
| 14 | `14-platform-analytics.png` | `/admin/platform-analytics` | ADMIN | 4 metric cards + at least 2 charts |
| 15 | `15-docker-ps.png` | — | — | Screenshot of the `docker compose ps` output in a terminal |

---

## Sample Pitch Deck Text (for screenshot #7)

Use exactly this text — it matches the analyzer's training vocabulary and produces non-zero output:

```
Small and medium businesses struggle to manage their finances efficiently. Existing tools are expensive, fragmented, and hard to integrate. Our platform provides a single, unified workspace for the entire workflow. We use machine learning to automate the most time-consuming tasks. The global market for this category is estimated at over 40 billion dollars. There are more than 30 million potential customers in our target segment. The core product is a web-based platform with mobile companions. Key features include automated reporting, alerts, and workflow automation. We operate a subscription SaaS model with tiered pricing. Revenue comes from monthly and annual recurring subscriptions. We have over 1,200 paying customers and monthly revenue of 85,000 dollars. Revenue has grown 15 percent month over month for the last four quarters. Our main competitors are large incumbents and a handful of startups. We differentiate through automation depth and integration breadth. Our founding team combines deep domain expertise and engineering leadership. The CEO previously scaled a similar company to 50 million in revenue.
```

---

## Capture Tips

- **Windows:** `Win + Shift + S` opens the Snipping Tool. Capture the region, save directly to `docs/screenshots/`.
- **DevTools:** press F12 and consider closing it for clean screenshots — unless the screenshot is deliberately showing network/console.
- **Wait for loading states to finish** before capturing. The AI calls can take a few seconds on cold start.
- **Don't include** personal email addresses, real names, or any actual credentials in the screenshot. If they appear, redact with a paint tool or create fresh demo accounts.
- **Grid layouts:** capture enough vertical space to show the whole card, not half-cut.

---

## Folder Structure

```
docs/
├── screenshots/
│   ├── 01-landing.png
│   ├── 02-login.png
│   ├── 03-startup-dashboard.png
│   ├── 04-startup-profile.png
│   ├── 05-startup-analytics.png
│   ├── 06-ai-potential.png
│   ├── 07-pitch-deck.png
│   ├── 08-investor-dashboard.png
│   ├── 09-investor-profile.png
│   ├── 10-recommended.png
│   ├── 11-ai-insights.png
│   ├── 12-admin-dashboard.png
│   ├── 13-ai-analytics.png
│   ├── 14-platform-analytics.png
│   └── 15-docker-ps.png
└── (this file + other docs)
```

---

## What to Do If a Screenshot Fails

| Symptom | Fix |
|---|---|
| Page shows red error card | AI service or backend down — `docker compose ps`, restart the failing container |
| Empty state instead of data | Profile not created — create one first |
| No recommendation cards | Investor profile missing or incomplete — create profile first |
| Blank white page | Check browser console — likely a stale bundle, hard refresh with `Ctrl+Shift+R` |
| Charts don't render | Wait a beat — recharts needs layout to compute |