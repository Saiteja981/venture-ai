"""
Phase 8.4 — Generate a synthetic startup-investor interaction dataset.

Combines the existing synthetic startup data (data/raw/raw_synthetic_startups.csv)
with the synthetic investor data (data/raw/raw_synthetic_investors.csv) to
produce labelled startup-investor pairs for training the two-tower model.

Label generation
----------------
The compatibility_label is generated deterministically from OVERLAP between
startup and investor attributes, plus a deterministic noise component:

    signal =
        0.30 * industry_match          (1 if investor lists startup's industry)
      + 0.20 * stage_match             (1 if investor lists startup's stage)
      + 0.15 * business_model_match    (1 if investor lists startup's BM)
      + 0.15 * investment_range_match  (1 if startup's funding fits range)
      + 0.10 * risk_match              (heuristic based on startup stage + runway)
      + 0.10 * location_match          (1 if same country / region group)

    label = 1 if (0.9 * signal + 0.1 * noise) >= 0.5 else 0

The noise is deterministic (seeded). This is deliberately NOT a copy of any
single input feature — it requires the model to learn a genuine joint
representation across both towers.

SYNTHETIC DATA WARNING: The labels do not represent real investor decisions.

Output:
    data/raw/synthetic_interactions.csv

Usage:
    python training/generate_recommendation_data.py
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd

RANDOM_SEED: int = 42
PAIRS_PER_INVESTOR: int = 20   # 300 investors × 20 = 6000 candidate pairs

PROJECT_ROOT = Path(__file__).resolve().parents[1]
RAW_DIR = PROJECT_ROOT / "data" / "raw"
STARTUPS_CSV = RAW_DIR / "raw_synthetic_startups.csv"
INVESTORS_CSV = RAW_DIR / "raw_synthetic_investors.csv"
OUTPUT_CSV = RAW_DIR / "synthetic_interactions.csv"

# Rough grouping of locations into regions for the location_match signal.
# This is only used for synthetic label generation.
REGION_MAP = {
    "San Francisco": "US", "New York": "US", "Toronto": "US",
    "London": "EU", "Berlin": "EU", "Paris": "EU",
    "Bangalore": "APAC", "Singapore": "APAC", "Sydney": "APAC",
    "Tel Aviv": "ME",
}


def _industry_match(startup_industry: str, investor_industries: str) -> int:
    return int(startup_industry in investor_industries.split(","))


def _stage_match(startup_stage: str, investor_stages: str) -> int:
    return int(startup_stage in investor_stages.split(","))


def _business_model_match(startup_bm: str, investor_bms: str) -> int:
    if not investor_bms or not isinstance(investor_bms, str):
        return 0
    return int(startup_bm in investor_bms.split(","))


def _investment_range_match(total_funding: float,
                            min_inv: float, max_inv: float) -> int:
    # Rough heuristic: assume the startup's next round is on the order of
    # 20% of its total funding so far (or the funding itself if very early).
    # This is deliberately fuzzy to avoid trivially leaking the label.
    target = max(total_funding * 0.2, 50_000.0)
    return int(min_inv <= target <= max_inv)


def _risk_match(startup_stage: str, runway_months: float,
                risk_preference: str) -> int:
    """
    High-risk investors prefer early-stage / short-runway startups.
    Low-risk investors prefer later-stage / long-runway startups.
    """
    if risk_preference == "HIGH":
        return int(startup_stage in {"PRE_SEED", "SEED"} or runway_months < 12)
    if risk_preference == "LOW":
        return int(startup_stage in {"SERIES_A", "SERIES_B"} and runway_months >= 12)
    # MEDIUM: either signal is acceptable
    return int(True)


def _location_match(startup_industry: str, investor_location: str) -> int:
    # Very rough synthetic signal: certain industries cluster in certain regions.
    region = REGION_MAP.get(investor_location, "OTHER")
    if region == "US" and startup_industry in {"SaaS", "Artificial Intelligence", "FinTech"}:
        return 1
    if region == "EU" and startup_industry in {"CleanTech", "HealthTech", "FinTech"}:
        return 1
    if region == "APAC" and startup_industry in {"E-Commerce", "EdTech", "Logistics"}:
        return 1
    return 0


def main() -> None:
    if not STARTUPS_CSV.exists():
        raise FileNotFoundError(
            f"Startups dataset not found: {STARTUPS_CSV}. "
            "Run training/generate_dataset.py first."
        )
    if not INVESTORS_CSV.exists():
        raise FileNotFoundError(
            f"Investors dataset not found: {INVESTORS_CSV}. "
            "Run training/generate_investor_dataset.py first."
        )

    startups = pd.read_csv(STARTUPS_CSV)
    investors = pd.read_csv(INVESTORS_CSV)

    # Deduplicate startups on startup_id (raw file contains 3 intentional dupes)
    startups = startups.drop_duplicates(subset=["startup_id"], keep="first").reset_index(drop=True)

    rng = np.random.default_rng(RANDOM_SEED)

    rows = []
    startup_idx = startups.index.to_numpy()
    for _, inv in investors.iterrows():
        # Sample PAIRS_PER_INVESTOR startups without replacement
        chosen = rng.choice(startup_idx, size=PAIRS_PER_INVESTOR, replace=False)
        for si in chosen:
            st = startups.loc[si]

            im = _industry_match(st["industry"], inv["preferred_industries"])
            sm = _stage_match(st["startup_stage"], inv["preferred_stages"])
            bm = _business_model_match(st["business_model"], inv["preferred_business_models"])
            rm = _investment_range_match(
                float(st["total_funding"]),
                float(inv["minimum_investment"]),
                float(inv["maximum_investment"]),
            )
            rkm = _risk_match(st["startup_stage"],
                              float(st["runway_months"]) if pd.notna(st["runway_months"]) else 12.0,
                              inv["risk_preference"])
            lm = _location_match(st["industry"], inv["preferred_location"])

            signal = (
                0.30 * im
                + 0.20 * sm
                + 0.15 * bm
                + 0.15 * rm
                + 0.10 * rkm
                + 0.10 * lm
            )
            noise = float(rng.uniform(0.0, 1.0))
            score = 0.9 * signal + 0.1 * noise
            label = int(score >= 0.5)

            rows.append({
                "startup_id": st["startup_id"],
                "investor_id": inv["investor_id"],
                "industry_match": im,
                "stage_match": sm,
                "business_model_match": bm,
                "investment_range_match": rm,
                "risk_match": rkm,
                "location_match": lm,
                "compatibility_signal": round(float(signal), 4),
                "compatibility_label": label,
            })

    df = pd.DataFrame(rows)
    df.to_csv(OUTPUT_CSV, index=False)

    pos = int(df["compatibility_label"].sum())
    total = len(df)
    print("=" * 60)
    print("Synthetic Interaction Dataset Generator")
    print("=" * 60)
    print(f"Random seed      : {RANDOM_SEED}")
    print(f"Pairs per investor: {PAIRS_PER_INVESTOR}")
    print(f"Total pairs      : {total}")
    print(f"Positive labels  : {pos} ({pos / total * 100:.1f}%)")
    print(f"Negative labels  : {total - pos}")
    print(f"Output           : {OUTPUT_CSV}")
    print("=" * 60)
    print()
    print("NOTE: These labels are synthetic and do not represent real investor")
    print("      decisions. They exist only to demonstrate recommendation learning.")


if __name__ == "__main__":
    main()