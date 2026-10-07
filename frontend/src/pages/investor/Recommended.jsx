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
import { useToast } from "../../hooks/useToast.js";
import { getMyInvestorProfile } from "../../services/investor/investorService.js";
import { getRecommendations } from "../../services/api/aiApi.js";
import { messageForError } from "../../utils/errorHandler.js";
import { Sparkles, UserCircle2 } from "lucide-react";

const NOT_FOUND = 404;
const TOP_K = 5;

export default function Recommended() {
    const toast = useToast();
    const [loadingProfile, setLoadingProfile] = useState(true);
    const [profile, setProfile] = useState(null);
    const [profileError, setProfileError] = useState("");

    const [loadingRecs, setLoadingRecs] = useState(false);
    const [recs, setRecs] = useState(null);
    const [recError, setRecError] = useState("");

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
            const result = await getRecommendations(payload, TOP_K);
            setRecs(result);
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

                                        <div className="space-y-3">
                                            {recs.recommendations.map((rec) => (
                                                <RecommendationCard
                                                    key={rec.startup_id}
                                                    recommendation={rec}
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
                                        {recs.disclaimer ||
                                            "Recommendations are produced by a model trained on synthetic development data and do not represent real investment guidance."}
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