"""
Phase 8.5 — Generate a synthetic pitch-deck section dataset for BERT
fine-tuning.

Each record is a short paragraph belonging to one of 8 business sections.
The text is synthetic and generated from templates + filler phrases. It is
NOT real startup or investment data.

Output:
    data/raw/pitch_deck_sections.csv

Usage:
    python training/generate_pitch_deck_dataset.py
"""

from __future__ import annotations

import random
from pathlib import Path

import pandas as pd

RANDOM_SEED: int = 42
SAMPLES_PER_SECTION: int = 60   # 8 sections x 60 = 480 records

PROJECT_ROOT = Path(__file__).resolve().parents[1]
RAW_DIR = PROJECT_ROOT / "data" / "raw"
OUTPUT_CSV = RAW_DIR / "pitch_deck_sections.csv"

SECTIONS = [
    "Problem",
    "Solution",
    "Market",
    "Product",
    "Business Model",
    "Traction",
    "Competition",
    "Team",
]

# Each section has a set of template sentences. The generator picks a subset
# of templates per record and joins them, which yields realistic multi-sentence
# paragraphs while keeping the label unambiguous.
TEMPLATES = {
    "Problem": [
        "Small and medium businesses struggle to manage their finances efficiently.",
        "Existing tools are expensive, fragmented, and hard to integrate.",
        "Manual processes lead to costly errors and lost revenue every quarter.",
        "Teams waste hours on repetitive tasks that should be automated.",
        "Current solutions fail to address the specific needs of growing companies.",
        "Customers report frustration with the complexity of enterprise software.",
        "The market lacks an affordable, unified platform for this workflow.",
        "Companies lose customers because of slow and inconsistent service.",
        "Fragmented data silos make decision-making slow and unreliable.",
        "Regulatory pressure is increasing, and existing tools cannot keep up.",
    ],
    "Solution": [
        "Our platform provides a single, unified workspace for the entire workflow.",
        "We use machine learning to automate the most time-consuming tasks.",
        "The product integrates with existing tools through open APIs.",
        "A self-serve onboarding flow lets customers get value in minutes.",
        "Our unique algorithm reduces processing time by an order of magnitude.",
        "We combine automation with human expertise to deliver reliable results.",
        "The solution is designed to scale from a single team to the enterprise.",
        "Customers see immediate cost savings and reduced manual effort.",
        "Our modular architecture lets clients adopt only what they need.",
        "Real-time dashboards surface the insights that matter most.",
    ],
    "Market": [
        "The global market for this category is estimated at over 40 billion dollars.",
        "There are more than 30 million potential customers in our target segment.",
        "The addressable market is growing at approximately 18 percent annually.",
        "Our initial focus is North America and Europe, with expansion planned for APAC.",
        "Small and medium businesses represent a 12 billion dollar subsegment.",
        "The category has seen increasing venture investment over the past five years.",
        "Regulatory tailwinds are expected to accelerate adoption across industries.",
        "Our beachhead is mid-market companies with 50 to 500 employees.",
        "The total addressable market is large and highly fragmented.",
        "Analysts project continued double-digit growth through the next decade.",
    ],
    "Product": [
        "The core product is a web-based platform with mobile companions.",
        "Key features include automated reporting, alerts, and workflow automation.",
        "Our architecture is built on a modern cloud-native stack.",
        "The product supports role-based access for teams of any size.",
        "Integration with leading third-party systems is out of the box.",
        "A robust API lets customers extend the platform for custom needs.",
        "Data is encrypted in transit and at rest, with SOC 2 compliance on the roadmap.",
        "The user interface is designed for non-technical operators.",
        "An admin console allows fine-grained permission management.",
        "The system scales horizontally to support tens of thousands of users.",
    ],
    "Business Model": [
        "We operate a subscription SaaS model with tiered pricing.",
        "Revenue comes from monthly and annual recurring subscriptions.",
        "Enterprise plans include premium support and custom integrations.",
        "The average contract value is growing as customers expand usage.",
        "We also generate revenue from usage-based API calls above plan limits.",
        "Gross margins exceed 75 percent at scale.",
        "Customer acquisition cost is offset within the first twelve months.",
        "Our pricing aligns with the value customers receive.",
        "A marketplace for third-party extensions adds an additional revenue stream.",
        "Multi-year contracts provide predictable recurring revenue.",
    ],
    "Traction": [
        "We have over 1,200 paying customers and monthly revenue of 85,000 dollars.",
        "Revenue has grown 15 percent month over month for the last four quarters.",
        "Customer retention exceeds 92 percent on an annual basis.",
        "We have signed three enterprise pilot agreements this quarter.",
        "Our user base grew from 5,000 to 40,000 in twelve months.",
        "Net promoter score is 62, well above the industry average.",
        "We have processed over 2 million transactions on the platform.",
        "The waitlist for our enterprise tier exceeds 200 companies.",
        "Annual recurring revenue has tripled year over year.",
        "Churn has declined steadily for the last six quarters.",
    ],
    "Competition": [
        "Our main competitors are large incumbents and a handful of startups.",
        "We differentiate through automation depth and integration breadth.",
        "Incumbent solutions are expensive and slow to deploy.",
        "Our unit economics are superior to the closest comparable players.",
        "No competitor currently offers the same end-to-end workflow.",
        "We have a defensible data moat from thousands of customer workflows.",
        "Partnerships give us distribution advantages over point solutions.",
        "Our pricing is 30 percent below the nearest enterprise competitor.",
        "The competitive landscape is fragmented, creating an opportunity to consolidate.",
        "We win head-to-head evaluations in over 70 percent of deals.",
    ],
    "Team": [
        "Our founding team combines deep domain expertise and engineering leadership.",
        "The CEO previously scaled a similar company to 50 million in revenue.",
        "The CTO has 15 years of experience building cloud platforms.",
        "Advisors include former executives from leading companies in the space.",
        "The team has shipped products used by millions of people.",
        "We have assembled a strong group of operators and engineers.",
        "Founders have worked together for over a decade.",
        "Key hires bring experience from top-tier technology companies.",
        "The team has raised and exited a previous venture.",
        "Our leadership has published research in the field.",
    ],
}


def _build_record(rng: random.Random, section: str) -> str:
    templates = TEMPLATES[section]
    n = rng.randint(2, 4)
    chosen = rng.sample(templates, n)
    return " ".join(chosen)


def main() -> None:
    rng = random.Random(RANDOM_SEED)
    RAW_DIR.mkdir(parents=True, exist_ok=True)

    rows = []
    sample_counter = 0
    for section in SECTIONS:
        for _ in range(SAMPLES_PER_SECTION):
            text = _build_record(rng, section)
            rows.append({
                "sample_id": f"pd_{sample_counter:04d}",
                "section": section,
                "text": text,
            })
            sample_counter += 1

    df = pd.DataFrame(rows)
    df.to_csv(OUTPUT_CSV, index=False)

    print("=" * 60)
    print("Synthetic Pitch-Deck Section Dataset")
    print("=" * 60)
    print(f"Random seed       : {RANDOM_SEED}")
    print(f"Sections          : {len(SECTIONS)}")
    print(f"Samples per sect. : {SAMPLES_PER_SECTION}")
    print(f"Total records     : {len(df)}")
    print(f"Output            : {OUTPUT_CSV}")
    print()
    print("Section distribution:")
    print(df["section"].value_counts().to_string())
    print()
    print("NOTE: Synthetic development-only pitch-deck data. Not real startup")
    print("      or investment data.")


if __name__ == "__main__":
    main()