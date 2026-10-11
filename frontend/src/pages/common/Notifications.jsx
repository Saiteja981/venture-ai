import { useCallback, useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import Badge from "../../components/common/Badge.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import ConfirmDialog from "../../components/common/ConfirmDialog.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useNotificationContext } from "../../context/NotificationContext.jsx";
import { messageForError } from "../../utils/errorHandler.js";
import {
    getNotifications,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    deleteNotification,
} from "../../services/notification/notificationService.js";
import {
    Bell, Brain, Sparkles, Rocket, UserCircle2, Briefcase,
    Mail, CheckCheck, Trash2, Check,
} from "lucide-react";

const TYPE_META = {
    SYSTEM:              { icon: Bell,         tone: "navy" },
    AI_RECOMMENDATION:   { icon: Brain,        tone: "brand" },
    STARTUP_UPDATE:      { icon: Rocket,       tone: "accent" },
    INVESTOR_UPDATE:     { icon: UserCircle2,  tone: "accent" },
    COLLABORATION:       { icon: Briefcase,    tone: "warning" },
    MESSAGE:             { icon: Mail,         tone: "success" },
};

function fmtDateTime(s) {
    if (!s) return "";
    const d = new Date(s);
    if (Number.isNaN(d.getTime())) return s;
    return d.toLocaleString("en-US", {
        year: "numeric", month: "short", day: "2-digit",
        hour: "2-digit", minute: "2-digit",
    });
}

export default function NotificationsPage() {
    const toast = useToast();
    const { refresh: refreshBadge } = useNotificationContext();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [items, setItems] = useState([]);
    const [busyId, setBusyId] = useState(null);
    const [confirmDelete, setConfirmDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await getNotifications();
            setItems(Array.isArray(data) ? data : []);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    async function handleMarkRead(n) {
        if (n.isRead) return;
        setBusyId(n.id);
        try {
            const updated = await markNotificationAsRead(n.id);
            setItems((prev) => prev.map((x) => (x.id === n.id ? updated : x)));
            await refreshBadge();
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setBusyId(null);
        }
    }

    async function handleMarkAll() {
        const unread = items.filter((x) => !x.isRead).length;
        if (unread === 0) {
            toast.info("No unread notifications.");
            return;
        }
        try {
            await markAllNotificationsAsRead();
            setItems((prev) => prev.map((x) => ({ ...x, isRead: true })));
            await refreshBadge();
            toast.success("All notifications marked as read.");
        } catch (err) {
            toast.error(messageForError(err));
        }
    }

    async function handleDelete() {
        if (!confirmDelete) return;
        setDeleting(true);
        try {
            await deleteNotification(confirmDelete.id);
            setItems((prev) => prev.filter((x) => x.id !== confirmDelete.id));
            setConfirmDelete(null);
            await refreshBadge();
            toast.success("Notification deleted.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setDeleting(false);
        }
    }

    const unreadCount = items.filter((x) => !x.isRead).length;

    return (
        <DashboardLayout title="Notifications">
            <div className="space-y-6">
                <PageHeader
                    title="Notifications"
                    description="Your AI activity, platform updates, and account events."
                    actions={
                        <Button
                            variant="secondary"
                            onClick={handleMarkAll}
                            disabled={unreadCount === 0}
                        >
                            <CheckCheck className="h-4 w-4" />
                            Mark all as read
                        </Button>
                    }
                />

                {loading ? (
                    <LoadingState message="Loading your notifications…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load notifications"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : items.length === 0 ? (
                    <EmptyState
                        icon={Bell}
                        title="No notifications yet"
                        description="When you run AI analyses or receive updates, they will appear here."
                    />
                ) : (
                    <>
                        <Card>
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <span className="text-sm text-slate-500">Total</span>
                                    <Badge tone="navy">{items.length}</Badge>
                                </div>
                                <div className="flex items-center gap-3">
                                    <span className="text-sm text-slate-500">Unread</span>
                                    <Badge tone={unreadCount > 0 ? "brand" : "navy"}>{unreadCount}</Badge>
                                </div>
                            </div>
                        </Card>

                        <div className="space-y-3">
                            {items.map((n) => {
                                const meta = TYPE_META[n.type] ?? TYPE_META.SYSTEM;
                                const Icon = meta.icon;
                                const busy = busyId === n.id;

                                return (
                                    <Card
                                        key={n.id}
                                        className={`transition-colors ${n.isRead ? "" : "border-brand-200 bg-brand-50/40"}`}
                                    >
                                        <div className="flex items-start gap-4">
                                            <div
                                                className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg ${
                                                    n.type === "AI_RECOMMENDATION"
                                                        ? "bg-brand-50 text-brand-600"
                                                        : n.type === "STARTUP_UPDATE"
                                                            ? "bg-cyan-50 text-cyan-600"
                                                            : n.type === "INVESTOR_UPDATE"
                                                                ? "bg-cyan-50 text-cyan-600"
                                                                : n.type === "COLLABORATION"
                                                                    ? "bg-amber-50 text-amber-600"
                                                                    : n.type === "MESSAGE"
                                                                        ? "bg-emerald-50 text-emerald-600"
                                                                        : "bg-slate-100 text-navy-800"
                                                }`}
                                            >
                                                <Icon className="h-5 w-5" />
                                            </div>

                                            <div className="min-w-0 flex-1">
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <h3 className="text-sm font-semibold text-navy-800">
                                                        {n.title}
                                                    </h3>
                                                    <Badge tone={meta.tone}>{n.type}</Badge>
                                                    {!n.isRead && (
                                                        <span className="inline-flex items-center gap-1 rounded-full bg-brand-600 px-2 py-0.5 text-[10px] font-semibold text-white">
                              NEW
                            </span>
                                                    )}
                                                </div>
                                                <p className="mt-1 text-sm text-slate-600">{n.message}</p>
                                                <p className="mt-2 text-xs text-slate-400">
                                                    {fmtDateTime(n.createdAt)}
                                                </p>
                                            </div>

                                            <div className="flex flex-shrink-0 gap-1">
                                                {!n.isRead && (
                                                    <Button
                                                        variant="secondary"
                                                        onClick={() => handleMarkRead(n)}
                                                        disabled={busy}
                                                        className="!px-3 !py-1.5 text-xs"
                                                    >
                                                        <Check className="h-3.5 w-3.5" />
                                                        Read
                                                    </Button>
                                                )}
                                                <Button
                                                    variant="secondary"
                                                    onClick={() => setConfirmDelete(n)}
                                                    disabled={busy}
                                                    className="!px-3 !py-1.5 text-xs"
                                                >
                                                    <Trash2 className="h-3.5 w-3.5" />
                                                    Delete
                                                </Button>
                                            </div>
                                        </div>
                                    </Card>
                                );
                            })}
                        </div>
                    </>
                )}
            </div>

            <ConfirmDialog
                open={!!confirmDelete}
                title="Delete notification?"
                message="This notification will be permanently removed."
                confirmLabel="Delete"
                variant="danger"
                loading={deleting}
                onConfirm={handleDelete}
                onCancel={() => setConfirmDelete(null)}
            />
        </DashboardLayout>
    );
}