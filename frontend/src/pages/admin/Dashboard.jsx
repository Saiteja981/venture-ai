import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import { messageForError } from "../../utils/errorHandler.js";
import { getAdminStats, listRecentAuditLogs } from "../../services/admin/adminService.js";
import {
    Users, Rocket, Building2, Brain, Activity,
    Clock, ShieldCheck, XCircle, CheckCircle2,
} from "lucide-react";

function StatCard({ icon: Icon, label, value, sub, tone = "navy", to }) {
    const toneMap = {
        navy: "bg-slate-100 text-navy-700",
        brand: "bg-brand-100 text-brand-700",
        success: "bg-emerald-100 text-emerald-700",
        warning: "bg-amber-100 text-amber-700",
        danger: "bg-red-100 text-red-700",
    };
    const body = (
        <Card className="!p-4 h-full hover:shadow-md transition">
            <div className="flex items-start justify-between">
                <div className="min-w-0">
                    <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                        {label}
                    </div>
                    <div className="mt-1 text-3xl font-semibold text-navy-800">
                        {value}
                    </div>
                    {sub && <div className="mt-1 text-xs text-slate-500">{sub}</div>}
                </div>
                <div className={`rounded-lg p-2 ${toneMap[tone]}`}>
                    <Icon className="h-5 w-5" />
                </div>
            </div>
        </Card>
    );
    return to ? <Link to={to}>{body}</Link> : body;
}

const ACTION_TONE = {
    USER_ACTIVATE: "success",
    USER_DEACTIVATE: "warning",
    USER_STATUS_UPDATE: "brand",
};

function fmtDate(s) {
    if (!s) return "";
    const d = new Date(s);
    return Number.isNaN(d.getTime())
        ? s
        : d.toLocaleString("en-US", {
            month: "short", day: "2-digit",
            hour: "2-digit", minute: "2-digit",
        });
}

