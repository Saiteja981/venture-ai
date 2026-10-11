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
import { listInvestors, getInvestor } from "../../services/admin/adminService.js";
import { Building2, Search, ChevronLeft, ChevronRight, X } from "lucide-react";

const PAGE_SIZE = 20;
const INVESTOR_TYPES = ["VC_FIRM", "ANGEL", "CORPORATE_VC", "ACCELERATOR", "FAMILY_OFFICE"];
const RISK_LEVELS = ["LOW", "MEDIUM", "HIGH"];

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

export default function AdminInvestors() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);
    const [search, setSearch] = useState("");
    const [investorType, setInvestorType] = useState("");
    const [riskPreference, setRiskPreference] = useState("");
    const [page, setPage] = useState(0);

    const [detail, setDetail] = useState(null);

    const debounceRef = useRef(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const result = await listInvestors({
                page, size: PAGE_SIZE, search, investorType, riskPreference,
            });
            setData(result);
        } catch (err) {
            setError(messageForError(err) || "Could not load investors.");
        } finally {
            setLoading(false);
        }
    }, [page, search, investorType, riskPreference]);

    useEffect(() => { load(); }, [load]);

    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => setPage(0), 350);
        return () => clearTimeout(debounceRef.current);
    }, [search, investorType, riskPreference]);

    async function openDetail(id) {
        try {
            const d = await getInvestor(id);
            setDetail(d);
        } catch (err) {
            setError(messageForError(err));
        }
    }

    return (
        <DashboardLayout title="Investors">
            <div className="space-y-6">
                <PageHeader
                    title="Investor Management"
                    description="Browse and inspect registered investor profiles."
                />

                <Card>
                    <div className="flex flex-col gap-3 sm:flex-row">
                        <div className="relative flex-1">
                            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                            <input
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                placeholder="Search by name or organization…"
                                className="input-field pl-9"
                            />
                        </div>
                        <select
                            value={investorType}
                            onChange={(e) => setInvestorType(e.target.value)}
                            className="input-field sm:w-44"
                        >
                            <option value="">All types</option>
                            {INVESTOR_TYPES.map((t) => (
                                <option key={t} value={t}>{t}</option>
                            ))}
                        </select>
                        <select
                            value={riskPreference}
                            onChange={(e) => setRiskPreference(e.target.value)}
                            className="input-field sm:w-36"
                        >
                            <option value="">All risk</option>
                            {RISK_LEVELS.map((r) => (
                                <option key={r} value={r}>{r}</option>
                            ))}
                        </select>
                    </div>
                </Card>

                {loading ? (
                    <LoadingState message="Loading investors…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load investors"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !data || data.content.length === 0 ? (
                    <EmptyState
                        icon={Building2}
                        title="No investors found"
                        description="Try adjusting your search or filters."
                    />
                ) : (
                    <>
                        <Card className="!p-0 overflow-hidden">
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                                    <tr>
                                        <th className="px-4 py-3">Investor</th>
                                        <th className="px-4 py-3">Organization</th>
                                        <th className="px-4 py-3">Type</th>
                                        <th className="px-4 py-3">Risk</th>
                                        <th className="px-4 py-3 text-right">Min Investment</th>
                                        <th className="px-4 py-3 text-right">Max Investment</th>
                                        <th className="px-4 py-3 text-right">Portfolio</th>
                                        <th className="px-4 py-3">Created</th>
                                        <th className="px-4 py-3 text-right">Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {data.content.map((i) => (
                                        <tr key={i.id}>
                                            <td className="px-4 py-3 font-medium text-navy-800">
                                                {i.investorName}
                                            </td>
                                            <td className="px-4 py-3 text-slate-600">
                                                {i.organizationName ?? "—"}
                                            </td>
                                            <td className="px-4 py-3">
                                                {i.investorType ? (
                                                    <Badge tone="brand">{i.investorType}</Badge>
                                                ) : "—"}
                                            </td>
                                            <td className="px-4 py-3">
                                                {i.riskPreference ? (
                                                    <Badge
                                                        tone={
                                                            i.riskPreference === "HIGH"
                                                                ? "danger"
                                                                : i.riskPreference === "MEDIUM"
                                                                    ? "warning"
                                                                    : "success"
                                                        }
                                                    >
                                                        {i.riskPreference}
                                                    </Badge>
                                                ) : "—"}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {fmtMoney(i.minimumInvestment)}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {fmtMoney(i.maximumInvestment)}
                                            </td>
                                            <td className="px-4 py-3 text-right text-slate-600">
                                                {i.portfolioSize ?? "—"}
                                            </td>
                                            <td className="px-4 py-3 text-slate-500">
                                                {fmtDate(i.createdAt)}
                                            </td>
                                            <td className="px-4 py-3 text-right">
                                                <Button
                                                    variant="secondary"
                                                    onClick={() => openDetail(i.id)}
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
                <Modal open onClose={() => setDetail(null)} title={detail.investorName}>
                    <div className="space-y-4">
                        {detail.bio && (
                            <p className="text-sm text-slate-600">{detail.bio}</p>
                        )}

                        <div className="flex flex-wrap gap-2">
                            {detail.investorType && <Badge tone="brand">{detail.investorType}</Badge>}
                            {detail.riskPreference && (
                                <Badge
                                    tone={
                                        detail.riskPreference === "HIGH"
                                            ? "danger"
                                            : detail.riskPreference === "MEDIUM"
                                                ? "warning"
                                                : "success"
                                    }
                                >
                                    {detail.riskPreference} risk
                                </Badge>
                            )}
                        </div>

                        <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                            <div>
                                <dt className="text-xs text-slate-500">Organization</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.organizationName ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Experience</dt>
                                <dd className="mt-1 text-sm text-navy-800">
                                    {detail.investmentExperienceYears != null
                                        ? `${detail.investmentExperienceYears} yrs`
                                        : "—"}
                                </dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Portfolio Size</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.portfolioSize ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Min Investment</dt>
                                <dd className="mt-1 text-sm text-navy-800">{fmtMoney(detail.minimumInvestment)}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Max Investment</dt>
                                <dd className="mt-1 text-sm text-navy-800">{fmtMoney(detail.maximumInvestment)}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Location</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.preferredLocation ?? "—"}</dd>
                            </div>
                        </dl>

                        <div className="grid grid-cols-1 gap-3 border-t border-slate-100 pt-4">
                            <div>
                                <dt className="text-xs text-slate-500">Preferred Industries</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.preferredIndustries ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Preferred Stages</dt>
                                <dd className="mt-1 text-sm text-navy-800">{detail.preferredStages ?? "—"}</dd>
                            </div>
                            <div>
                                <dt className="text-xs text-slate-500">Preferred Business Models</dt>
                                <dd className="mt-1 text-sm text-navy-800">
                                    {detail.preferredBusinessModels ?? "—"}
                                </dd>
                            </div>
                        </div>

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