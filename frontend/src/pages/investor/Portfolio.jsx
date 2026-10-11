import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import MetricCard from "../../components/common/MetricCard.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import { messageForError } from "../../utils/errorHandler.js";
import { getInvestorPortfolio } from "../../services/investor/investorService.js";
import {
    Briefcase, Compass, Bookmark, Clock, Building2, AlertCircle,
} from "lucide-react";

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    if (n >= 1_000_000) return `$${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `$${(n / 1_000).toFixed(0)}K`;
    return `$${n}`;
}

function fmtDate(s) {
    if (!s) return "";
    const d = new Date(s);
    return Number.isNaN(d.getTime()) ? s : d.toLocaleDateString("en-US", {
        year: "numeric", month: "short", day: "2-digit",
    });
}

export default function Portfolio() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const result = await getInvestorPortfolio();
            setData(result);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    return (
        <DashboardLayout title="Portfolio">
            <div className="space-y-6">
                <PageHeader
                    title="Portfolio"
                    description="Startups you are actively collaborating with."
                />

                {/* Business rule notice */}
                <Card className="border-amber-200 bg-amber-50">
                    <div className="flex items-start gap-3">
                        <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-amber-600" />
                        <div>
                            <h3 className="text-sm font-semibold text-amber-900">
                                About this portfolio
                            </h3>
                            <p className="mt-1 text-sm text-amber-800">
                                This portfolio lists startups connected to your
                                <strong> ACCEPTED collaboration requests</strong>.
                                An accepted collaboration represents mutual interest —
                                it is <strong>not</strong> a financial investment,
                                and no investment amounts or returns are tracked here.
                            </p>
                        </div>
                    </div>
                </Card>

                {loading ? (
                    <LoadingState message="Loading portfolio…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load portfolio"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !data ? (
                    <EmptyState
                        icon={Briefcase}
                        title="No portfolio data"
                        description="Something went wrong loading your portfolio."
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : (
                    <>
                        {/* Summary metrics */}
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                            <MetricCard
                                label="Active Collaborations"
                                value={data.activeCollaborations ?? 0}
                                icon={Briefcase}
                                tone="success"
                            />
                            <MetricCard
                                label="Pending Requests"
                                value={data.pendingCollaborations ?? 0}
                                icon={Clock}
                                tone="warning"
                            />
                            <MetricCard
                                label="Distinct Industries"
                                value={data.distinctIndustries ?? 0}
                                icon={Building2}
                                tone="accent"
                            />
                            <MetricCard
                                label="Saved Startups"
                                value={data.savedStartups ?? 0}
                                icon={Bookmark}
                                tone="brand"
                            />
                        </div>

                        {/* Section */}
                        <div>
                            <h2 className="text-lg font-semibold text-navy-800">
                                Collaboration Portfolio
                            </h2>
                            <p className="mt-1 text-sm text-slate-500">
                                Startups you have an active collaboration with.
                            </p>
                        </div>

                        {(!data.startups || data.startups.length === 0) ? (
                            <EmptyState
                                icon={Briefcase}
                                title="No accepted collaborations yet."
                                description="Discover startups and express interest. Once a startup accepts, it will appear here."
                                action={
                                    <Link to="/investor/discover" className="btn-primary">
                                        <Compass className="inline h-4 w-4 mr-1" />
                                        Discover Startups
                                    </Link>
                                }
                            />
                        ) : (
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                {data.startups.map((s) => (
                                    <Card key={s.collaborationRequestId} hover className="flex flex-col gap-3">
                                        <div className="flex items-start justify-between gap-3">
                                            <h3 className="text-base font-semibold text-navy-800">
                                                {s.companyName}
                                            </h3>
                                            <Badge tone="success">Collaboration Active</Badge>
                                        </div>
                                        <div className="flex flex-wrap gap-2">
                                            <Badge tone="brand">{s.industry}</Badge>
                                            {s.businessModel && <Badge tone="accent">{s.businessModel}</Badge>}
                                            {s.startupStage && <Badge tone="navy">{s.startupStage}</Badge>}
                                        </div>

                                        {s.description && (
                                            <p className="line-clamp-2 text-sm text-slate-600">
                                                {s.description}
                                            </p>
                                        )}

                                        <div className="grid grid-cols-3 gap-3 border-t border-slate-100 pt-3">
                                            <div>
                                                <div className="text-xs text-slate-500">Funding</div>
                                                <div className="font-medium text-navy-800">
                                                    {fmtMoney(s.totalFunding)}
                                                </div>
                                            </div>
                                            <div>
                                                <div className="text-xs text-slate-500">Revenue</div>
                                                <div className="font-medium text-navy-800">
                                                    {fmtMoney(s.monthlyRevenue)}
                                                </div>
                                            </div>
                                            <div>
                                                <div className="text-xs text-slate-500">Team</div>
                                                <div className="font-medium text-navy-800">
                                                    {s.teamSize ?? "—"}
                                                </div>
                                            </div>
                                        </div>

                                        {s.collaborationAcceptedAt && (
                                            <p className="flex items-center gap-1 text-xs text-slate-400">
                                                <Clock className="h-3 w-3" />
                                                Accepted {fmtDate(s.collaborationAcceptedAt)}
                                            </p>
                                        )}
                                    </Card>
                                ))}
                            </div>
                        )}
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}