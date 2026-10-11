import { useCallback, useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import { messageForError } from "../../utils/errorHandler.js";
import { listAuditLogs } from "../../services/admin/adminService.js";
import { ScrollText, ChevronLeft, ChevronRight } from "lucide-react";

const PAGE_SIZE = 25;

const OUTCOME_TONE = { SUCCESS: "success", DENIED: "warning", FAILED: "danger" };

function fmtDate(s) {
    if (!s) return "";
    const d = new Date(s);
    return Number.isNaN(d.getTime())
        ? s
        : d.toLocaleString("en-US", {
              month: "short",
              day: "2-digit",
              hour: "2-digit",
              minute: "2-digit",
          });
}

export default function AuditLogs() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);
    const [action, setAction] = useState("");
    const [outcome, setOutcome] = useState("");
    const [page, setPage] = useState(0);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const result = await listAuditLogs({ page, size: PAGE_SIZE, action, outcome });
            setData(result);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, [page, action, outcome]);

    useEffect(() => {
        load();
    }, [load]);

    return (
        <DashboardLayout title="Audit Logs">
            <div className="space-y-6">
                <PageHeader
                    title="Audit Logs"
                    description="Administrative actions recorded by the platform."
                />

                <Card>
                    <div className="flex flex-col gap-3 sm:flex-row">
                        <input
                            value={action}
                            onChange={(e) => {
                                setAction(e.target.value);
                                setPage(0);
                            }}
                            placeholder="Filter by action (e.g. USER_DEACTIVATE)"
                            className="input-field flex-1"
                        />
                        <select
                            value={outcome}
                            onChange={(e) => {
                                setOutcome(e.target.value);
                                setPage(0);
                            }}
                            className="input-field sm:w-40"
                        >
                            <option value="">All outcomes</option>
                            <option value="SUCCESS">Success</option>
                            <option value="DENIED">Denied</option>
                            <option value="FAILED">Failed</option>
                        </select>
                    </div>
                </Card>

                {loading ? (
                    <LoadingState message="Loading audit logs…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load audit logs"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !data || data.content.length === 0 ? (
                    <EmptyState
                        icon={ScrollText}
                        title="No audit log entries"
                        description="No administrative actions match the current filters."
                    />
                ) : (
                    <>
                        <Card className="!p-0 overflow-hidden">
                            <table className="w-full text-sm">
                                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                                    <tr>
                                        <th className="px-4 py-3">When</th>
                                        <th className="px-4 py-3">Actor</th>
                                        <th className="px-4 py-3">Action</th>
                                        <th className="px-4 py-3">Target</th>
                                        <th className="px-4 py-3">Outcome</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {data.content.map((a) => (
                                        <tr key={a.id}>
                                            <td className="px-4 py-3 text-slate-500">
                                                {fmtDate(a.createdAt)}
                                            </td>
                                            <td className="px-4 py-3 text-slate-600">
                                                {a.actorEmail ?? "—"}
                                            </td>
                                            <td className="px-4 py-3 font-medium text-navy-800">
                                                {a.action}
                                            </td>
                                            <td className="px-4 py-3 text-slate-600">
                                                {a.targetType}
                                                {a.targetId ? ` #${a.targetId}` : ""}
                                            </td>
                                            <td className="px-4 py-3">
                                                <Badge tone={OUTCOME_TONE[a.outcome] ?? "navy"}>
                                                    {a.outcome}
                                                </Badge>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </Card>

                        <Card>
                            <div className="flex items-center justify-between">
                                <Button
                                    variant="secondary"
                                    disabled={page === 0}
                                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                                >
                                    <ChevronLeft className="h-4 w-4" /> Previous
                                </Button>
                                <span className="text-sm text-slate-600">
                                    Page {data.currentPage + 1} of {data.totalPages || 1}
                                </span>
                                <Button
                                    variant="secondary"
                                    disabled={data.last}
                                    onClick={() => setPage((p) => p + 1)}
                                >
                                    Next <ChevronRight className="h-4 w-4" />
                                </Button>
                            </div>
                        </Card>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}