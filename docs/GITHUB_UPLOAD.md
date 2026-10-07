# GitHub Upload Guide

**Read this entire document before running any `git` command.**

The project contains one large binary that requires special handling:
- `ai-service/saved_models/pitch_analyzer/model/tf_model.h5` — **438 MB**

GitHub's hard limits:

| Limit | Value |
|---|---|
| Per-file (regular push) | 100 MB — will be rejected |
| Repository size (soft) | 1 GB recommended |
| Git LFS free storage | 1 GB (5 GB bandwidth/month) |

The BERT model alone is 438 MB, so **it cannot be committed without Git LFS**.

---

## Pre-Flight — Choose Your Model Strategy

You have three options. Pick one before starting.

### Option A — Git LFS (recommended if you want the model in the repo)

**Pros:** Anyone who clones gets a working AI service immediately.
**Cons:** Uses ~438 MB of your 1 GB LFS quota.

### Option B — Exclude the model, document regeneration

**Pros:** Keeps the repo small, no LFS quota consumed.
**Cons:** Anyone who clones must run `train_pitch_analyzer.py` (~10 minutes) to regenerate the model. Requires `data/raw/` which is also gitignored.

### Option C — External hosting

**Pros:** Clean repo, no quota used.
**Cons:** Requires an extra download step and a stable hosting location (HuggingFace Hub, S3, etc.).

**The commands below default to Option A (Git LFS).** If you prefer B or C, skip the LFS section and add one line to `.gitignore`:

```
ai-service/saved_models/pitch_analyzer/model/tf_model.h5
```

---

## Step 0 — Install Git LFS (if choosing Option A)

```powershell
git lfs install
```

Expected output: `Git LFS initialized.`

Verify:

```powershell
git lfs version
```

If `git` itself is not installed, install from https://git-scm.com/ first.

---

## Step 1 — Pre-Flight Inspection (no changes yet)

From the project root:

```powershell
cd "C:\Users\SAI TEJA\OneDrive\Desktop\Projects\AD\venture-ai"
```

### 1a — Find any file larger than 10 MB

```powershell
Get-ChildItem -Recurse -File |
  Where-Object { $_.FullName -notmatch "node_modules|\.venv|\.git\\" -and $_.Length -gt 10MB } |
  Sort-Object Length -Descending |
  Select-Object @{N='SizeMB';E={[math]::Round($_.Length/1MB,1)}}, FullName |
  Select-Object -First 20
```

Expected: only `ai-service\saved_models\pitch_analyzer\model\tf_model.h5` should appear at ~438 MB.

If any other file > 100 MB appears, add its path to `.gitignore` before continuing.

### 1b — Confirm secrets are excluded

```powershell
Write-Host "Files containing the JWT secret:"
Get-ChildItem -Recurse -File |
  Where-Object { $_.FullName -notmatch "node_modules|\.venv|\.git\\|target\\|\.pytest_cache" } |
  Select-String -Pattern "9ixW6i1q7IB8U0" -List

Write-Host ""
Write-Host "Files containing the DB password:"
Get-ChildItem -Recurse -File |
  Where-Object { $_.FullName -notmatch "node_modules|\.venv|\.git\\|target\\|\.pytest_cache" } |
  Select-String -Pattern "srinivas|change_me_local_password" -List
```

Expected matches (all gitignored, none will be committed):

- `.env`
- `backend\src\main\resources\application.properties`
- `.env.example` (placeholder only)
- `backend\src\main\resources\application.properties.example` (placeholder only)

If any **other** file appears, investigate and gitignore it before continuing.

---

## Step 2 — Initialize the Repository (local only, no remote yet)

```powershell
cd "C:\Users\SAI TEJA\OneDrive\Desktop\Projects\AD\venture-ai"
git init
git branch -M main
```

Expected: `Initialized empty Git repository in ...`.

---

## Step 3 — Configure Git LFS for the BERT Model

**Skip if you chose Option B or C.**

```powershell
git lfs track "ai-service/saved_models/pitch_analyzer/model/tf_model.h5"
```

This creates a `.gitattributes` file. Confirm:

```powershell
Get-Content .gitattributes
```

Expected:

```
ai-service/saved_models/pitch_analyzer/model/tf_model.h5 filter=lfs diff=lfs merge=lfs -text
```

Commit `.gitattributes` **first**, before any other file:

```powershell
git add .gitattributes
git commit -m "chore: configure Git LFS for BERT model"
```

---

## Step 4 — Dry-Run: See What Will Be Committed

```powershell
git add -n .
```

This shows every file that **would** be staged — without staging anything. **Read the entire output.** Look for:

- ❌ Any `.env` file
- ❌ `backend/src/main/resources/application.properties`
- ❌ Any `node_modules/` path
- ❌ Any `.venv/` path
- ❌ Any `target/` path
- ❌ Any `__pycache__/` or `.pytest_cache/` path
- ❌ Any file > 100 MB that is NOT tracked by LFS

**Do not proceed if any of those appear.** Instead:

1. Add the offending pattern to `.gitignore`
2. Re-run `git add -n .`
3. Repeat until clean

### Count the files

```powershell
git add -n . | Measure-Object -Line
```

Expected: a few hundred to ~1000 lines (source + docs + small model files). If it's tens of thousands, `.gitignore` is not working.

### Check the total staged size

```powershell
git add -n . |
  ForEach-Object { $_ -replace '^add ''(.+)''$', '$1' } |
  Where-Object { Test-Path $_ } |
  ForEach-Object { (Get-Item $_).Length } |
  Measure-Object -Sum |
  Select-Object @{N='TotalMB';E={[math]::Round($_.Sum/1MB,1)}}
```

