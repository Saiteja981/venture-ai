import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import MetricCard from "../../components/common/MetricCard.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import { useAuth } from "../../hooks/useAuth.js";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { getMyStartups } from "../../services/startup/startupService.js";
import {
    Rocket, Users, UserCheck, DollarSign, Landmark, Clock, ArrowRight,
} from "lucide-react";

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(n);
}
function fmtNumber(v) {
    if (v === null || v === undefined) return "—";
    return new Intl.NumberFormat("en-US").format(Number(v));
}

export default function StartupDashboard() {
    const { name } = useAuth();
    const toast = useToast();
    const [loading, setLoading] = useState(true);
    const [startup, setStartup] = useState(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const list = await getMyStartups();
                if (!cancelled) setStartup(Array.isArray(list) && list.length ? list[0] : null);
            } catch (err) {
                if (!cancelled) toast.error(messageForError(err));
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, [toast]);

    return (
        <DashboardLayout title="Startup Dashboard">
            <div className="space-y-6">
                <PageHeader
                    title={`Welcome back, ${name || "founder"}`}
                    description="Track your startup profile and key metrics."
                />

                {loading ? (
                    <Card>Loading…</Card>
                ) : !startup ? (
                    <EmptyState
                        icon={Rocket}
                        title="No startup profile yet"
                        description="Create your startup profile to unlock dashboards, AI insights, and investor matching."
                        action={<Link to="/startup/profile" className="btn-primary">Create Startup Profile</Link>}
                    />
                ) : (
                    <>
                        <Card>
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                                <div>
                                    <h2 className="text-xl font-semibold text-navy-800">{startup.companyName}</h2>
                                    <div className="mt-2 flex flex-wrap items-center gap-2">
                                        <Badge tone="brand">{startup.industry}</Badge>
                                        <Badge tone="accent">{startup.businessModel}</Badge>
                                        <Badge tone="success">{startup.startupStage}</Badge>
                                    </div>
                                </div>
                                <Link to="/startup/profile" className="btn-secondary">Manage Profile</Link>
                            </div>
                        </Card>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                            <MetricCard label="Team" value={startup.teamSize ?? "—"} icon={Users} tone="brand" />
                            <MetricCard label="Customers" value={fmtNumber(startup.customerCount)} icon={UserCheck} tone="accent" />
                            <MetricCard label="Users" value={fmtNumber(startup.totalUsers)} icon={Users} tone="success" />
                            <MetricCard label="Revenue" value={fmtMoney(startup.monthlyRevenue)} icon={DollarSign} tone="brand" />
                            <MetricCard label="Funding" value={fmtMoney(startup.totalFunding)} icon={Landmark} tone="warning" />
                            <MetricCard label="Runway" value={startup.runwayMonths ? `${startup.runwayMonths} mo` : "—"} icon={Clock} tone="accent" />
                            <MetricCard label="Valuation" value={fmtMoney(startup.valuation)} icon={DollarSign} tone="success" />
                            <MetricCard label="Growth" value={startup.revenueGrowthRate ? `${startup.revenueGrowthRate}%` : "—"} icon={ArrowRight} tone="brand" />
                        </div>

                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">Quick Actions</h3>
                            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-4">
                                <Link to="/startup/profile" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">Manage Startup</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                                <Link to="/startup/potential" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">View AI Potential</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                                <Link to="/startup/pitch-deck" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">Analyze Pitch Deck</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                                <Link to="/startup/matches" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">Investor Matches</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                            </div>
                        </Card>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}