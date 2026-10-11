"""
Phase 8.5 — Reusable pitch-deck analysis service.

Loads the trained BERT section classifier (HuggingFace
TFBertForSequenceClassification) and exposes:

    analyze_pitch_deck(text: str) -> dict
"""

from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any

import numpy as np
import tensorflow as tf
from transformers import BertTokenizer, TFBertForSequenceClassification

PROJECT_ROOT = Path(__file__).resolve().parents[2]
SAVED_DIR = PROJECT_ROOT / "saved_models" / "pitch_analyzer"
MODEL_DIR = SAVED_DIR / "model"
TOKENIZER_DIR = SAVED_DIR / "tokenizer"
LABEL_MAPPING_PATH = SAVED_DIR / "label_mapping.json"

MAX_SEQ_LEN = 128
MAX_CHUNK_CHARS = 500
CONFIDENCE_THRESHOLD = 0.60

REQUIRED_SECTIONS = [
    "Problem", "Solution", "Market", "Product",
    "Business Model", "Traction", "Competition", "Team",
]

SEVERITY_MISSING = "HIGH"
SEVERITY_WEAK = "MEDIUM"
SEVERITY_THIN = "LOW"

GAP_MESSAGES = {
    "Problem": "Problem statement information is missing or insufficient.",
    "Solution": "Solution description is missing or insufficient.",
    "Market": "Market size or target-market information is missing or insufficient.",
    "Product": "Product description is missing or insufficient.",
    "Business Model": "Business model or revenue-model information is missing or insufficient.",
    "Traction": "Traction metrics such as customers, revenue, growth, or adoption are missing or insufficient.",
    "Competition": "Competitive landscape or differentiation information is missing or insufficient.",
    "Team": "Founder/team background and relevant expertise are missing or insufficient.",
}

# ---------- Phase 22: strengths and suggestions ----------

STRENGTH_THRESHOLD = 70.0

ACTIONABLE_SUGGESTIONS = {
    "Problem": "Describe the specific problem your startup solves and who experiences it. Avoid generic market pain.",
    "Solution": "Explain how your product solves the problem, ideally with a concrete example or workflow.",
    "Market": "Quantify the market — TAM/SAM/SOM or equivalent — and cite the source or assumption.",
    "Product": "Describe what the product does, its key features, and how it is used in practice.",
    "Business Model": "State how you make money (pricing, unit economics, revenue streams).",
    "Traction": "Add concrete metrics — ARR/MRR, growth rate, customer count, retention, key logos.",
    "Competition": "Name your competitors and explain your differentiation or defensibility.",
    "Team": "List founders, relevant experience, and any prior exits or domain expertise.",
}

PRIORITY_MAP = {
    "HIGH": "HIGH",
    "MEDIUM": "MEDIUM",
    "LOW": "LOW",
}


# ---------- module-level cached state ----------

_model = None
_tokenizer = None
_label_to_idx: dict[str, int] | None = None
_idx_to_label: dict[int, str] | None = None


def _load() -> None:
    global _model, _tokenizer, _label_to_idx, _idx_to_label
    if _model is not None:
        return
    if not MODEL_DIR.exists():
        raise FileNotFoundError(
            f"Pitch analyzer model not found at {MODEL_DIR}. "
            "Run training/train_pitch_analyzer.py first."
        )
    if not TOKENIZER_DIR.exists():
        raise FileNotFoundError(
            f"Tokenizer not found at {TOKENIZER_DIR}. "
            "Run training/train_pitch_analyzer.py first."
        )
    _model = TFBertForSequenceClassification.from_pretrained(str(MODEL_DIR))
    _tokenizer = BertTokenizer.from_pretrained(str(TOKENIZER_DIR))
    with open(LABEL_MAPPING_PATH, "r", encoding="utf-8") as f:
        mapping = json.load(f)
    _label_to_idx = mapping["label_to_idx"]
    _idx_to_label = {int(k): v for k, v in mapping["idx_to_label"].items()}


def _split_into_chunks(text: str, max_chars: int = MAX_CHUNK_CHARS) -> list[str]:
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return []
    paragraphs = [p.strip() for p in re.split(r"(?<=[.!?])\s{2,}", text) if p.strip()]
    chunks: list[str] = []
    for para in paragraphs:
        if len(para) <= max_chars:
            chunks.append(para)
            continue
        sentences = re.split(r"(?<=[.!?])\s+", para)
        buf = ""
        for s in sentences:
            if not s:
                continue
            if len(buf) + len(s) + 1 <= max_chars:
                buf = f"{buf} {s}".strip()
            else:
                if buf:
                    chunks.append(buf)
                buf = s
        if buf:
            chunks.append(buf)
    if not chunks:
        chunks = [text[i:i + max_chars] for i in range(0, len(text), max_chars)]
    return chunks


def _classify_chunks(chunks: list[str]) -> list[dict[str, Any]]:
    _load()
    enc = _tokenizer(
        chunks,
        padding="max_length",
        truncation=True,
        max_length=MAX_SEQ_LEN,
        return_tensors="np",
    )
    outputs = _model.predict(
        {
            "input_ids": enc["input_ids"].astype(np.int32),
            "attention_mask": enc["attention_mask"].astype(np.int32),
            "token_type_ids": enc["token_type_ids"].astype(np.int32),
        },
        verbose=0,
    )
    logits = outputs.logits if hasattr(outputs, "logits") else outputs[0]
    probs = tf.nn.softmax(logits, axis=-1).numpy()
    results = []
    for i, chunk in enumerate(chunks):
        p = probs[i]
        idx = int(np.argmax(p))
        label = _idx_to_label[idx]
        conf = float(p[idx])
        results.append({"text": chunk, "section": label, "confidence": conf})
    return results


