import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import AIPageHeader from "../../components/ai/AIPageHeader.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import RecommendationCard from "../../components/ai/RecommendationCard.jsx";
import FeedbackControl from "../../components/ai/FeedbackControl.jsx";
import { useToast } from "../../hooks/useToast.js";
import { getMyInvestorProfile } from "../../services/investor/investorService.js";
import { getRecommendations } from "../../services/api/aiApi.js";
import { getPreferences } from "../../services/settings/settingsService.js";
import { reportAiEvent } from "../../services/notification/notificationService.js";
import { messageForError } from "../../utils/errorHandler.js";
import { Sparkles, UserCircle2, CheckCircle2, XCircle } from "lucide-react";

const NOT_FOUND = 404;
const DEFAULT_TOP_K = 5;

const CRITERION_LABELS = {
    industry: "Industry",
    startup_stage: "Stage",
    business_model: "Business model",
};

function DirectMatchPanel({ matches }) {
    if (!matches || matches.length === 0) return null;
    return (
        <Card className="!p-4">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Preference matches (rule-based)
            </h4>
            <ul className="mt-3 space-y-2">
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
                                {m.startup_value ?? "—"}
                            </span>
                            {m.note && (
                                <span className="ml-2 text-[10px] italic text-slate-400">
                                    {m.note}
                                </span>
                            )}
                        </div>
                        {m.matched ? (
                            <span className="inline-flex flex-shrink-0 items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-medium text-emerald-700">
                                <CheckCircle2 className="h-3 w-3" /> matched
                            </span>
                        ) : (
                            <span className="inline-flex flex-shrink-0 items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">
                                <XCircle className="h-3 w-3" /> no match
                            </span>
                        )}
                    </li>
                ))}
            </ul>
            <p className="mt-3 border-t border-slate-100 pt-2 text-[10px] text-slate-400">
                Rule-based comparison with your stated preferences — independent of the
                learned compatibility score above.
            </p>
        </Card>
    );
}

