import Card from "../common/Card.jsx";
import Badge from "../common/Badge.jsx";
import Button from "../common/Button.jsx";
import {
    ArrowRight, Users, TrendingUp, Landmark, Handshake,
    Bookmark, BookmarkCheck,
} from "lucide-react";

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    if (n >= 1_000_000) return `$${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `$${(n / 1_000).toFixed(0)}K`;
    return `$${n}`;
}

export default function StartupCard({
                                        startup,
                                        onViewDetails,
                                        onExpressInterest,
                                        requestStatus,
                                        isSaved,
                                        onToggleSave,
                                        savingSave,
                                    }) {
    const hasActiveRequest = requestStatus === "PENDING" || requestStatus === "ACCEPTED";

    return (
        <Card hover className="flex flex-col gap-4">
            <div>
                <div className="flex flex-wrap items-start justify-between gap-3">
                    <h3 className="text-base font-semibold text-navy-800">
                        {startup.companyName}
                    </h3>
                    <div className="flex flex-wrap items-center gap-2">
                        {onToggleSave && (
                            <button
                                type="button"
                                onClick={() => onToggleSave(startup)}
                                disabled={savingSave}
                                className={`rounded-md p-1.5 ${
                                    isSaved
                                        ? "text-brand-600 hover:bg-brand-50"
                                        : "text-slate-400 hover:bg-slate-100"
                                }`}
                                aria-label={isSaved ? "Remove bookmark" : "Save startup"}
                            >
                                {isSaved ? (
                                    <BookmarkCheck className="h-4 w-4" />
                                ) : (
                                    <Bookmark className="h-4 w-4" />
                                )}
                            </button>
                        )}
                        <Badge tone="success">{startup.startupStage}</Badge>
                        {requestStatus === "PENDING" && (
                            <Badge tone="warning">Request Pending</Badge>
                        )}
                        {requestStatus === "ACCEPTED" && (
                            <Badge tone="brand">Collaborating</Badge>
                        )}
                    </div>
                </div>
                <div className="mt-2 flex flex-wrap gap-2">
                    <Badge tone="brand">{startup.industry}</Badge>
                    {startup.businessModel && (
                        <Badge tone="accent">{startup.businessModel}</Badge>
                    )}
                </div>
            </div>

            {startup.description && (
                <p className="line-clamp-2 text-sm text-slate-600">
                    {startup.description}
                </p>
            )}

            <div className="grid grid-cols-3 gap-3 border-t border-slate-100 pt-3">
                <div>
                    <div className="flex items-center gap-1 text-xs text-slate-500">
                        <Landmark className="h-3 w-3" /> Funding
                    </div>
                    <div className="mt-0.5 text-sm font-medium text-navy-800">
                        {fmtMoney(startup.totalFunding)}
                    </div>
                </div>
                <div>
                    <div className="flex items-center gap-1 text-xs text-slate-500">
                        <TrendingUp className="h-3 w-3" /> Revenue
                    </div>
                    <div className="mt-0.5 text-sm font-medium text-navy-800">
                        {fmtMoney(startup.monthlyRevenue)}
                    </div>
                </div>
                <div>
                    <div className="flex items-center gap-1 text-xs text-slate-500">
                        <Users className="h-3 w-3" /> Team
                    </div>
                    <div className="mt-0.5 text-sm font-medium text-navy-800">
                        {startup.teamSize ?? "—"}
                    </div>
                </div>
            </div>

            <div className="flex flex-wrap justify-end gap-2">
                <Button variant="secondary" onClick={() => onViewDetails(startup)}>
                    View Details
                    <ArrowRight className="h-4 w-4" />
                </Button>
                {onExpressInterest && !hasActiveRequest && (
                    <Button onClick={() => onExpressInterest(startup)}>
                        <Handshake className="h-4 w-4" />
                        Express Interest
                    </Button>
                )}
            </div>
        </Card>
    );
}