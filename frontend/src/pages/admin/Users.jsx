import { useCallback, useEffect, useRef, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import ConfirmDialog from "../../components/common/ConfirmDialog.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { listUsers, updateUserStatus } from "../../services/admin/adminService.js";
import { Users, Search, ChevronLeft, ChevronRight } from "lucide-react";

const PAGE_SIZE = 20;

const ROLE_TONE = { ADMIN: "danger", INVESTOR: "brand", STARTUP: "success" };
const STATUS_TONE = { ACTIVE: "success", DEACTIVATED: "navy" };

export default function AdminUsers() {
    const toast = useToast();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);
    const [search, setSearch] = useState("");
    const [role, setRole] = useState("");
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [confirmTarget, setConfirmTarget] = useState(null);
    const [busyId, setBusyId] = useState(null);
    const debounceRef = useRef(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const result = await listUsers({ page, size: PAGE_SIZE, search, role, status });
            setData(result);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, [page, search, role, status]);

    useEffect(() => { load(); }, [load]);

    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => { setPage(0); }, 350);
        return () => clearTimeout(debounceRef.current);
    }, [search]);

    async function handleConfirmStatus() {
        if (!confirmTarget) return;
        setBusyId(confirmTarget.id);
        const next = confirmTarget.status === "ACTIVE" ? "DEACTIVATED" : "ACTIVE";
        try {
            await updateUserStatus(confirmTarget.id, next);
            toast.success(`User ${next === "ACTIVE" ? "activated" : "deactivated"}.`);
            setConfirmTarget(null);
            load();
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setBusyId(null);
        }
    }

    return (
        <DashboardLayout title="Users">
            <div className="space-y-6">
                <PageHeader title="User Management" description="Manage platform users and account status." />

                <Card>
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                        <div className="relative flex-1">
                            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                            <input
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                placeholder="Search by name or email…"
                                className="input-field pl-9"
                            />
                        </div>
                        <select value={role} onChange={(e) => { setRole(e.target.value); setPage(0); }} className="input-field sm:w-40">
                            <option value="">All roles</option>
                            <option value="ADMIN">Admin</option>
                            <option value="INVESTOR">Investor</option>
                            <option value="STARTUP">Startup</option>
                        </select>
                        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} className="input-field sm:w-40">
                            <option value="">All statuses</option>
                            <option value="ACTIVE">Active</option>
                            <option value="DEACTIVATED">Deactivated</option>
                        </select>
                    </div>
                </Card>

                {loading ? (
                    <LoadingState message="Loading users…" />
                ) : error ? (
                    <ErrorState title="Could not load users" message={error} action={<Button onClick={load}>Retry</Button>} />
                ) : !data || data.content.length === 0 ? (
                    <EmptyState icon={Users} title="No users found" description="Try adjusting your search or filters." />
                ) : (
                    <>
                        <Card className="!p-0 overflow-hidden">
                            <table className="w-full text-sm">
                                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                                <tr>
                                    <th className="px-4 py-3">Name</th>
                                    <th className="px-4 py-3">Email</th>
                                    <th className="px-4 py-3">Role</th>
                                    <th className="px-4 py-3">Status</th>
                                    <th className="px-4 py-3 text-right">Actions</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                {data.content.map((u) => (
                                    <tr key={u.id}>
                                        <td className="px-4 py-3 font-medium text-navy-800">{u.name}</td>
                                        <td className="px-4 py-3 text-slate-600">{u.email}</td>
                                        <td className="px-4 py-3"><Badge tone={ROLE_TONE[u.role] ?? "navy"}>{u.role}</Badge></td>
                                        <td className="px-4 py-3"><Badge tone={STATUS_TONE[u.status] ?? "navy"}>{u.status}</Badge></td>
                                        <td className="px-4 py-3 text-right">
                                            <Button
                                                variant="secondary"
                                                disabled={busyId === u.id}
                                                onClick={() => setConfirmTarget(u)}
                                            >
                                                {u.status === "ACTIVE" ? "Deactivate" : "Activate"}
                                            </Button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </Card>

                        <Card>
                            <div className="flex items-center justify-between">
                                <Button variant="secondary" disabled={page === 0}
                                        onClick={() => setPage((p) => Math.max(0, p - 1))}>
                                    <ChevronLeft className="h-4 w-4" /> Previous
                                </Button>
                                <span className="text-sm text-slate-600">
                                    Page {data.number + 1} of {data.totalPages || 1}
                                </span>
                                <Button variant="secondary"
                                        disabled={(data.number + 1) >= (data.totalPages || 1)}
                                        onClick={() => setPage((p) => p + 1)}>
                                    Next <ChevronRight className="h-4 w-4" />
                                </Button>
                            </div>
                        </Card>
                    </>
                )}
            </div>

            <ConfirmDialog
                open={!!confirmTarget}
                title={confirmTarget?.status === "ACTIVE" ? "Deactivate user?" : "Activate user?"}
                message={
                    confirmTarget
                        ? confirmTarget.status === "ACTIVE"
                            ? `${confirmTarget.name} will not be able to log in until reactivated.`
                            : `${confirmTarget.name} will regain access to the platform.`
                        : ""
                }
                confirmLabel={confirmTarget?.status === "ACTIVE" ? "Deactivate" : "Activate"}
                variant={confirmTarget?.status === "ACTIVE" ? "danger" : "primary"}
                loading={busyId === confirmTarget?.id}
                onConfirm={handleConfirmStatus}
                onCancel={() => setConfirmTarget(null)}
            />
        </DashboardLayout>
    );
}