Expected with LFS: ~10–50 MB (the BERT file is not counted here because LFS replaces it with a pointer).
Expected without LFS: would be ~450 MB, and the push would fail.

---

## Step 5 — Stage and Commit

**Only after Step 4's dry run is clean.**

```powershell
git add .
git status
```

Review `git status` one more time. Then:

```powershell
git commit -m "Initial commit — Venture AI platform"
```

If Git LFS is in use, this step may take a minute as it hashes the 438 MB model.

Verify the commit size:

```powershell
git count-objects -vH
```

The `size-pack` line shows the compressed repo size. Expect ~500 MB with LFS, ~10 MB without.

---

## Step 6 — Create the GitHub Repository

**Do this on github.com, not on the command line.**

1. Go to https://github.com/new
2. **Repository name:** `venture-ai` (or `Venture-AI-Platform`)
3. **Description:** `AI-Powered Venture Capital and Startup Collaboration Platform Using Deep Learning and Data Analytics`
4. **Visibility:** Private (recommended until the academic submission is finalized) or Public
5. **Do NOT initialize** with README, .gitignore, or license
    - We already have those locally
    - Initializing them creates a merge conflict on the first push
6. Click **Create repository**

Copy the HTTPS URL shown on the next page. It will look like:

```
https://github.com/<your-username>/venture-ai.git
```

---

## Step 7 — Add Remote and Push

```powershell
git remote add origin https://github.com/<your-username>/venture-ai.git
git remote -v
```

Expected: two lines showing `origin` for fetch and push.

Push:

```powershell
git push -u origin main
```

**If Git LFS is in use**, the push will first upload the model via the LFS API, then push the commit. This may take several minutes on a slow connection.

Expected success:

```
Uploading LFS objects: 100% (1/1), 438 MB | ...
Writing objects: 100% (N/N), done.
branch 'main' set up to track 'origin/main'.
```

---

## Step 8 — Verify on GitHub

Open the repository in a browser:

1. Confirm the file tree looks correct — no `.env`, no `node_modules`, no `target`
2. Check the BERT model file:
    - Click `ai-service/saved_models/pitch_analyzer/model/tf_model.h5`
    - It should show `Stored with Git LFS` and a file size of ~438 MB
3. Click on `README.md` — should render the full README
4. Check the Insights tab → Community Standards should show a green check for README

---

## Troubleshooting

### "This exceeds GitHub's file size limit of 100.00 MB"

The BERT file was not tracked by LFS. Fix:

```powershell
git lfs track "ai-service/saved_models/pitch_analyzer/model/tf_model.h5"
git add .gitattributes
git add ai-service/saved_models/pitch_analyzer/model/tf_model.h5
git commit --amend --no-edit
git push -u origin main --force
```

Only use `--force` if this is your own repository and you are the only contributor.

### "remote: error: GH001: Large files detected"

Same fix as above. The commit already contains the large file without LFS; you need to rewrite history:

```powershell
git rm --cached ai-service/saved_models/pitch_analyzer/model/tf_model.h5
git commit --amend --no-edit
git push -u origin main --force
```

### "Authentication failed"

GitHub no longer accepts passwords over HTTPS. Use a **Personal Access Token** instead:

1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate a token with `repo` scope
3. When `git push` prompts for a password, paste the token

Or install the GitHub CLI (`gh auth login`) and let it manage credentials.

### Push is slow or times out

The LFS upload is bandwidth-bound. On a slow connection:

- Wait — Git LFS retries automatically
- Or push without the model (Option B) and document the regeneration step

### "Updates were rejected because the remote contains work that you do not have"

You created the GitHub repo with a README or .gitignore. Fix:

```powershell
git pull origin main --allow-unrelated-histories
# resolve any conflicts
git push -u origin main
```

Or delete the GitHub repo and recreate it empty.

---

## After the Push

### Recommended repository settings

1. **About** section: add a short description and topics
    - Topics: `deep-learning`, `recommender-system`, `bert`, `spring-boot`, `fastapi`, `react`, `mysql`, `docker`
2. **Branches:** consider protecting `main` if you add collaborators
3. **Actions:** not needed yet; you can add a CI workflow later
4. **Releases:** optionally tag the submission commit as `v1.0-academic-submission`

### Update the local README with the GitHub URL

Add a badge at the top:

```markdown
[![Repository](https://img.shields.io/badge/github-venture--ai-blue)](https://github.com/<your-username>/venture-ai)
```

### Back up the local repo

After a successful push, ZIP the project folder (excluding `.git`, `node_modules`, `.venv`, `target`) as an extra backup for the academic submission.

---

## What Not to Do

- ❌ Do not run `git add .` before Step 4's dry-run passes
- ❌ Do not `git push --force` unless you are the only contributor
- ❌ Do not commit `.env`, `application.properties`, or any file containing real credentials
- ❌ Do not initialize the GitHub repo with a README — it will conflict
- ❌ Do not add the BERT model without Git LFS — the push will fail

---

## Quick Command Summary (Once Everything Above Is Understood)

```powershell
# Option A — with Git LFS for the BERT model
cd "C:\Users\SAI TEJA\OneDrive\Desktop\Projects\AD\venture-ai"
git lfs install
git init
git branch -M main
git lfs track "ai-service/saved_models/pitch_analyzer/model/tf_model.h5"
git add .gitattributes
git commit -m "chore: configure Git LFS"
git add -n .                          # DRY RUN — verify carefully
git add .
git commit -m "Initial commit — Venture AI platform"
git remote add origin https://github.com/<you>/venture-ai.git
git push -u origin main
```

**Wait for `git add -n .` to look correct before running `git add .`.**