def _aggregate_sections(classifications: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    sections = {s: {"present": False, "confidence": 0.0, "text": ""}
                for s in REQUIRED_SECTIONS}
    best_by_section: dict[str, dict[str, Any]] = {}
    for c in classifications:
        s = c["section"]
        if s not in sections:
            continue
        prev = best_by_section.get(s)
        if prev is None or c["confidence"] > prev["confidence"]:
            best_by_section[s] = c
    for s, info in best_by_section.items():
        sections[s]["confidence"] = float(info["confidence"])
        sections[s]["text"] = info["text"]
        sections[s]["present"] = bool(info["confidence"] >= CONFIDENCE_THRESHOLD)
    return sections


def _text_coverage_ratio(text: str, total_chars: int) -> float:
    if total_chars <= 0:
        return 0.0
    return min(1.0, len(text) / max(total_chars, 1))


def _completeness_for_section(info: dict[str, Any], total_chars: int) -> float:
    if not info["present"]:
        return 0.0
    cov = _text_coverage_ratio(info["text"], total_chars)
    score = 100.0 * (0.7 * info["confidence"] + 0.3 * cov)
    return float(max(0.0, min(100.0, score)))


def _build_information_gaps(sections: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    gaps = []
    for s in REQUIRED_SECTIONS:
        info = sections[s]
        if not info["present"]:
            gaps.append({"section": s, "severity": SEVERITY_MISSING,
                         "message": GAP_MESSAGES[s]})
        elif info["confidence"] < CONFIDENCE_THRESHOLD:
            gaps.append({"section": s, "severity": SEVERITY_WEAK,
                         "message": GAP_MESSAGES[s]})
    return gaps


# ---------- Phase 22: strength and suggestion builders ----------

def _build_strengths(sections: dict[str, dict[str, Any]]) -> list[str]:
    return [
        s for s in REQUIRED_SECTIONS
        if sections[s]["present"]
        and sections[s].get("completeness", 0.0) >= STRENGTH_THRESHOLD
    ]


def _build_section_suggestions(sections: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    suggestions: list[dict[str, Any]] = []
    for s in REQUIRED_SECTIONS:
        info = sections[s]
        if not info["present"]:
            priority = PRIORITY_MAP["HIGH"]
        elif info["confidence"] < CONFIDENCE_THRESHOLD:
            priority = PRIORITY_MAP["MEDIUM"]
        elif info.get("completeness", 0.0) < STRENGTH_THRESHOLD:
            priority = PRIORITY_MAP["LOW"]
        else:
            continue
        suggestions.append({
            "section": s,
            "priority": priority,
            "suggestion": ACTIONABLE_SUGGESTIONS[s],
        })
    order = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}
    suggestions.sort(key=lambda x: (order[x["priority"]], x["section"]))
    return suggestions


# ---------- public entry point ----------

def analyze_pitch_deck(text: str) -> dict[str, Any]:
    if text is None or not isinstance(text, str) or not text.strip():
        return {
            "overall_completeness_score": 0.0,
            "sections": {s: {"present": False, "confidence": 0.0, "text": "",
                             "completeness": 0.0} for s in REQUIRED_SECTIONS},
            "information_gaps": [
                {"section": s, "severity": SEVERITY_MISSING, "message": GAP_MESSAGES[s]}
                for s in REQUIRED_SECTIONS
            ],
            "summary": {"detected_count": 0,
                        "missing_count": len(REQUIRED_SECTIONS),
                        "total_sections": len(REQUIRED_SECTIONS)},
            "strengths": [],
            "section_suggestions": [
                {"section": s, "priority": "HIGH",
                 "suggestion": ACTIONABLE_SUGGESTIONS[s]}
                for s in REQUIRED_SECTIONS
            ],
        }

    chunks = _split_into_chunks(text)
    if not chunks:
        return analyze_pitch_deck("")

    classifications = _classify_chunks(chunks)
    sections = _aggregate_sections(classifications)

    total_chars = sum(len(s["text"]) for s in sections.values()) or 1

    for s in REQUIRED_SECTIONS:
        sections[s]["completeness"] = _completeness_for_section(sections[s], total_chars)

    overall = float(np.mean([sections[s]["completeness"] for s in REQUIRED_SECTIONS]))
    overall = float(max(0.0, min(100.0, overall)))

    gaps = _build_information_gaps(sections)
    detected = sum(1 for s in REQUIRED_SECTIONS if sections[s]["present"])
    missing = len(REQUIRED_SECTIONS) - detected

    strengths = _build_strengths(sections)
    section_suggestions = _build_section_suggestions(sections)

    return {
        "overall_completeness_score": round(overall, 2),
        "sections": {
            s: {
                "present": sections[s]["present"],
                "confidence": round(sections[s]["confidence"], 4),
                "text": sections[s]["text"],
                "completeness": round(sections[s]["completeness"], 2),
            } for s in REQUIRED_SECTIONS
        },
        "information_gaps": gaps,
        "summary": {"detected_count": detected, "missing_count": missing,
                    "total_sections": len(REQUIRED_SECTIONS)},
        "strengths": strengths,
        "section_suggestions": section_suggestions,
    }