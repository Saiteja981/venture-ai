import Badge from "../common/Badge.jsx";
import Button from "../common/Button.jsx";
import { X, ExternalLink, Send } from "lucide-react";

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US", {
        style: "currency", currency: "USD", maximumFractionDigits: 0,
    }).format(n);
}
function fmtNumber(v) {
    if (v === null || v === undefined) return "—";
    return new Intl.NumberFormat("en-US").format(Number(v));
}

export default function StartupDetailModal({ startup, onClose, onExpressInterest }) {
    if (!startup) return null;
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-navy-900/60" onClick={onClose} aria-hidden="true" />
            <div className="relative max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                    <div>
                        <h2 className="text-2xl font-semibold text-navy-800">
                            {startup.companyName}
                        </h2>
                        <div className="mt-2 flex flex-wrap gap-2">
                            <Badge tone="brand">{startup.industry}</Badge>
                            {startup.businessModel && <Badge tone="accent">{startup.businessModel}</Badge>}
                            <Badge tone="success">{startup.startupStage}</Badge>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100"
                        aria-label="Close"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {startup.description && (
                    <div className="mt-6">
                        <h3 className="text-sm font-semibold text-navy-800">About</h3>
                        <p className="mt-2 whitespace-pre-line text-sm text-slate-600">
                            {startup.description}
                        </p>
                    </div>
                )}

                <div className="mt-6">
                    <h3 className="text-sm font-semibold text-navy-800">Metrics</h3>
                    <dl className="mt-3 grid grid-cols-2 gap-4 sm:grid-cols-3">
                        <div><dt className="text-xs text-slate-500">Founded</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{startup.foundedYear ?? "—"}</dd></div>
                        <div><dt className="text-xs text-slate-500">Team Size</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{fmtNumber(startup.teamSize)}</dd></div>
                        <div><dt className="text-xs text-slate-500">Users</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{fmtNumber(startup.totalUsers)}</dd></div>
                        <div><dt className="text-xs text-slate-500">Customers</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{fmtNumber(startup.customerCount)}</dd></div>
                        <div><dt className="text-xs text-slate-500">Monthly Revenue</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{fmtMoney(startup.monthlyRevenue)}</dd></div>
                        <div><dt className="text-xs text-slate-500">Revenue Growth</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">
                                {startup.revenueGrowthRate ? `${startup.revenueGrowthRate}%` : "—"}</dd></div>
                        <div><dt className="text-xs text-slate-500">Total Funding</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{fmtMoney(startup.totalFunding)}</dd></div>
                        <div><dt className="text-xs text-slate-500">Runway</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">
                                {startup.runwayMonths ? `${startup.runwayMonths} mo` : "—"}</dd></div>
                        <div><dt className="text-xs text-slate-500">Valuation</dt>
                            <dd className="mt-1 text-sm font-medium text-navy-800">{fmtMoney(startup.valuation)}</dd></div>
                    </dl>
                </div>

                {startup.website && (
                    <div className="mt-6">
                        <h3 className="text-sm font-semibold text-navy-800">Website</h3>
                        <a
                            href={startup.website}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-1 inline-flex items-center gap-1 text-sm text-brand-600 hover:text-brand-700"
                        >
                            {startup.website} <ExternalLink className="h-3.5 w-3.5" />
                        </a>
                    </div>
                )}

                <div className="mt-8 flex flex-wrap justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>Close</Button>
                    {onExpressInterest && (
                        <Button onClick={() => onExpressInterest(startup)}>
                            <Send className="h-4 w-4" />
                            Express Interest
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}