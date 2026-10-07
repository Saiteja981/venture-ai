import { useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import MetricCard from "../../components/common/MetricCard.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import Button from "../../components/common/Button.jsx";
import ChartCard from "../../components/analytics/ChartCard.jsx";
import BarChartBlock from "../../components/analytics/BarChartBlock.jsx";
import PieChartBlock from "../../components/analytics/PieChartBlock.jsx";
import { getAdminStats } from "../../services/analytics/analyticsService.js";
import { messageForError } from "../../utils/errorHandler.js";
import { Users, Rocket, Building2, ShieldCheck } from "lucide-react";

function mapToSeries(mapObj) {
    if (!mapObj) return [];
    return Object.entries(mapObj).map(([label, value]) => ({
        label,
        value: Number(value) || 0,
    }));
}

export default function PlatformAnalytics() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [stats, setStats] = useState(null);

    async function load() {
        setLoading(true);
        setError("");
        try {
            const data = await getAdminStats();
            setStats(data);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    if (loading) {
        return (
            <DashboardLayout title="Platform Analytics">
                <LoadingState message="Loading platform analytics…" />
            </DashboardLayout>
        );
    }

    if (error) {
        return (
            <DashboardLayout title="Platform Analytics">
                <ErrorState
                    title="Could not load platform analytics"
                    message={error}
                    action={<Button onClick={load}>Retry</Button>}
                />
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout title="Platform Analytics">
            <div className="space-y-6">
                <PageHeader
                    title="Platform Analytics"
                    description="Live platform-wide statistics from Venture AI's database."
                />

                {/* Totals */}
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <MetricCard
                        label="Total Users"
                        value={stats?.totalUsers ?? 0}
                        icon={Users}
                        tone="brand"
                    />
                    <MetricCard
                        label="Total Startups"
                        value={stats?.totalStartups ?? 0}
                        icon={Rocket}
                        tone="accent"
                    />
                    <MetricCard
                        label="Total Investors"
                        value={stats?.totalInvestors ?? 0}
                        icon={Building2}
                        tone="success"
                    />
                    <MetricCard
                        label="Admins"
                        value={stats?.totalAdmins ?? 0}
                        icon={ShieldCheck}
                        tone="warning"
                    />
                </div>

                {/* Distributions */}
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                    <ChartCard
                        title="Users by Role"
                        subtitle="Distribution of registered users"
                    >
                        <PieChartBlock data={mapToSeries(stats?.usersByRole)} />
                    </ChartCard>

                    <ChartCard
                        title="Startups by Industry"
                        subtitle="Number of startups per industry"
                    >
                        <BarChartBlock data={mapToSeries(stats?.startupsByIndustry)} />
                    </ChartCard>

                    <ChartCard
                        title="Startups by Stage"
                        subtitle="Number of startups per funding stage"
                    >
                        <BarChartBlock data={mapToSeries(stats?.startupsByStage)} />
                    </ChartCard>

                    <ChartCard
                        title="Investors by Type"
                        subtitle="Distribution of investor types"
                    >
                        <PieChartBlock data={mapToSeries(stats?.investorsByType)} />
                    </ChartCard>
                </div>

                {/* Attribution */}
                <Card>
                    <p className="text-xs text-slate-500">
                        All values shown are live database counts. No caching or estimation
                        is applied.
                    </p>
                </Card>
            </div>
        </DashboardLayout>
    );
}