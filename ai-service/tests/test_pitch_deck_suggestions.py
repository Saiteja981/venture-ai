"""Unit tests for strength detection and suggestion prioritization."""
from app.services.pitch_analyzer_service import (
    ACTIONABLE_SUGGESTIONS,
    REQUIRED_SECTIONS,
    STRENGTH_THRESHOLD,
    _build_section_suggestions,
    _build_strengths,
)


def _empty_sections():
    return {
        s: {"present": False, "confidence": 0.0, "text": "", "completeness": 0.0}
        for s in REQUIRED_SECTIONS
    }


def test_strengths_empty_when_all_missing():
    assert _build_strengths(_empty_sections()) == []


def test_strengths_uses_completeness_threshold():
    sections = _empty_sections()
    sections["Problem"] = {
        "present": True, "confidence": 0.9, "text": "x",
        "completeness": STRENGTH_THRESHOLD,
    }
    sections["Team"] = {
        "present": True, "confidence": 0.9, "text": "x",
        "completeness": STRENGTH_THRESHOLD - 1,
    }
    assert _build_strengths(sections) == ["Problem"]


def test_suggestions_cover_all_missing_as_high():
    suggestions = _build_section_suggestions(_empty_sections())
    assert len(suggestions) == len(REQUIRED_SECTIONS)
    assert all(s["priority"] == "HIGH" for s in suggestions)


def test_suggestions_medium_defensive_branch():
    """MEDIUM is a defensive branch for inconsistent state — verify it works."""
    sections = _empty_sections()
    sections["Problem"] = {
        "present": True, "confidence": 0.3, "text": "x", "completeness": 20.0,
    }
    suggestions = _build_section_suggestions(sections)
    problem = next(s for s in suggestions if s["section"] == "Problem")
    assert problem["priority"] == "MEDIUM"


def test_suggestions_low_when_present_but_thin():
    sections = _empty_sections()
    sections["Problem"] = {
        "present": True, "confidence": 0.8, "text": "x",
        "completeness": STRENGTH_THRESHOLD - 10,
    }
    suggestions = _build_section_suggestions(sections)
    problem = next(s for s in suggestions if s["section"] == "Problem")
    assert problem["priority"] == "LOW"


def test_suggestions_omitted_when_strong():
    sections = _empty_sections()
    sections["Problem"] = {
        "present": True, "confidence": 0.9, "text": "x",
        "completeness": STRENGTH_THRESHOLD + 10,
    }
    suggestions = _build_section_suggestions(sections)
    assert all(s["section"] != "Problem" for s in suggestions)


def test_suggestions_priority_ordering():
    """HIGH must appear before MEDIUM before LOW."""
    sections = _empty_sections()
    sections["Problem"] = {
        "present": True, "confidence": 0.8, "text": "x", "completeness": 20.0,
    }  # LOW
    sections["Solution"] = {
        "present": True, "confidence": 0.3, "text": "x", "completeness": 20.0,
    }  # MEDIUM
    # Team and Market remain absent -> HIGH
    suggestions = _build_section_suggestions(sections)
    rank = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}
    priorities = [rank[s["priority"]] for s in suggestions]
    assert priorities == sorted(priorities)


def test_suggestions_use_actionable_text():
    suggestions = _build_section_suggestions(_empty_sections())
    for s in suggestions:
        assert s["suggestion"] == ACTIONABLE_SUGGESTIONS[s["section"]]
        assert s["suggestion"]