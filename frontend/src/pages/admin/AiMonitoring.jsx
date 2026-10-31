import { useCallback, useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import ExportCard from "../../components/reports/ExportCard.jsx";
import axiosClient from "../../services/api/axiosClient.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    downloadAdminAnalytics,
    downloadAdminAuditLogs,
    downloadAdminAiExecutions,
} from "../../services/reports/reportService.js";
import {
    Activity, CheckCircle2, XCircle, AlertTriangle,
    MessageSquare, Star, BarChart3, RefreshCw,
} from "lucide-react";
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
    ResponsiveContainer, Legend,
} from "recharts";

function fmtNumber(n) {
    return new Intl.NumberFormat("en-US").format(n ?? 0);
}

function MetricCard({ icon: Icon, label, value, sub, tone = "navy" }) {
    const toneMap = {
        navy: "bg-slate-100 text-navy-700",
        success: "bg-emerald-100 text-emerald-700",
        danger: "bg-red-100 text-red-700",
        brand: "bg-brand-100 text-brand-700",
        warning: "bg-amber-100 text-amber-700",
    };
    return (
        <Card className="!p-4">
            <div className="flex items-start justify-between">
                <div className="min-w-0">
                    <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                        {label}
                    </div>
                    <div className="mt-1 text-2xl font-semibold text-navy-800">
                        {value}
                    </div>
                    {sub && (
                        <div className="mt-1 text-xs text-slate-500">{sub}</div>
                    )}
                </div>
                {Icon && (
                    <div className={`rounded-lg p-2 ${toneMap[tone] ?? toneMap.navy}`}>
                        <Icon className="h-5 w-5" />
                    </div>
                )}
            </div>
        </Card>
    );
}

