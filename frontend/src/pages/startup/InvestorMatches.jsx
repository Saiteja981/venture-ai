import { useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import AIPageHeader from "../../components/ai/AIPageHeader.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import FeedbackControl from "../../components/ai/FeedbackControl.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useStartupProfile } from "../../hooks/useStartupProfile.js";
import { messageForError } from "../../utils/errorHandler.js";
import { deriveStartupFeatures } from "../../utils/aiDerivedFeatures.js";
import { getInvestorMatches } from "../../services/api/aiApi.js";
import { reportAiEvent } from "../../services/notification/notificationService.js";
import { Users, UserCircle2, CheckCircle2, XCircle, Trophy } from "lucide-react";

const TOP_K = 5;

const CRITERION_LABELS = {
    industry: "Industry",
    startup_stage: "Stage",
    business_model: "Business model",
};

function MatchPanel({ matches }) {
    if (!matches || matches.length === 0) return null;
    return (
        <div className="mt-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
            <h4 className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                Preference alignment (rule-based)
            </h4>
            <ul className="mt-2 space-y-1.5">
                {matches.map((m) => (
                    <li
                        key={m.criterion}
                        className="flex items-start justify-between gap-3 text-xs"
                    >
                        <div className="min-w-0">
                            <span className="font-medium text-navy-800">
                                {CRITERION_LABELS[m.criterion] ?? m.criterion}
                            </span>
                            <span className="ml-2 text-slate-500">
                                you: {m.startup_value ?? "—"}
                            </span>
                            <span className="ml-2 text-[11px] text-slate-400">
                                wants: {(m.investor_preference || []).join(", ") || "—"}
                            </span>
                        </div>
                        {m.matched ? (
                            <span className="inline-flex flex-shrink-0 items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-medium text-emerald-700">
                                <CheckCircle2 className="h-3 w-3" /> aligned
                            </span>
                        ) : (
                            <span className="inline-flex flex-shrink-0 items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">
                                <XCircle className="h-3 w-3" /> not aligned
                            </span>
                        )}
                    </li>
                ))}
            </ul>
            <p className="mt-2 border-t border-slate-100 pt-2 text-[10px] text-slate-400">
                Rule-based comparison with the investor's stated preferences — independent
                of the learned compatibility score above.
            </p>
        </div>
    );
}

function InvestorMatchCard({ investor }) {
    return (
        <Card className="!p-4">
            <div className="flex items-center justify-between gap-3">
                <div className="flex min-w-0 items-center gap-3">
                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-brand-50">
                        <Trophy className="h-5 w-5 text-brand-600" />
                    </div>
                    <div className="min-w-0">
                        <div className="flex items-center gap-2">
                            <span className="truncate text-sm font-semibold text-navy-800">
                                {investor.investor_id}
                            </span>
                            <span className="inline-flex rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600">
                                Rank #{investor.rank}
                            </span>
                        </div>
                    </div>
                </div>
                <div className="flex-shrink-0 text-right">
                    <div className="text-lg font-semibold text-navy-800">
                        {Number(investor.compatibility_score).toFixed(1)}
                    </div>
                    <div className="text-[10px] uppercase tracking-wide text-slate-400">
                        compatibility
                    </div>
                </div>
            </div>
            <MatchPanel matches={investor.direct_matches} />
        </Card>
    );
}

export default function InvestorMatches() {
    const toast = useToast();
    const { loading, error, startup, reload } = useStartupProfile();

    const [loadingMatches, setLoadingMatches] = useState(false);
    const [matches, setMatches] = useState(null);
    const [matchError, setMatchError] = useState("");

    async function handleFind() {
        if (!startup) return;
        setLoadingMatches(true);
        setMatchError("");
        setMatches(null);
        try {
            const payload = deriveStartupFeatures(startup);
            const result = await getInvestorMatches(payload, TOP_K, true);
            setMatches(result);
            reportAiEvent("RECOMMENDATIONS_LOADED");
        } catch (err) {
            const msg =
                err?.isNetworkError || err?.isTimeout
                    ? "AI service is currently unavailable. Please try again later."
                    : messageForError(err) || "Investor matching failed.";
            setMatchError(msg);
            toast.error(msg);
        } finally {
            setLoadingMatches(false);
        }
    }

    return (
        <DashboardLayout title="Investor Matches">
            <div className="space-y-6">
                <AIPageHeader
                    icon={Users}
                    title="Investor Matches"
                    description="Use the two-tower recommendation model to find investors whose stated preferences align with your startup profile."
                />

                {loading ? (
                    <LoadingState message="Loading your startup profile…" />
                ) : error ? (
                    <AIErrorState message={error} onRetry={reload} />
                ) : !startup ? (
                    <EmptyState
                        icon={UserCircle2}
                        title="No startup profile yet"
                        description="Create your startup profile before requesting investor matches."
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
                                        Your profile will be sent to the recommendation model.
                                    </p>
                                </div>
                                <Button onClick={handleFind} disabled={loadingMatches}>
                                    {loadingMatches ? "Searching…" : "Find Investors"}
                                </Button>
                            </div>
                            <dl className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
                                <div>
                                    <dt className="text-xs text-slate-500">Industry</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.industry}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Stage</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.startupStage}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Business Model</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.businessModel}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Total Funding</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        ${Number(startup.totalFunding ?? 0).toLocaleString()}
                                    </dd>
                                </div>
                            </dl>
                        </Card>

                        {loadingMatches && (
                            <AILoadingState message="Finding investor matches…" />
                        )}

                        {!loadingMatches && matchError && (
                            <AIErrorState message={matchError} onRetry={handleFind} />
                        )}

                        {!loadingMatches && matches && (
                            <>
                                {matches.recommendations.length === 0 ? (
                                    <EmptyState
                                        icon={Users}
                                        title="No investor matches found yet"
                                        description="Your profile may need more information. Try updating your industry, stage, or business model."
                                        action={
                                            <Link to="/startup/profile" className="btn-primary">
                                                Update Profile
                                            </Link>
                                        }
                                    />
                                ) : (
                                    <>
                                        <Card>
                                            <h3 className="text-base font-semibold text-navy-800">
                                                Suggested Investors
                                            </h3>
                                            <p className="mt-1 text-xs text-slate-500">
                                                {matches.count} result
                                                {matches.count === 1 ? "" : "s"} from{" "}
                                                {matches.model_version}
                                            </p>
                                        </Card>

                                        {/* Phase 24: feedback for this matching run */}
                                        <FeedbackControl executionId={matches._execution?.id} />

                                        <div className="space-y-3">
                                            {matches.recommendations.map((inv) => (
                                                <InvestorMatchCard
                                                    key={inv.investor_id}
                                                    investor={inv}
                                                />
                                            ))}
                                        </div>
                                    </>
                                )}

                                <Card>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Disclaimer
                                    </h3>
                                    <p className="mt-2 text-sm text-slate-600">
                                        {matches.disclaimer ||
                                            "Investor matches are produced by a model trained on synthetic development data. They reflect model-computed similarity, not real investor interest."}
                                    </p>
                                    {matches.explanation_note && (
                                        <p className="mt-3 border-t border-slate-100 pt-3 text-xs text-slate-500">
                                            {matches.explanation_note}
                                        </p>
                                    )}
                                </Card>
                            </>
                        )}
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}