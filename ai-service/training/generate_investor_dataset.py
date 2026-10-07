"""
Phase 8.4 — Generate a synthetic investor dataset for the two-tower
recommendation model.

IMPORTANT
---------
This is SYNTHETIC development data. It does NOT represent real investors,
real investment behavior, or real VC activity.

The dataset is deterministic (fixed seed) and reproducible.

Output:
    data/raw/raw_synthetic_investors.csv

Usage:
    python training/generate_investor_dataset.py
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd

RANDOM_SEED: int = 42
N_INVESTORS: int = 300

PROJECT_ROOT = Path(__file__).resolve().parents[1]
RAW_DIR = PROJECT_ROOT / "data" / "raw"
OUTPUT_CSV = RAW_DIR / "raw_synthetic_investors.csv"

INVESTOR_TYPES = ["ANGEL", "VC_FIRM", "CORPORATE", "FAMILY_OFFICE"]
RISK_PREFERENCES = ["LOW", "MEDIUM", "HIGH"]

INDUSTRIES = [
    "Artificial Intelligence", "FinTech", "HealthTech", "EdTech",
    "E-Commerce", "SaaS", "Cybersecurity", "CleanTech", "Logistics", "Gaming",
]
STAGES = ["PRE_SEED", "SEED", "SERIES_A", "SERIES_B"]
BUSINESS_MODELS = [
    "B2B SaaS", "B2C SaaS", "Marketplace", "Subscription",
    "Freemium", "Enterprise", "Transactional",
]
LOCATIONS = [
    "San Francisco", "New York", "London", "Berlin", "Bangalore",
    "Singapore", "Toronto", "Tel Aviv", "Paris", "Sydney",
]


def _pick_multi(rng: np.random.Generator, options: list[str], min_k: int = 1, max_k: int = 3) -> str:
    k = int(rng.integers(min_k, max_k + 1))
    chosen = rng.choice(options, size=k, replace=False)
    return ",".join(sorted(chosen.tolist()))


def main() -> None:
    rng = np.random.default_rng(RANDOM_SEED)
    RAW_DIR.mkdir(parents=True, exist_ok=True)

    records = []
    for i in range(N_INVESTORS):
        investor_type = INVESTOR_TYPES[i % len(INVESTOR_TYPES)]
        risk = RISK_PREFERENCES[i % len(RISK_PREFERENCES)]

        min_inv = float(rng.uniform(10_000, 500_000))
        max_inv = min_inv * float(rng.uniform(1.5, 20.0))

        records.append({
            "investor_id": f"INV{1000 + i}",
            "investor_type": investor_type,
            "preferred_industries": _pick_multi(rng, INDUSTRIES, 1, 3),
            "preferred_stages": _pick_multi(rng, STAGES, 1, 3),
            "preferred_business_models": _pick_multi(rng, BUSINESS_MODELS, 1, 3),
            "minimum_investment": round(min_inv, 2),
            "maximum_investment": round(max_inv, 2),
            "risk_preference": risk,
            "investment_experience_years": int(rng.integers(1, 30)),
            "portfolio_size": int(rng.integers(1, 60)),
            "preferred_location": LOCATIONS[i % len(LOCATIONS)],
        })

    df = pd.DataFrame(records)
    df.to_csv(OUTPUT_CSV, index=False)

    print("=" * 60)
    print("Synthetic Investor Dataset Generator")
    print("=" * 60)
    print(f"Random seed     : {RANDOM_SEED}")
    print(f"Investors       : {len(df)}")
    print(f"Columns         : {len(df.columns)}")
    print(f"Output          : {OUTPUT_CSV}")
    print("=" * 60)


if __name__ == "__main__":
    main()