export default function AiMonitoring() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const { data } = await axiosClient.get(
                "/admin/ai/monitoring?trendDays=14"
            );
            setData(data);
        } catch (err) {
            setError(messageForError(err) || "Could not load AI monitoring data.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    return (
        <DashboardLayout title="AI Monitoring">
            <div className="space-y-6">
                <PageHeader
                    title="AI Monitoring"
                    description="Operational metrics for AI executions and user feedback. These are not model accuracy measurements."
                    actions={
                        <Button variant="secondary" onClick={load} disabled={loading}>
                            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
                            Refresh
                        </Button>
                    }
                />

                {/* Phase 27: Reports & Export */}
                <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
                    <ExportCard
                        title="Platform Analytics"
                        description="User, startup, and investor totals with distributions."
                        onExport={downloadAdminAnalytics}
                    />
                    <ExportCard
                        title="AI Execution Summary"
                        description="Operational counts and latency aggregates."
                        onExport={downloadAdminAiExecutions}
                    />
                    <ExportCard
                        title="Audit Logs"
                        description="Administrative action history. CSV only due to volume."
                        onExport={downloadAdminAuditLogs}
                        formats={["csv"]}
                    />
                </div>

                {loading ? (
                    <LoadingState message="Loading AI monitoring data…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load monitoring data"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !data || data.totalExecutions === 0 ? (
                    <EmptyState
                        icon={Activity}
                        title="No AI executions recorded yet"
                        description="Once users run predictions, recommendations, or pitch-deck analysis, operational metrics will appear here."
                    />
                ) : (
                    <>
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
                            <MetricCard
                                icon={Activity}
                                label="Total executions"
                                value={fmtNumber(data.totalExecutions)}
                            />
                            <MetricCard
                                icon={CheckCircle2}
                                tone="success"
                                label="Successful"
                                value={fmtNumber(data.totalSuccessfulExecutions)}
                                sub={`${data.overallSuccessRatePercent}% success rate`}
                            />
                            <MetricCard
                                icon={XCircle}
                                tone="danger"
                                label="Failed"
                                value={fmtNumber(data.totalFailedExecutions)}
                            />
                            <MetricCard
                                icon={MessageSquare}
                                tone="brand"
                                label="Feedback entries"
                                value={fmtNumber(data.totalFeedbackSubmissions)}
                            />
                            <MetricCard
                                icon={Star}
                                tone="warning"
                                label="Avg feedback rating"
                                value={
                                    data.averageFeedbackRating != null
                                        ? data.averageFeedbackRating.toFixed(2)
                                        : "—"
                                }
                                sub={data.totalFeedbackSubmissions === 0 ? "No feedback yet" : null}
                            />
                            <MetricCard
                                icon={BarChart3}
                                label="Tasks tracked"
                                value={data.byTask.length}
                            />
                        </div>

                        {data.dailyTrend.length > 0 && (
                            <Card>
                                <div className="flex items-center justify-between">
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Daily executions (last 14 days)
                                    </h3>
                                    <span className="text-xs text-slate-500">
                                        Operational counts — not accuracy
                                    </span>
                                </div>
                                <div className="mt-4" style={{ height: 260 }}>
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={data.dailyTrend}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                            <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                                            <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                                            <Tooltip />
                                            <Legend />
                                            <Bar
                                                dataKey="successfulExecutions"
                                                name="Successful"
                                                stackId="a"
                                                fill="#10b981"
                                            />
                                            <Bar
                                                dataKey="failedExecutions"
                                                name="Failed"
                                                stackId="a"
                                                fill="#ef4444"
                                            />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            </Card>
                        )}

                        <Card className="!p-0 overflow-hidden">
                            <div className="border-b border-slate-100 px-4 py-3">
                                <h3 className="text-base font-semibold text-navy-800">
                                    Executions by task
                                </h3>
                            </div>
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                                    <tr>
                                        <th className="px-4 py-3">Task</th>
                                        <th className="px-4 py-3 text-right">Total</th>
                                        <th className="px-4 py-3 text-right">Success</th>
                                        <th className="px-4 py-3 text-right">Failed</th>
                                        <th className="px-4 py-3 text-right">Success %</th>
                                        <th className="px-4 py-3 text-right">Avg latency</th>
                                        <th className="px-4 py-3 text-right">Max latency</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {data.byTask.map((t) => (
                                        <tr key={t.task}>
                                            <td className="px-4 py-3 font-medium text-navy-800">
                                                {t.task}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {fmtNumber(t.totalExecutions)}
                                            </td>
                                            <td className="px-4 py-3 text-right text-emerald-700">
                                                {fmtNumber(t.successfulExecutions)}
                                            </td>
                                            <td className="px-4 py-3 text-right text-red-700">
                                                {fmtNumber(t.failedExecutions)}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-700">
                                                {t.successRatePercent}%
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {t.avgLatencyMs != null
                                                    ? `${t.avgLatencyMs} ms`
                                                    : "—"}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {t.maxLatencyMs != null
                                                    ? `${t.maxLatencyMs} ms`
                                                    : "—"}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </Card>

                        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                            <Card>
                                <h3 className="text-base font-semibold text-navy-800">
                                    Executions by model version
                                </h3>
                                {data.byModelVersion.length === 0 ? (
                                    <p className="mt-3 text-sm text-slate-500">
                                        No versioned executions recorded.
                                    </p>
                                ) : (
                                    <ul className="mt-3 space-y-2">
                                        {data.byModelVersion.map((m) => (
                                            <li
                                                key={m.modelVersion}
                                                className="flex items-center justify-between text-sm"
                                            >
                                                <span className="font-medium text-navy-800">
                                                    {m.modelVersion}
                                                </span>
                                                <span className="text-slate-600">
                                                    {fmtNumber(m.executionCount)}
                                                </span>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </Card>

                            <Card>
                                <h3 className="text-base font-semibold text-navy-800">
                                    Error categories
                                </h3>
                                {data.errorsByCategory.length === 0 ? (
                                    <p className="mt-3 text-sm text-slate-500">
                                        No failures recorded in this window.
                                    </p>
                                ) : (
                                    <ul className="mt-3 space-y-2">
                                        {data.errorsByCategory.map((e) => (
                                            <li
                                                key={e.errorCategory}
                                                className="flex items-center justify-between text-sm"
                                            >
                                                <span className="inline-flex items-center gap-2">
                                                    <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
                                                    <span className="font-medium text-navy-800">
                                                        {e.errorCategory}
                                                    </span>
                                                </span>
                                                <span className="text-slate-600">{e.count}</span>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </Card>
                        </div>

                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">
                                Feedback by category
                            </h3>
                            {data.feedbackByCategory.length === 0 ? (
                                <p className="mt-3 text-sm text-slate-500">
                                    No feedback submitted yet.
                                </p>
                            ) : (
                                <div className="mt-3 flex flex-wrap gap-2">
                                    {data.feedbackByCategory.map((f) => (
                                        <Badge key={f.category} tone="brand">
                                            {f.category}: {f.count}
                                        </Badge>
                                    ))}
                                </div>
                            )}
                        </Card>

                        {data.note && (
                            <Card className="border-amber-100 bg-amber-50/50">
                                <div className="flex items-start gap-3">
                                    <AlertTriangle className="mt-0.5 h-4 w-4 flex-shrink-0 text-amber-600" />
                                    <p className="text-xs text-amber-900">{data.note}</p>
                                </div>
                            </Card>
                        )}
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}