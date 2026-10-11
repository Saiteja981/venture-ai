import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import ConfirmDialog from "../../components/common/ConfirmDialog.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    getInvestorCollaborations,
    withdrawCollaboration,
} from "../../services/collaboration/collaborationService.js";
import { createOrGetConversation } from "../../services/messaging/messagingService.js";
import { Handshake, Clock, CheckCircle2, XCircle, Undo2, MessageSquare } from "lucide-react";

const STATUS_TONE = {
    PENDING: "warning",
    ACCEPTED: "success",
    REJECTED: "danger",
    WITHDRAWN: "navy",
};

const STATUS_LABEL = {
    PENDING: "Pending",
    ACCEPTED: "Collaboration Active",
    REJECTED: "Rejected",
    WITHDRAWN: "Withdrawn",
};

function fmtDateTime(s) {
    if (!s) return "";
    const d = new Date(s);
    return Number.isNaN(d.getTime()) ? s : d.toLocaleString("en-US", {
        year: "numeric", month: "short", day: "2-digit",
        hour: "2-digit", minute: "2-digit",
    });
}

export default function InvestorCollaborations() {
    const toast = useToast();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [items, setItems] = useState([]);
    const [confirmWithdraw, setConfirmWithdraw] = useState(null);
    const [busyId, setBusyId] = useState(null);
    const [busyMsgId, setBusyMsgId] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await getInvestorCollaborations();
            setItems(Array.isArray(data) ? data : []);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    async function handleWithdraw() {
        if (!confirmWithdraw) return;
        setBusyId(confirmWithdraw.id);
        try {
            const updated = await withdrawCollaboration(confirmWithdraw.id);
            setItems((prev) => prev.map((x) => (x.id === updated.id ? updated : x)));
            setConfirmWithdraw(null);
            toast.success("Collaboration request withdrawn.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setBusyId(null);
        }
    }

    async function handleOpenMessages(r) {
        setBusyMsgId(r.id);
        try {
            const conv = await createOrGetConversation(r.id);
            navigate(`/investor/messages/${conv.id}`);
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setBusyMsgId(null);
        }
    }

    return (
        <DashboardLayout title="Collaborations">
            <div className="space-y-6">
                <PageHeader
                    title="Collaborations"
                    description="Track your sent collaboration requests and their status."
                />

                {loading ? (
                    <LoadingState message="Loading your collaboration requests…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load collaborations"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : items.length === 0 ? (
                    <EmptyState
                        icon={Handshake}
                        title="No collaboration requests yet."
                        description="Browse startups on Discover Startups and express interest to begin a collaboration."
                    />
                ) : (
                    <div className="space-y-3">
                        {items.map((r) => (
                            <Card key={r.id}>
                                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2">
                                            <h3 className="text-base font-semibold text-navy-800">
                                                {r.startupName}
                                            </h3>
                                            <Badge tone={STATUS_TONE[r.status] ?? "navy"}>
                                                {STATUS_LABEL[r.status] ?? r.status}
                                            </Badge>
                                        </div>
                                        <div className="mt-2 flex flex-wrap gap-2">
                                            {r.startupIndustry && (
                                                <Badge tone="brand">{r.startupIndustry}</Badge>
                                            )}
                                            {r.startupStage && (
                                                <Badge tone="accent">{r.startupStage}</Badge>
                                            )}
                                        </div>
                                        {r.message && (
                                            <p className="mt-3 text-sm text-slate-600">
                                                {r.message}
                                            </p>
                                        )}
                                        <p className="mt-2 flex items-center gap-1 text-xs text-slate-400">
                                            <Clock className="h-3 w-3" />
                                            Sent {fmtDateTime(r.createdAt)}
                                        </p>
                                    </div>

                                    <div className="flex flex-shrink-0 flex-wrap items-center gap-2">
                                        {r.status === "PENDING" && (
                                            <Button
                                                variant="secondary"
                                                onClick={() => setConfirmWithdraw(r)}
                                                disabled={busyId === r.id}
                                            >
                                                <Undo2 className="h-4 w-4" />
                                                Withdraw
                                            </Button>
                                        )}
                                        {r.status === "ACCEPTED" && (
                                            <>
                                                <span className="inline-flex items-center gap-2 text-sm font-medium text-emerald-600">
                                                    <CheckCircle2 className="h-4 w-4" />
                                                    Active
                                                </span>
                                                <Button
                                                    variant="secondary"
                                                    onClick={() => handleOpenMessages(r)}
                                                    disabled={busyMsgId === r.id}
                                                >
                                                    <MessageSquare className="h-4 w-4" />
                                                    {busyMsgId === r.id ? "Opening…" : "Open Messages"}
                                                </Button>
                                            </>
                                        )}
                                        {r.status === "REJECTED" && (
                                            <span className="inline-flex items-center gap-2 text-sm text-red-600">
                                                <XCircle className="h-4 w-4" />
                                                Declined
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </Card>
                        ))}
                    </div>
                )}
            </div>

            <ConfirmDialog
                open={!!confirmWithdraw}
                title="Withdraw request?"
                message={
                    confirmWithdraw
                        ? `Withdraw your collaboration request to ${confirmWithdraw.startupName}?`
                        : ""
                }
                confirmLabel="Withdraw"
                variant="danger"
                loading={busyId === confirmWithdraw?.id}
                onConfirm={handleWithdraw}
                onCancel={() => setConfirmWithdraw(null)}
            />
        </DashboardLayout>
    );
}