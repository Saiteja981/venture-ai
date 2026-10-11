import { useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import AIPageHeader from "../../components/ai/AIPageHeader.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import CompletenessBar from "../../components/ai/CompletenessBar.jsx";
import InsightList from "../../components/ai/InsightList.jsx";
import FeedbackControl from "../../components/ai/FeedbackControl.jsx";
import { useToast } from "../../hooks/useToast.js";
import { analyzePitchDeck } from "../../services/api/aiApi.js";
import { reportAiEvent } from "../../services/notification/notificationService.js";
import { messageForError } from "../../utils/errorHandler.js";
import { FileText, CheckCircle2, Lightbulb } from "lucide-react";

const MAX_CHARS = 100000;

const PRIORITY_STYLE = {
    HIGH: "bg-red-50 text-red-700 border-red-100",
    MEDIUM: "bg-amber-50 text-amber-700 border-amber-100",
    LOW: "bg-blue-50 text-blue-700 border-blue-100",
};

function PriorityBadge({ priority }) {
    const cls = PRIORITY_STYLE[priority] ?? "bg-slate-100 text-slate-500 border-slate-100";
    return (
        <span
            className={`inline-flex flex-shrink-0 items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${cls}`}
        >
            {priority}
        </span>
    );
}

export default function PitchDeck() {
    const toast = useToast();
    const [text, setText] = useState("");
    const [analyzing, setAnalyzing] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState("");

    async function handleAnalyze(e) {
        e.preventDefault();
        if (!text.trim()) {
            toast.error("Paste your pitch deck content first.");
            return;
        }
        setAnalyzing(true);
        setError("");
        try {
            const res = await analyzePitchDeck(text);
            setResult(res);
            reportAiEvent("PITCH_DECK_ANALYZED");
        } catch (err) {
            const msg =
                err?.isNetworkError || err?.isTimeout
                    ? "AI service is currently unavailable. Please try again later."
                    : messageForError(err) || "Pitch deck analysis failed.";
            setError(msg);
            toast.error(msg);
        } finally {
            setAnalyzing(false);
        }
    }

    return (
        <DashboardLayout title="Pitch Deck">
            <div className="space-y-6">
                <AIPageHeader
                    icon={FileText}
                    title="Pitch Deck Analyzer"
                    description="Paste your pitch deck text and get section-level completeness analysis powered by a BERT classifier."
                />

                <Card>
                    <form onSubmit={handleAnalyze} className="space-y-4">
                        <div>
                            <label
                                htmlFor="pitch-text"
                                className="block text-sm font-medium text-slate-700"
                            >
                                Pitch Deck Content
                            </label>
                            <p className="mt-1 text-xs text-slate-500">
                                Plain text. Include your Problem, Solution, Market, Product,
                                Business Model, Traction, Competition, and Team sections.
                            </p>
                            <textarea
                                id="pitch-text"
                                rows={14}
                                value={text}
                                onChange={(e) => setText(e.target.value)}
                                maxLength={MAX_CHARS}
                                placeholder="Paste your pitch deck text here…"
                                className="input-field mt-2 font-mono text-sm"
                            />
                            <div className="mt-1 text-right text-xs text-slate-400">
                                {text.length}/{MAX_CHARS}
                            </div>
                        </div>

                        <div className="flex justify-end">
                            <Button type="submit" disabled={analyzing}>
                                {analyzing ? "Analyzing…" : "Analyze Pitch Deck"}
                            </Button>
                        </div>
                    </form>
                </Card>

                {analyzing && <AILoadingState message="Analyzing pitch deck…" />}

                {!analyzing && error && (
                    <AIErrorState message={error} onRetry={handleAnalyze} />
                )}

                {!analyzing && result && (
                    <>
                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">
                                Overall Completeness
                            </h3>
                            <p className="mt-3 text-3xl font-semibold text-navy-800">
                                {Number(result.overall_completeness_score).toFixed(1)}%
                            </p>
                            <p className="mt-1 text-xs text-slate-500">
                                Model: {result.summary?.model_version || "pitch_analyzer"} ·
                                Sections detected:{" "}
                                {result.summary?.detected_count ??
                                    Object.values(result.sections).filter((s) => s.present).length}
                                /{result.summary?.total_sections ?? Object.keys(result.sections).length}
                            </p>
                        </Card>

                        {/* Phase 24: feedback for this analysis run */}
                        <FeedbackControl executionId={result._execution?.id} />

                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">Sections</h3>
                            <div className="mt-4 space-y-4">
                                {Object.entries(result.sections).map(([name, info]) => (
                                    <CompletenessBar
                                        key={name}
                                        label={name}
                                        value={info.completeness ?? 0}
                                    />
                                ))}
                            </div>
                        </Card>

                        {/* Phase 22: strengths & actionable suggestions */}
                        {(result.strengths?.length > 0 ||
                            result.section_suggestions?.length > 0) && (
                            <Card>
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <h3 className="text-base font-semibold text-navy-800">
                                            Strengths & Suggestions
                                        </h3>
                                        <p className="mt-1 text-xs text-slate-500">
                                            Derived from the classifier&apos;s detected
                                            sections and completeness estimates.
                                        </p>
                                    </div>
                                    <Lightbulb className="h-5 w-5 flex-shrink-0 text-amber-500" />
                                </div>

                                {result.strengths?.length > 0 && (
                                    <div className="mt-5">
                                        <h4 className="text-xs font-semibold uppercase tracking-wide text-emerald-700">
                                            Detected strengths
                                        </h4>
                                        <div className="mt-2 flex flex-wrap gap-2">
                                            {result.strengths.map((s) => (
                                                <span
                                                    key={s}
                                                    className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700"
                                                >
                                                    <CheckCircle2 className="h-3 w-3" />
                                                    {s}
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {result.section_suggestions?.length > 0 && (
                                    <div className="mt-5">
                                        <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                                            Suggestions
                                        </h4>
                                        <ul className="mt-2 space-y-2">
                                            {result.section_suggestions.map((s) => (
                                                <li
                                                    key={s.section}
                                                    className="flex items-start gap-3 rounded-lg border border-slate-100 p-3"
                                                >
                                                    <PriorityBadge priority={s.priority} />
                                                    <div className="flex-1">
                                                        <div className="text-sm font-medium text-navy-800">
                                                            {s.section}
                                                        </div>
                                                        <div className="mt-0.5 text-xs text-slate-600">
                                                            {s.suggestion}
                                                        </div>
                                                    </div>
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                )}

                                {result.explanation_note && (
                                    <p className="mt-5 border-t border-slate-100 pt-3 text-xs text-slate-500">
                                        {result.explanation_note}
                                    </p>
                                )}
                            </Card>
                        )}

                        <InsightList
                            title="Information Gaps"
                            items={(result.information_gaps || []).map(
                                (g) => `${g.section} — ${g.message} (severity: ${g.severity})`
                            )}
                            emptyText="No information gaps detected. Your pitch deck covers all required sections."
                        />

                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">
                                Disclaimer
                            </h3>
                            <p className="mt-2 text-sm text-slate-600">
                                AI-generated insights are analytical estimates based on the
                                provided pitch deck text and should not be considered financial
                                or investment advice.
                            </p>
                        </Card>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}