export default function Recommended() {
    const toast = useToast();
    const [loadingProfile, setLoadingProfile] = useState(true);
    const [profile, setProfile] = useState(null);
    const [profileError, setProfileError] = useState("");

    const [loadingRecs, setLoadingRecs] = useState(false);
    const [recs, setRecs] = useState(null);
    const [recError, setRecError] = useState("");
    const [topK, setTopK] = useState(DEFAULT_TOP_K);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const data = await getMyInvestorProfile();
                if (!cancelled) setProfile(data);
            } catch (err) {
                if (cancelled) return;
                if (err?.status === NOT_FOUND) setProfile(null);
                else setProfileError(messageForError(err));
            } finally {
                if (!cancelled) setLoadingProfile(false);
            }
        })();
        return () => {
            cancelled = true;
        };
    }, []);

    // Phase 26: read the investor's recommendation-display preference.
    // Falls back to DEFAULT_TOP_K when unset or if the call fails.
    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const prefs = await getPreferences();
                if (cancelled) return;
                if (prefs?.recommendationsPerPage) {
                    setTopK(prefs.recommendationsPerPage);
                }
            } catch {
                // Non-critical: fall back to the default of 5.
            }
        })();
        return () => { cancelled = true; };
    }, []);

    async function handleFind() {
        if (!profile) return;
        setLoadingRecs(true);
        setRecError("");
        setRecs(null);
        try {
            const payload = {
                investor_type: profile.investorType,
                preferred_industries: profile.preferredIndustries,
                preferred_stages: profile.preferredStages,
                preferred_business_models: profile.preferredBusinessModels ?? null,
                minimum_investment: Number(profile.minimumInvestment),
                maximum_investment: Number(profile.maximumInvestment),
                risk_preference: profile.riskPreference,
                investment_experience_years: Number(profile.investmentExperienceYears),
                portfolio_size:
                    profile.portfolioSize !== null && profile.portfolioSize !== undefined
                        ? Number(profile.portfolioSize)
                        : null,
                preferred_location: profile.preferredLocation ?? null,
            };
            const result = await getRecommendations(payload, topK, true);
            setRecs(result);
            reportAiEvent("RECOMMENDATIONS_LOADED");
        } catch (err) {
            const msg =
                err?.isNetworkError || err?.isTimeout
                    ? "AI service is currently unavailable. Please try again later."
                    : messageForError(err) || "Recommendation failed.";
            setRecError(msg);
            toast.error(msg);
        } finally {
            setLoadingRecs(false);
        }
    }

    return (
        <DashboardLayout title="Recommended Startups">
            <div className="space-y-6">
                <AIPageHeader
                    icon={Sparkles}
                    title="Recommended Startups"
                    description="Use the two-tower recommendation model to find startups matched to your investor profile."
                />

                {loadingProfile ? (
                    <LoadingState message="Loading your investor profile…" />
                ) : profileError ? (
                    <AIErrorState message={profileError} />
                ) : !profile ? (
                    <EmptyState
                        icon={UserCircle2}
                        title="No investor profile yet"
                        description="Create your investor profile before requesting AI-powered startup recommendations."
                        action={
                            <Link to="/investor/profile" className="btn-primary">
                                Create Investor Profile
                            </Link>
                        }
                    />
                ) : (
                    <>
                        <Card>
                            <div className="flex items-center justify-between">
                                <div>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        {profile.investorName}
                                    </h3>
                                    <p className="mt-1 text-xs text-slate-500">
                                        Your profile will be sent to the recommendation model.
                                    </p>
                                </div>
                                <Button onClick={handleFind} disabled={loadingRecs}>
                                    {loadingRecs ? "Searching…" : "Find Matches"}
                                </Button>
                            </div>
                            <dl className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
                                <div>
                                    <dt className="text-xs text-slate-500">Type</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {profile.investorType}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Risk</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {profile.riskPreference}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Min Investment</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        ${Number(profile.minimumInvestment).toLocaleString()}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Max Investment</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        ${Number(profile.maximumInvestment).toLocaleString()}
                                    </dd>
                                </div>
                            </dl>
                        </Card>

                        {loadingRecs && (
                            <AILoadingState message="Finding investor matches…" />
                        )}

                        {!loadingRecs && recError && (
                            <AIErrorState message={recError} onRetry={handleFind} />
                        )}

                        {!loadingRecs && recs && (
                            <>
                                {recs.recommendations.length === 0 ? (
                                    <EmptyState
                                        icon={Sparkles}
                                        title="No matching startups found yet"
                                        description="Your profile may need more information. Try updating your preferred industries, stages, or investment range."
                                        action={
                                            <Link to="/investor/profile" className="btn-primary">
                                                Update Profile
                                            </Link>
                                        }
                                    />
                                ) : (
                                    <>
                                        <Card>
                                            <h3 className="text-base font-semibold text-navy-800">
                                                Recommended Startups
                                            </h3>
                                            <p className="mt-1 text-xs text-slate-500">
                                                {recs.count} result{recs.count === 1 ? "" : "s"} from{" "}
                                                {recs.model_version}
                                            </p>
                                        </Card>

                                        <FeedbackControl executionId={recs._execution?.id} />

                                        <div className="space-y-4">
                                            {recs.recommendations.map((rec) => (
                                                <div key={rec.startup_id} className="space-y-2">
                                                    <RecommendationCard
                                                        recommendation={rec}
                                                    />
                                                    <DirectMatchPanel
                                                        matches={rec.direct_matches}
                                                    />
                                                </div>
                                            ))}
                                        </div>
                                    </>
                                )}

                                <Card>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Disclaimer
                                    </h3>
                                    <p className="mt-2 text-sm text-slate-600">
                                        {recs.disclaimer ||
                                            "Recommendations are produced by a model trained on synthetic development data and do not represent real investment guidance."}
                                    </p>
                                    {recs.explanation_note && (
                                        <p className="mt-3 border-t border-slate-100 pt-3 text-xs text-slate-500">
                                            {recs.explanation_note}
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