export default function AdminDashboard() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [stats, setStats] = useState(null);
    const [recent, setRecent] = useState([]);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const [s, r] = await Promise.all([
                getAdminStats(),
                listRecentAuditLogs(6),
            ]);
            setStats(s);
            setRecent(r?.content ?? []);
        } catch (err) {
            setError(messageForError(err) || "Could not load admin data.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    return (
        <DashboardLayout title="Admin Dashboard">
            <div className="space-y-6">
                <div>
                    <h1 className="text-2xl font-semibold text-navy-800">
                        Admin Dashboard
                    </h1>
                    <p className="mt-1 text-sm text-slate-500">
                        Oversight of users, startups, investors, and platform analytics.
                    </p>
                </div>

                {loading ? (
                    <LoadingState message="Loading platform data…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load platform data"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : (
                    <>
                        {/* Top metric cards */}
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                            <StatCard
                                icon={Users}
                                tone="brand"
                                label="Total Users"
                                value={stats.totalUsers}
                                sub={`${stats.totalAdmins} admin${stats.totalAdmins === 1 ? "" : "s"}`}
                                to="/admin/users"
                            />
                            <StatCard
                                icon={Rocket}
                                tone="success"
                                label="Total Startups"
                                value={stats.totalStartups}
                                to="/admin/startups"
                            />
                            <StatCard
                                icon={Building2}
                                tone="warning"
                                label="Total Investors"
                                value={stats.totalInvestors}
                                to="/admin/investors"
                            />
                            <StatCard
                                icon={Brain}
                                label="AI Executions"
                                value={stats.totalAiExecutions}
                                to="/admin/ai-monitoring"
                            />
                        </div>

                        {/* Users by role */}
                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">
                                Users by role
                            </h3>
                            <div className="mt-4 flex flex-wrap gap-3">
                                {Object.entries(stats.usersByRole || {}).map(([role, count]) => (
                                    <Badge
                                        key={role}
                                        tone={
                                            role === "ADMIN"
                                                ? "danger"
                                                : role === "INVESTOR"
                                                    ? "brand"
                                                    : "success"
                                        }
                                    >
                                        {role}: {count}
                                    </Badge>
                                ))}
                                {Object.keys(stats.usersByRole || {}).length === 0 && (
                                    <p className="text-sm text-slate-500">No users yet.</p>
                                )}
                            </div>
                        </Card>

                        {/* Recent activity from audit log */}
                        <Card className="!p-0 overflow-hidden">
                            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
                                <div>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Recent Platform Activity
                                    </h3>
                                    <p className="mt-0.5 text-xs text-slate-500">
                                        Latest administrative actions from the audit log.
                                    </p>
                                </div>
                                <Link
                                    to="/admin/audit-logs"
                                    className="text-xs font-medium text-brand-600 hover:underline"
                                >
                                    View all →
                                </Link>
                            </div>

                            {recent.length === 0 ? (
                                <div className="px-4 py-8 text-center text-sm text-slate-500">
                                    No administrative actions recorded yet.
                                </div>
                            ) : (
                                <ul className="divide-y divide-slate-100">
                                    {recent.map((a) => (
                                        <li key={a.id} className="flex items-center gap-3 px-4 py-3">
                                            <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-slate-100">
                                                {a.outcome === "SUCCESS" ? (
                                                    <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                                                ) : a.outcome === "DENIED" ? (
                                                    <XCircle className="h-4 w-4 text-amber-600" />
                                                ) : (
                                                    <Activity className="h-4 w-4 text-slate-500" />
                                                )}
                                            </div>
                                            <div className="min-w-0 flex-1">
                                                <div className="flex items-center gap-2">
                                                    <span className="truncate text-sm font-medium text-navy-800">
                                                        {a.action}
                                                    </span>
                                                    <Badge tone={ACTION_TONE[a.action] ?? "navy"}>
                                                        {a.outcome}
                                                    </Badge>
                                                </div>
                                                <div className="mt-0.5 flex items-center gap-2 text-xs text-slate-500">
                                                    <span>{a.actorEmail ?? "system"}</span>
                                                    {a.targetType && (
                                                        <>
                                                            <span>·</span>
                                                            <span>
                                                                {a.targetType}
                                                                {a.targetId ? ` #${a.targetId}` : ""}
                                                            </span>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="flex-shrink-0 text-right text-[11px] text-slate-400">
                                                <Clock className="ml-auto h-3 w-3" />
                                                {fmtDate(a.createdAt)}
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </Card>

                        {/* Quick actions */}
                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">
                                Quick Actions
                            </h3>
                            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
                                <Link
                                    to="/admin/users"
                                    className="flex items-center justify-between rounded-lg border border-slate-100 px-4 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                                >
                                    Manage Users
                                    <Users className="h-4 w-4 text-slate-400" />
                                </Link>
                                <Link
                                    to="/admin/startups"
                                    className="flex items-center justify-between rounded-lg border border-slate-100 px-4 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                                >
                                    Manage Startups
                                    <Rocket className="h-4 w-4 text-slate-400" />
                                </Link>
                                <Link
                                    to="/admin/investors"
                                    className="flex items-center justify-between rounded-lg border border-slate-100 px-4 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                                >
                                    Manage Investors
                                    <Building2 className="h-4 w-4 text-slate-400" />
                                </Link>
                                <Link
                                    to="/admin/audit-logs"
                                    className="flex items-center justify-between rounded-lg border border-slate-100 px-4 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                                >
                                    Audit Logs
                                    <ShieldCheck className="h-4 w-4 text-slate-400" />
                                </Link>
                                <Link
                                    to="/admin/ai-monitoring"
                                    className="flex items-center justify-between rounded-lg border border-slate-100 px-4 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                                >
                                    AI Monitoring
                                    <Activity className="h-4 w-4 text-slate-400" />
                                </Link>
                                <Link
                                    to="/admin/platform-analytics"
                                    className="flex items-center justify-between rounded-lg border border-slate-100 px-4 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                                >
                                    Platform Analytics
                                    <Activity className="h-4 w-4 text-slate-400" />
                                </Link>
                            </div>
                        </Card>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}