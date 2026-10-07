import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import MetricCard from "../../components/common/MetricCard.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import ChartCard from "../../components/analytics/ChartCard.jsx";
import BarChartBlock from "../../components/analytics/BarChartBlock.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { getMyInvestorProfile } from "../../services/investor/investorService.js";
import { getRecommendations } from "../../services/api/aiApi.js";
import {
    UserCircle2,
    Shield,
    Briefcase,
    TrendingUp,
    Sparkles,
    Trophy,
    BarChart3,
} from "lucide-react";

const NOT_FOUND = 404;
const TOP_K = 5;

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: 0,
    }).format(n);
}

function ChipList({ csv, emptyText = "—" }) {
    if (!csv) return <span className="text-sm text-slate-400">{emptyText}</span>;
    return (
        <div className="flex flex-wrap gap-2">
            {csv
                .split(",")
                .map((x) => x.trim())
                .filter(Boolean)
                .map((x) => (
                    <Badge key={x} tone="navy">
                        {x}
                    </Badge>
                ))}
        </div>
    );
}

export default function InvestorInsights() {
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

    async function handleLoadInsights() {
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
                investment_experience_years: Number(
                    profile.investmentExperienceYears
                ),
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
                    : messageForError(err) || "Could not load insights.";
            setRecError(msg);
            toast.error(msg);
        } finally {
            setLoadingRecs(false);
        }
    }

    if (loadingProfile) {
        return (
            <DashboardLayout title="AI Insights">
                <LoadingState message="Loading investor profile…" />
            </DashboardLayout>
        );
    }

    if (profileError) {
        return (
            <DashboardLayout title="AI Insights">
                <AIErrorState message={profileError} />
            </DashboardLayout>
        );
    }

    if (!profile) {
        return (
            <DashboardLayout title="AI Insights">
                <EmptyState
                    icon={UserCircle2}
                    title="No investor profile yet"
                    description="Create your investor profile to unlock insights and recommendations."
                    action={
                        <Link to="/investor/profile" className="btn-primary">
                            Create Investor Profile
                        </Link>
                    }
                />
            </DashboardLayout>
        );
    }

    const scores = recs?.recommendations?.map((r) => ({
        label: r.startup_id,
        value: Number(r.compatibility_score) || 0,
    })) || [];

    const topScore =
        scores.length > 0 ? Math.max(...scores.map((s) => s.value)) : null;
    const avgScore =
        scores.length > 0
            ? scores.reduce((a, b) => a + b.value, 0) / scores.length
            : null;

    return (
        <DashboardLayout title="AI Insights">
            <div className="space-y-6">
                <PageHeader
                    title="AI Insights"
                    description="Your investor profile, recommendation analytics, and matched startups."
                />

                {/* Profile summary */}
                <Card>
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                            <h2 className="text-xl font-semibold text-navy-800">
                                {profile.investorName}
                            </h2>
                            {profile.organizationName && (
                                <p className="mt-1 text-sm text-slate-500">
                                    {profile.organizationName}
                                </p>
                            )}
                            <div className="mt-3 flex flex-wrap items-center gap-2">
                                <Badge tone="brand">{profile.investorType}</Badge>
                                <Badge tone="warning">Risk: {profile.riskPreference}</Badge>
                            </div>
                        </div>
                        <Link to="/investor/profile" className="btn-secondary">
                            Manage Profile
                        </Link>
                    </div>
                </Card>

                {/* Investment range */}
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <MetricCard
                        label="Min Investment"
                        value={fmtMoney(profile.minimumInvestment)}
                        icon={Briefcase}
                        tone="brand"
                    />
                    <MetricCard
                        label="Max Investment"
                        value={fmtMoney(profile.maximumInvestment)}
                        icon={Briefcase}
                        tone="success"
                    />
                    <MetricCard
                        label="Experience"
                        value={
                            profile.investmentExperienceYears != null
                                ? `${profile.investmentExperienceYears} yrs`
                                : "—"
                        }
                        icon={TrendingUp}
                        tone="accent"
                    />
                    <MetricCard
                        label="Risk Preference"
                        value={profile.riskPreference}
                        icon={Shield}
                        tone="warning"
                    />
                </div>

                {/* Preferences */}
                <Card>
                    <h3 className="text-base font-semibold text-navy-800">
                        Investment Preferences
                    </h3>
                    <div className="mt-4 space-y-4">
                        <div>
                            <div className="text-xs text-slate-500">Industries</div>
                            <div className="mt-1">
                                <ChipList csv={profile.preferredIndustries} />
                            </div>
                        </div>
                        <div>
                            <div className="text-xs text-slate-500">Stages</div>
                            <div className="mt-1">
                                <ChipList csv={profile.preferredStages} />
                            </div>
                        </div>
                        <div>
                            <div className="text-xs text-slate-500">Business Models</div>
                            <div className="mt-1">
                                <ChipList csv={profile.preferredBusinessModels} />
                            </div>
                        </div>
                    </div>
                </Card>

                {/* Recommendation insights */}
                <Card>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                                <Sparkles className="h-5 w-5" />
                            </div>
                            <div>
                                <h3 className="text-base font-semibold text-navy-800">
                                    Recommendation Insights
                                </h3>
                                <p className="mt-0.5 text-xs text-slate-500">
                                    Live model output — not persisted, computed per request.
                                </p>
                            </div>
                        </div>
                        <Button onClick={handleLoadInsights} disabled={loadingRecs}>
                            {loadingRecs ? "Loading…" : "Load Insights"}
                        </Button>
                    </div>

                    <div className="mt-6 space-y-6">
                        {loadingRecs && <AILoadingState message="Loading insights…" />}

                        {!loadingRecs && recError && (
                            <AIErrorState message={recError} onRetry={handleLoadInsights} />
                        )}

                        {!loadingRecs && recs && (
                            <>
                                <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                                    <MetricCard
                                        label="Recommendations"
                                        value={recs.count ?? recs.recommendations.length}
                                        icon={BarChart3}
                                        tone="brand"
                                    />
                                    <MetricCard
                                        label="Top Compatibility"
                                        value={
                                            topScore !== null ? topScore.toFixed(1) : "—"
                                        }
                                        icon={Trophy}
                                        tone="success"
                                    />
                                    <MetricCard
                                        label="Average Compatibility"
                                        value={
                                            avgScore !== null ? avgScore.toFixed(1) : "—"
                                        }
                                        icon={TrendingUp}
                                        tone="accent"
                                    />
                                </div>

                                <ChartCard
                                    title="Compatibility by Startup"
                                    subtitle={`Model: ${recs.model_version ?? "unknown"}`}
                                    height={280}
                                >
                                    <BarChartBlock data={scores} />
                                </ChartCard>

                                <p className="text-xs text-slate-500">
                                    {recs.disclaimer ||
                                        "Recommendations are produced by a model trained on synthetic development data and do not represent real investment guidance."}
                                </p>
                            </>
                        )}

                        {!loadingRecs && !recs && !recError && (
                            <p className="text-sm text-slate-500">
                                Click <span className="font-medium">Load Insights</span> to
                                compute recommendation analytics for your profile.
                            </p>
                        )}
                    </div>
                </Card>
            </div>
        </DashboardLayout>
    );
}