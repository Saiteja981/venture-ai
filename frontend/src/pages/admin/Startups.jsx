import { useCallback, useEffect, useRef, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import Modal from "../../components/common/Modal.jsx";
import { messageForError } from "../../utils/errorHandler.js";
import { listStartups, getStartup } from "../../services/admin/adminService.js";
import { Rocket, Search, ChevronLeft, ChevronRight, X } from "lucide-react";

const PAGE_SIZE = 20;
const STAGES = ["PRE_SEED", "SEED", "SERIES_A", "SERIES_B", "SERIES_C"];

function fmtMoney(v) {
    if (v == null) return "—";
    const n = Number(v);
    if (Number.isNaN(n)) return "—";
    if (n >= 1_000_000) return `$${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `$${(n / 1_000).toFixed(0)}K`;
    return `$${n}`;
}

function fmtDate(s) {
    if (!s) return "";
    const d = new Date(s);
    return Number.isNaN(d.getTime())
        ? s
        : d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "2-digit" });
}

export default function AdminStartups() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);
    const [search, setSearch] = useState("");
    const [industry, setIndustry] = useState("");
    const [stage, setStage] = useState("");
    const [page, setPage] = useState(0);

    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);

    const debounceRef = useRef(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const result = await listStartups({
                page, size: PAGE_SIZE, search, industry, stage,
            });
            setData(result);
        } catch (err) {
            setError(messageForError(err) || "Could not load startups.");
        } finally {
            setLoading(false);
        }
    }, [page, search, industry, stage]);

    useEffect(() => { load(); }, [load]);

    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => setPage(0), 350);
        return () => clearTimeout(debounceRef.current);
    }, [search, industry, stage]);

    async function openDetail(id) {
        setDetailLoading(true);
        try {
            const d = await getStartup(id);
            setDetail(d);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setDetailLoading(false);
        }
    }

    return (
        <DashboardLayout title="Startups">
            <div className="space-y-6">
                <PageHeader
                    title="Startup Management"
                    description="Browse and inspect registered startup profiles."
                />

                <Card>
                    <div className="flex flex-col gap-3 sm:flex-row">
                        <div className="relative flex-1">
                            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                            <input
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                placeholder="Search by company name or description…"
                                className="input-field pl-9"
                            />
                        </div>
                        <input
                            value={industry}
                            onChange={(e) => setIndustry(e.target.value)}
                            placeholder="Industry"
                            className="input-field sm:w-40"
                        />
                        <select
                            value={stage}
                            onChange={(e) => setStage(e.target.value)}
                            className="input-field sm:w-40"
                        >
                            <option value="">All stages</option>
                            {STAGES.map((s) => (
                                <option key={s} value={s}>{s}</option>
                            ))}
                        </select>
                    </div>
                </Card>

                {loading ? (
                    <LoadingState message="Loading startups…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load startups"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !data || data.content.length === 0 ? (
                    <EmptyState
                        icon={Rocket}
                        title="No startups found"
                        description="Try adjusting your search or filters."
                    />
                ) : (
                    <>
                        <Card className="!p-0 overflow-hidden">
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                                    <tr>
                                        <th className="px-4 py-3">Company</th>
                                        <th className="px-4 py-3">Industry</th>
                                        <th className="px-4 py-3">Stage</th>
                                        <th className="px-4 py-3 text-right">Team</th>
                                        <th className="px-4 py-3 text-right">Funding</th>
                                        <th className="px-4 py-3 text-right">Valuation</th>
                                        <th className="px-4 py-3">Created</th>
                                        <th className="px-4 py-3 text-right">Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {data.content.map((s) => (
                                        <tr key={s.id}>
                                            <td className="px-4 py-3 font-medium text-navy-800">
                                                {s.companyName}
                                            </td>
                                            <td className="px-4 py-3">
                                                {s.industry ? (
                                                    <Badge tone="brand">{s.industry}</Badge>
                                                ) : "—"}
                                            </td>
                                            <td className="px-4 py-3">
                                                {s.startupStage ? (
                                                    <Badge tone="success">{s.startupStage}</Badge>
                                                ) : "—"}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {s.teamSize ?? "—"}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {fmtMoney(s.totalFunding)}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {fmtMoney(s.valuation)}
                                            </td>
                                            <td className="px-4 py-3 text-slate-500">
                                                {fmtDate(s.createdAt)}
                                            </td>
                                            <td className="px-4 py-3 text-right">
                                                <Button
                                                    variant="secondary"
                                                    onClick={() => openDetail(s.id)}
                                                >
                                                    Details
                                                </Button>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
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
                                    Page {data.currentPage + 1} of {data.totalPages || 1} ·
                                    {" "}{data.totalElements} total
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

            {detail && (
                <Modal open onClose={() => setDetail(null)} title={detail.companyName}>
                    <div className="space-y-4">
                        <p className="text-sm text-slate-600">{detail.description}</p>

                        <div className="flex flex-wrap gap-2">
                            {detail.industry && <Badge tone="brand">{detail.industry}</Badge>}
                            {detail.startupStage && <Badge tone="success">{detail.startupStage}</Badge>}
                            {detail.businessModel && <Badge tone="accent">{detail.businessModel}</Badge>}
                        </div>

                        <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                            <div>
                                <dt className="text-xs text-slate-500">Team Size</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.teamSize ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Total Users</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.totalUsers ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Customers</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.customerCount ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Monthly Revenue</dt>
                                <dd className="mt-1 text-sm text-navy-800">{fmtMoney(detail.monthlyRevenue)}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Growth Rate</dt>
                                <dd className="mt-1 text-sm text-navy-800">
                                    {detail.revenueGrowthRate != null
                                        ? `${detail.revenueGrowthRate}%`
                                        : "—"}
                                </dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Total Funding</dt>
                                <dd className="mt-1 text-sm text-navy-800">{fmtMoney(detail.totalFunding)}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Runway</dt>
                                <dd className="mt-1 text-sm text-navy-800">
                                    {detail.runwayMonths != null ? `${detail.runwayMonths} mo` : "—"}
                                </dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Valuation</dt>
                                <dd className="mt-1 text-sm text-navy-800">{fmtMoney(detail.valuation)}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Website</dt>
                                <dd className="mt-1 text-sm text-navy-800">
                                    {detail.website ? (
                                        <a
                                            href={detail.website}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="text-brand-600 hover:underline"
                                        >
                                            {detail.website}
                                        </a>
                                    ) : "—"}
                                </dd>
                            </div>
                        </dl>

                        <div className="border-t border-slate-100 pt-4">
                            <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                                Owner
                            </h4>
                            <p className="mt-1 text-sm text-navy-800">
                                {detail.ownerName} · {detail.ownerEmail}
                            </p>
                        </div>

                        <div className="flex justify-end pt-2">
                            <Button onClick={() => setDetail(null)}>
                                <X className="h-4 w-4" /> Close
                            </Button>
                        </div>
                    </div>
                </Modal>
            )}
        </DashboardLayout>
    );
}