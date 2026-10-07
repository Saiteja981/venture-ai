import { useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import MetricCard from "../../components/common/MetricCard.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import ScoreCard from "../../components/ai/ScoreCard.jsx";
import RiskBadge from "../../components/ai/RiskBadge.jsx";
import ChartCard from "../../components/analytics/ChartCard.jsx";
import BarChartBlock from "../../components/analytics/BarChartBlock.jsx";
import LineChartBlock from "../../components/analytics/LineChartBlock.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useStartupProfile } from "../../hooks/useStartupProfile.js";
import { messageForError } from "../../utils/errorHandler.js";
import { deriveStartupFeatures } from "../../utils/aiDerivedFeatures.js";
import { predictStartup } from "../../services/api/aiApi.js";
import {
    Rocket,
    Users,
    UserCheck,
    DollarSign,
    TrendingUp,
    Landmark,
    Flame,
    Clock,
    Brain,
} from "lucide-react";

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

function fmtNumber(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US").format(n);
}

export default function StartupAnalytics() {
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
            const result = await predictStartup(payload);
            setPrediction(result);
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

    if (loading) {
        return (
            <DashboardLayout title="Startup Analytics">
                <LoadingState message="Loading analytics…" />
            </DashboardLayout>
        );
    }

    if (error) {
        return (
            <DashboardLayout title="Startup Analytics">
                <AIErrorState message={error} onRetry={reload} />
            </DashboardLayout>
        );
    }

    if (!startup) {
        return (
            <DashboardLayout title="Startup Analytics">
                <EmptyState
                    icon={Rocket}
                    title="No startup profile yet"
                    description="Create your startup profile to unlock analytics, charts, and AI insights."
                    action={
                        <Link to="/startup/profile" className="btn-primary">
                            Create Startup Profile
                        </Link>
                    }
                />
            </DashboardLayout>
        );
    }

    const fundingVsBurn = [
        { label: "Total Funding", value: Number(startup.totalFunding) || 0 },
        { label: "Monthly Burn", value: Number(startup.monthlyBurnRate) || 0 },
        { label: "Monthly Revenue", value: Number(startup.monthlyRevenue) || 0 },
    ];

    const usersVsCustomers = [
        { label: "Total Users", value: Number(startup.totalUsers) || 0 },
        { label: "Customers", value: Number(startup.customerCount) || 0 },
        { label: "Team Size", value: Number(startup.teamSize) || 0 },
    ];

    const growthSeries = [
        { label: "Current", value: Number(startup.revenueGrowthRate) || 0 },
    ];

    const runwaySeries = [
        { label: "Runway (mo)", value: Number(startup.runwayMonths) || 0 },
    ];

    return (
        <DashboardLayout title="Startup Analytics">
            <div className="space-y-6">
                <PageHeader
                    title="Startup Analytics"
                    description="Visualize your startup's performance, funding, and AI insights."
                />

                {/* Company overview */}
                <Card>
                    <div className="flex items-start justify-between">
                        <div>
                            <h2 className="text-xl font-semibold text-navy-800">
                                {startup.companyName}
                            </h2>
                            <p className="mt-1 text-sm text-slate-500">
                                {startup.industry} · {startup.businessModel} ·{" "}
                                {startup.startupStage}
                            </p>
                        </div>
                        <Link to="/startup/profile" className="btn-secondary">
                            Manage Profile
                        </Link>
                    </div>
                </Card>

                {/* Metric grid */}
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <MetricCard
                        label="Monthly Revenue"
                        value={fmtMoney(startup.monthlyRevenue)}
                        icon={DollarSign}
                        tone="success"
                    />
                    <MetricCard
                        label="Revenue Growth"
                        value={
                            startup.revenueGrowthRate
                                ? `${startup.revenueGrowthRate}%`
                                : "—"
                        }
                        icon={TrendingUp}
                        tone="brand"
                    />
                    <MetricCard
                        label="Total Funding"
                        value={fmtMoney(startup.totalFunding)}
                        icon={Landmark}
                        tone="accent"
                    />
                    <MetricCard
                        label="Monthly Burn"
                        value={fmtMoney(startup.monthlyBurnRate)}
                        icon={Flame}
                        tone="warning"
                    />
                    <MetricCard
                        label="Runway"
                        value={startup.runwayMonths ? `${startup.runwayMonths} mo` : "—"}
                        icon={Clock}
                        tone="brand"
                    />
                    <MetricCard
                        label="Valuation"
                        value={fmtMoney(startup.valuation)}
                        icon={DollarSign}
                        tone="success"
                    />
                    <MetricCard
                        label="Customers"
                        value={fmtNumber(startup.customerCount)}
                        icon={UserCheck}
                        tone="accent"
                    />
                    <MetricCard
                        label="Users"
                        value={fmtNumber(startup.totalUsers)}
                        icon={Users}
                        tone="brand"
                    />
                </div>

                {/* Charts */}
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                    <ChartCard
                        title="Funding vs Burn vs Revenue"
                        subtitle="USD per month / total"
                    >
                        <BarChartBlock data={fundingVsBurn} />
                    </ChartCard>

                    <ChartCard
                        title="Users, Customers & Team"
                        subtitle="Counts"
                    >
                        <BarChartBlock data={usersVsCustomers} />
                    </ChartCard>

                    <ChartCard
                        title="Revenue Growth"
                        subtitle="Percent (%)"
                    >
                        <LineChartBlock data={growthSeries} />
                    </ChartCard>

                    <ChartCard
                        title="Runway"
                        subtitle="Months"
                    >
                        <LineChartBlock data={runwaySeries} color="#06B6D4" />
                    </ChartCard>
                </div>

                {/* AI Summary */}
                <Card>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                                <Brain className="h-5 w-5" />
                            </div>
                            <div>
                                <h3 className="text-base font-semibold text-navy-800">
                                    AI Summary
                                </h3>
                                <p className="mt-0.5 text-xs text-slate-500">
                                    Real predictions from the startup potential & risk model.
                                </p>
                            </div>
                        </div>
                        <Button onClick={handleAnalyze} disabled={predicting}>
                            {predicting ? "Analyzing…" : "Run AI Analysis"}
                        </Button>
                    </div>

                    <div className="mt-6">
                        {predicting && <AILoadingState message="Analyzing startup…" />}

                        {!predicting && predictError && (
                            <AIErrorState message={predictError} onRetry={handleAnalyze} />
                        )}

                        {!predicting && prediction && (
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
                                    <p className="mt-4 text-sm text-slate-600">
                                        Model version:{" "}
                                        <span className="font-medium text-navy-800">
                      {prediction.model_version}
                    </span>
                                    </p>
                                    <p className="mt-3 text-xs text-slate-500">
                                        AI-generated insights are analytical estimates based on your
                                        startup profile and should not be considered financial or
                                        investment advice.
                                    </p>
                                </Card>
                            </div>
                        )}

                        {!predicting && !prediction && !predictError && (
                            <p className="text-sm text-slate-500">
                                Click <span className="font-medium">Run AI Analysis</span> to
                                compute your startup's potential score and risk category.
                            </p>
                        )}
                    </div>
                </Card>
            </div>
        </DashboardLayout>
    );
}