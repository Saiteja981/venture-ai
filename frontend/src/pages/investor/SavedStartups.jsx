import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    getSavedStartups,
    removeSavedStartup,
} from "../../services/investor/investorService.js";
import { Bookmark, Trash2, Compass, Clock } from "lucide-react";

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

export default function SavedStartups() {
    const toast = useToast();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [items, setItems] = useState([]);
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await getSavedStartups();
            setItems(Array.isArray(data) ? data : []);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    async function handleRemove(s) {
        setBusyId(s.startupId);
        try {
            await removeSavedStartup(s.startupId);
            setItems((prev) => prev.filter((x) => x.startupId !== s.startupId));
            toast.success("Removed from saved startups.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setBusyId(null);
        }
    }

    return (
        <DashboardLayout title="Saved Startups">
            <div className="space-y-6">
                <PageHeader
                    title="Saved Startups"
                    description="Startups you bookmarked for later review."
                />

                {loading ? (
                    <LoadingState message="Loading saved startups…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load saved startups"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : items.length === 0 ? (
                    <EmptyState
                        icon={Bookmark}
                        title="You haven't saved any startups yet."
                        description="Browse Discover Startups and click the bookmark icon to save a startup."
                        action={
                            <Link to="/investor/discover" className="btn-primary">
                                <Compass className="inline h-4 w-4 mr-1" />
                                Discover Startups
                            </Link>
                        }
                    />
                ) : (
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                        {items.map((s) => (
                            <Card key={s.id} hover className="flex flex-col gap-3">
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <h3 className="text-base font-semibold text-navy-800">
                                            {s.companyName}
                                        </h3>
                                        <div className="mt-2 flex flex-wrap gap-2">
                                            <Badge tone="brand">{s.industry}</Badge>
                                            {s.businessModel && <Badge tone="accent">{s.businessModel}</Badge>}
                                            <Badge tone="success">{s.startupStage}</Badge>
                                        </div>
                                    </div>
                                </div>

                                {s.description && (
                                    <p className="line-clamp-2 text-sm text-slate-600">
                                        {s.description}
                                    </p>
                                )}

                                <div className="grid grid-cols-3 gap-3 border-t border-slate-100 pt-3 text-sm">
                                    <div>
                                        <div className="text-xs text-slate-500">Funding</div>
                                        <div className="font-medium text-navy-800">{fmtMoney(s.totalFunding)}</div>
                                    </div>
                                    <div>
                                        <div className="text-xs text-slate-500">Revenue</div>
                                        <div className="font-medium text-navy-800">{fmtMoney(s.monthlyRevenue)}</div>
                                    </div>
                                    <div>
                                        <div className="text-xs text-slate-500">Team</div>
                                        <div className="font-medium text-navy-800">{s.teamSize ?? "—"}</div>
                                    </div>
                                </div>

                                <div className="flex items-center justify-between border-t border-slate-100 pt-3">
                                    <span className="inline-flex items-center gap-1 text-xs text-slate-400">
                                        <Clock className="h-3 w-3" />
                                        Saved {fmtDate(s.savedAt)}
                                    </span>
                                    <div className="flex gap-2">
                                        <Button
                                            variant="secondary"
                                            onClick={() => navigate(`/investor/discover`)}
                                        >
                                            View Details
                                        </Button>
                                        <Button
                                            variant="secondary"
                                            onClick={() => handleRemove(s)}
                                            disabled={busyId === s.startupId}
                                        >
                                            <Trash2 className="h-4 w-4" />
                                            Remove
                                        </Button>
                                    </div>
                                </div>
                            </Card>
                        ))}
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
}