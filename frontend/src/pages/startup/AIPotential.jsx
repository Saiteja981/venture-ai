import { useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import ScoreCard from "../../components/ai/ScoreCard.jsx";
import RiskBadge from "../../components/ai/RiskBadge.jsx";
import AIPageHeader from "../../components/ai/AIPageHeader.jsx";
import FeedbackControl from "../../components/ai/FeedbackControl.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useStartupProfile } from "../../hooks/useStartupProfile.js";
import { messageForError } from "../../utils/errorHandler.js";
import { deriveStartupFeatures } from "../../utils/aiDerivedFeatures.js";
import { predictStartup } from "../../services/api/aiApi.js";
import { reportAiEvent } from "../../services/notification/notificationService.js";
import { Brain, Sparkles, Rocket, Info } from "lucide-react";

const DIRECTION_STYLE = {
    increases: { bar: "bg-emerald-500", arrow: "↑", label: "raises" },
    decreases: { bar: "bg-amber-500", arrow: "↓", label: "lowers" },
    neutral: { bar: "bg-slate-300", arrow: "—", label: "no change" },
};

function ExplanationRow({ contribution, maxAbs }) {
    const style = DIRECTION_STYLE[contribution.direction] ?? DIRECTION_STYLE.neutral;
    const pct = maxAbs > 0 ? Math.min(100, (Math.abs(contribution.delta) / maxAbs) * 100) : 0;
    return (
        <li>
            <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-navy-800">{contribution.feature}</span>
                <span className="text-slate-500">
                    {style.arrow} {Math.abs(contribution.delta).toFixed(2)} pts
                </span>
            </div>
            <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                    className={`h-full rounded-full ${style.bar}`}
                    style={{ width: `${pct}%` }}
                />
            </div>
            <div className="mt-0.5 text-[10px] text-slate-400">
                value {String(contribution.original_value)} · baseline{" "}
                {String(contribution.baseline_value)}
            </div>
        </li>
    );
}

export default function AIPotential() {
    const toast = useToast();
    const { loading, error, startup, reload } = useStartupProfile();

    const [predicting, setPredicting] = useState(false);
    const [prediction, setPrediction] = useState(null);
    const [predictError, setPredictError] = useState("");

    async function handleAnalyze() {
        if (!startup) return;
        setPredicting(true);
        setPredictError("");
        try {
            const payload = deriveStartupFeatures(startup);
            const result = await predictStartup(payload, true);
            setPrediction(result);
            reportAiEvent("AI_POTENTIAL_COMPLETED");
        } catch (err) {
            const msg =
                err?.isNetworkError || err?.isTimeout
                    ? "AI service is currently unavailable. Please try again later."
                    : messageForError(err) || "Prediction failed.";
            setPredictError(msg);
            toast.error(msg);
        } finally {
            setPredicting(false);
        }
    }

    const maxAbs = prediction?.explanation?.length
        ? Math.max(...prediction.explanation.map((c) => Math.abs(c.delta) || 0))
        : 0;

    return (
        <DashboardLayout title="AI Potential & Risk">
            <div className="space-y-6">
                <AIPageHeader
                    icon={Brain}
                    title="AI Potential & Risk"
                    description="Analyze your startup using Venture AI's deep-learning prediction model. Scores are estimates based on your startup profile."
                />

                {loading ? (
                    <LoadingState message="Loading your startup profile…" />
                ) : error ? (
                    <AIErrorState message={error} onRetry={reload} />
                ) : !startup ? (
                    <EmptyState
                        icon={Rocket}
                        title="No startup profile yet"
                        description="Create your startup profile before running AI analysis."
                        action={
                            <Link to="/startup/profile" className="btn-primary">
                                Create Startup Profile
                            </Link>
                        }
                    />
                ) : (
                    <>
                        <Card>
                            <div className="flex items-center justify-between">
                                <div>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        {startup.companyName}
                                    </h3>
                                    <p className="mt-1 text-xs text-slate-500">
                                        The following profile will be sent to the model.
                                    </p>
                                </div>
                                <Button onClick={handleAnalyze} disabled={predicting}>
                                    {predicting ? "Analyzing…" : "Analyze Startup"}
                                </Button>
                            </div>

                            <dl className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
                                <div>
                                    <dt className="text-xs text-slate-500">Industry</dt>
                                    <dd className="mt-1 text-sm text-navy-800">{startup.industry}</dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Stage</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.startupStage}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Team Size</dt>
                                    <dd className="mt-1 text-sm text-navy-800">{startup.teamSize}</dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Customers</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.customerCount}
                                    </dd>
                                </div>
                            </dl>
                        </Card>

                        {predicting && <AILoadingState message="Analyzing startup…" />}

                        {!predicting && predictError && (
                            <AIErrorState message={predictError} onRetry={handleAnalyze} />
                        )}

                        {!predicting && prediction && (
                            <>
                                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                                    <ScoreCard
                                        score={prediction.potential_score}
                                        label="AI Potential Score"
                                    />
                                    <Card>
                                        <div className="flex items-center justify-between">
                                            <h3 className="text-base font-semibold text-navy-800">
                                                Risk Category
                                            </h3>
                                            <RiskBadge category={prediction.risk_category} />
                                        </div>
                                        <div className="mt-4 flex items-center gap-3">
                                            <Sparkles className="h-5 w-5 text-brand-600" />
                                            <div>
                                                <p className="text-sm text-navy-800">
                                                    Model version: {prediction.model_version}
                                                </p>
                                                <p className="mt-1 text-xs text-slate-500">
                                                    Categorical risk prediction from the same model.
                                                </p>
                                            </div>
                                        </div>
                                    </Card>
                                </div>

                                {/* Phase 24: feedback for this specific execution */}
                                <FeedbackControl executionId={prediction._execution?.id} />

                                {/* Phase 22: approximate explanation panel */}
                                {prediction.explanation && prediction.explanation.length > 0 ? (
                                    <Card>
                                        <div className="flex flex-wrap items-start justify-between gap-2">
                                            <div>
                                                <h3 className="text-base font-semibold text-navy-800">
                                                    Why this prediction?
                                                </h3>
                                                <p className="mt-1 text-xs text-slate-500">
                                                    Approximate influence of each input on the
                                                    score for this specific prediction.
                                                </p>
                                            </div>
                                            <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                                                <Info className="h-3 w-3" /> approximate
                                            </span>
                                        </div>

                                        <ul className="mt-5 space-y-4">
                                            {prediction.explanation.map((c) => (
                                                <ExplanationRow
                                                    key={c.feature}
                                                    contribution={c}
                                                    maxAbs={maxAbs}
                                                />
                                            ))}
                                        </ul>

                                        {prediction.explanation_note && (
                                            <p className="mt-5 border-t border-slate-100 pt-3 text-xs text-slate-500">
                                                {prediction.explanation_note}
                                            </p>
                                        )}
                                    </Card>
                                ) : prediction.explanation_note ? (
                                    <Card>
                                        <div className="flex items-start gap-2">
                                            <Info className="mt-0.5 h-4 w-4 flex-shrink-0 text-slate-400" />
                                            <p className="text-xs text-slate-500">
                                                {prediction.explanation_note}
                                            </p>
                                        </div>
                                    </Card>
                                ) : null}

                                <Card>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Disclaimer
                                    </h3>
                                    <p className="mt-2 text-sm text-slate-600">
                                        AI-generated insights are analytical estimates based on the
                                        provided startup information and should not be considered
                                        financial or investment advice.
                                    </p>
                                </Card>
                            </>
                        )}
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}