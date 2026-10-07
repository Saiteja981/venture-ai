import Card from "../common/Card.jsx";
import Badge from "../common/Badge.jsx";
import Button from "../common/Button.jsx";
import { Pencil, Trash2, ExternalLink } from "lucide-react";

function fmtMoney(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: 0,
    }).format(n);
}
function fmtNumber(v) {
    if (v === null || v === undefined) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    return new Intl.NumberFormat("en-US").format(n);
}
function fmtPercent(v) {
    if (v === null || v === undefined) return "—";
    return `${Number(v).toFixed(2)}%`;
}

export default function StartupProfileView({ startup, onEdit, onDelete }) {
    return (
        <div className="space-y-6">
            <Card>
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                        <h1 className="text-2xl font-semibold text-navy-800">
                            {startup.companyName}
                        </h1>
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                            <Badge tone="brand">{startup.industry}</Badge>
                            <Badge tone="accent">{startup.businessModel}</Badge>
                            <Badge tone="success">{startup.startupStage}</Badge>
                        </div>
                    </div>
                    <div className="flex gap-2">
                        <Button variant="secondary" onClick={onEdit}>
                            <Pencil className="h-4 w-4" />
                            Edit Profile
                        </Button>
                        <Button variant="danger" onClick={onDelete}>
                            <Trash2 className="h-4 w-4" />
                            Delete Profile
                        </Button>
                    </div>
                </div>
            </Card>

            {startup.description && (
                <Card>
                    <h3 className="text-base font-semibold text-navy-800">About</h3>
                    <p className="mt-3 whitespace-pre-line text-sm text-slate-600">
                        {startup.description}
                    </p>
                </Card>
            )}

            <Card>
                <h3 className="text-base font-semibold text-navy-800">Company Metrics</h3>
                <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
                    <div>
                        <dt className="text-xs text-slate-500">Team Size</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtNumber(startup.teamSize)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Customers</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtNumber(startup.customerCount)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Users</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtNumber(startup.totalUsers)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Founded</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {startup.foundedYear ?? "—"}
                        </dd>
                    </div>
                </dl>
            </Card>

            <Card>
                <h3 className="text-base font-semibold text-navy-800">Financial Metrics</h3>
                <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
                    <div>
                        <dt className="text-xs text-slate-500">Monthly Revenue</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtMoney(startup.monthlyRevenue)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Revenue Growth</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtPercent(startup.revenueGrowthRate)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Total Funding</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtMoney(startup.totalFunding)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Monthly Burn</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtMoney(startup.monthlyBurnRate)}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Runway</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {startup.runwayMonths ? `${startup.runwayMonths} mo` : "—"}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-xs text-slate-500">Valuation</dt>
                        <dd className="mt-1 text-sm font-medium text-navy-800">
                            {fmtMoney(startup.valuation)}
                        </dd>
                    </div>
                </dl>
            </Card>

            {(startup.website || startup.pitchDeckUrl) && (
                <Card>
                    <h3 className="text-base font-semibold text-navy-800">Links</h3>
                    <ul className="mt-3 space-y-2 text-sm">
                        {startup.website && (
                            <li>
                                <a
                                    href={startup.website}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="inline-flex items-center gap-1 text-brand-600 hover:text-brand-700"
                                >
                                    Website <ExternalLink className="h-3.5 w-3.5" />
                                </a>
                            </li>
                        )}
                        {startup.pitchDeckUrl && (
                            <li>
                                <a
                                    href={startup.pitchDeckUrl}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="inline-flex items-center gap-1 text-brand-600 hover:text-brand-700"
                                >
                                    Pitch Deck <ExternalLink className="h-3.5 w-3.5" />
                                </a>
                            </li>
                        )}
                    </ul>
                </Card>
            )}
        </div>
    );
}