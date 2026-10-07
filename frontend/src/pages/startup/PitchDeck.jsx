import { useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import AIPageHeader from "../../components/ai/AIPageHeader.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import CompletenessBar from "../../components/ai/CompletenessBar.jsx";
import InsightList from "../../components/ai/InsightList.jsx";
import { useToast } from "../../hooks/useToast.js";
import { analyzePitchDeck } from "../../services/api/aiApi.js";
import { messageForError } from "../../utils/errorHandler.js";
import { FileText } from "lucide-react";

const MAX_CHARS = 100000;

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