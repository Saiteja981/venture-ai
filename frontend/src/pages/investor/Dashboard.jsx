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
import { getMyInvestorProfile } from "../../services/investor/investorService.js";
import { UserCircle2, Briefcase, DollarSign, TrendingUp, Shield, Users, ArrowRight } from "lucide-react";

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(n);
}

export default function InvestorDashboard() {
    const { name } = useAuth();
    const toast = useToast();
    const [loading, setLoading] = useState(true);
    const [investor, setInvestor] = useState(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const data = await getMyInvestorProfile();
                if (!cancelled) setInvestor(data);
            } catch (err) {
                if (!cancelled && err?.status !== 404) toast.error(messageForError(err));
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, [toast]);

    return (
        <DashboardLayout title="Investor Dashboard">
            <div className="space-y-6">
                <PageHeader
                    title={`Welcome back, ${name || "investor"}`}
                    description="Manage your investor profile and preferences."
                />

                {loading ? (
                    <Card>Loading…</Card>
                ) : !investor ? (
                    <EmptyState
                        icon={UserCircle2}
                        title="No investor profile yet"
                        description="Create your investor profile to discover startups matched to your investment preferences."
                        action={<Link to="/investor/profile" className="btn-primary">Create Investor Profile</Link>}
                    />
                ) : (
                    <>
                        <Card>
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                                <div>
                                    <h2 className="text-xl font-semibold text-navy-800">{investor.investorName}</h2>
                                    {investor.organizationName && <p className="mt-1 text-sm text-slate-600">{investor.organizationName}</p>}
                                    <div className="mt-2 flex flex-wrap items-center gap-2">
                                        <Badge tone="brand">{investor.investorType}</Badge>
                                        <Badge tone="warning">Risk: {investor.riskPreference}</Badge>
                                    </div>
                                </div>
                                <Link to="/investor/profile" className="btn-secondary">Manage Profile</Link>
                            </div>
                        </Card>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                            <MetricCard label="Investment Type" value={investor.investorType} icon={Briefcase} tone="brand" />
                            <MetricCard label="Min Investment" value={fmtMoney(investor.minimumInvestment)} icon={DollarSign} tone="accent" />
                            <MetricCard label="Max Investment" value={fmtMoney(investor.maximumInvestment)} icon={DollarSign} tone="success" />
                            <MetricCard label="Experience" value={`${investor.investmentExperienceYears ?? "—"} yrs`} icon={TrendingUp} tone="brand" />
                            <MetricCard label="Portfolio" value={investor.portfolioSize ?? "—"} icon={Users} tone="accent" />
                            <MetricCard label="Risk" value={investor.riskPreference} icon={Shield} tone="warning" />
                        </div>

                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">Quick Actions</h3>
                            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-4">
                                <Link to="/investor/profile" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">Manage Profile</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                                <Link to="/investor/discover" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">Discover Startups</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                                <Link to="/investor/recommended" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">Recommended</span>
                                    <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                                </Link>
                                <Link to="/investor/insights" className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40">
                                    <span className="text-sm font-medium text-navy-800">AI Insights</span>
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