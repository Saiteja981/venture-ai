import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import StartupCard from "../../components/discovery/StartupCard.jsx";
import FilterPanel from "../../components/discovery/FilterPanel.jsx";
import StartupDetailModal from "../../components/discovery/StartupDetailModal.jsx";
import ExpressInterestModal from "../../components/collaboration/ExpressInterestModal.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { discoverStartups } from "../../services/startup/startupService.js";
import {
    createCollaborationRequest,
    getInvestorCollaborations,
} from "../../services/collaboration/collaborationService.js";
import {
    saveStartup as saveStartupApi,
    getSavedStartups,
    removeSavedStartup,
} from "../../services/investor/investorService.js";
import { Search, X, ChevronLeft, ChevronRight, Compass } from "lucide-react";

const PAGE_SIZE = 12;
const SORT_OPTIONS = [
    { value: "newest", label: "Newest" },
    { value: "funding-high", label: "Funding: High to Low" },
    { value: "revenue-high", label: "Revenue: High to Low" },
    { value: "growth-high", label: "Growth: High to Low" },
    { value: "valuation-high", label: "Valuation: High to Low" },
    { value: "team-high", label: "Team Size: High to Low" },
];

export default function Discover() {
    const toast = useToast();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);

    const [searchInput, setSearchInput] = useState("");
    const [search, setSearch] = useState("");
    const [filters, setFilters] = useState({});
    const [sort, setSort] = useState("newest");
    const [page, setPage] = useState(0);

    const [selectedStartup, setSelectedStartup] = useState(null);

    // Collaboration state
    const [interestStartup, setInterestStartup] = useState(null);
    const [submittingInterest, setSubmittingInterest] = useState(false);
    const [interestError, setInterestError] = useState("");
    const [collaborationsByStartup, setCollaborationsByStartup] = useState({});

    // Saved startups state
    const [savedIds, setSavedIds] = useState(new Set());
    const [savingId, setSavingId] = useState(null);

    // Debounce search input
    const debounceRef = useRef(null);
    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
            setSearch(searchInput.trim());
            setPage(0);
        }, 350);
        return () => clearTimeout(debounceRef.current);
    }, [searchInput]);

    const queryParams = useMemo(() => ({
        ...(search ? { search } : {}),
        ...Object.fromEntries(
            Object.entries(filters).filter(([, v]) => v !== "" && v !== null && v !== undefined)
        ),
        sort,
        page,
        size: PAGE_SIZE,
    }), [search, filters, sort, page]);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const result = await discoverStartups(queryParams);
            setData(result);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, [queryParams]);

    useEffect(() => { load(); }, [load]);

    // Load existing collaborations for status badges
    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const collabs = await getInvestorCollaborations();
                if (cancelled) return;
                const map = {};
                collabs.forEach((c) => { map[c.startupId] = c.status; });
                setCollaborationsByStartup(map);
            } catch {
                // Non-critical
            }
        })();
        return () => { cancelled = true; };
    }, []);

    // Load saved startups
    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const list = await getSavedStartups();
                if (cancelled) return;
                setSavedIds(new Set(list.map((s) => s.startupId)));
            } catch {
                // Non-critical
            }
        })();
        return () => { cancelled = true; };
    }, []);

    async function handleSendInterest(message) {
        if (!interestStartup) return;
        setSubmittingInterest(true);
        setInterestError("");
        try {
            const created = await createCollaborationRequest(interestStartup.id, message);
            setCollaborationsByStartup((prev) => ({
                ...prev,
                [created.startupId]: created.status,
            }));
            setInterestStartup(null);
            toast.success("Collaboration request sent.");
        } catch (err) {
            setInterestError(messageForError(err));
        } finally {
            setSubmittingInterest(false);
        }
    }

    async function handleToggleSave(startup) {
        setSavingId(startup.id);
        try {
            if (savedIds.has(startup.id)) {
                await removeSavedStartup(startup.id);
                setSavedIds((prev) => {
                    const next = new Set(prev);
                    next.delete(startup.id);
                    return next;
                });
                toast.success("Removed from saved startups.");
            } else {
                await saveStartupApi(startup.id);
                setSavedIds((prev) => new Set(prev).add(startup.id));
                toast.success("Startup saved.");
            }
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setSavingId(null);
        }
    }

    function openInterest(startup) {
        setInterestError("");
        setSelectedStartup(null);
        setInterestStartup(startup);
    }

    function applyFilters(next) {
        setFilters(next);
        setPage(0);
    }

    function clearFilters() {
        setFilters({});
        setPage(0);
    }

    function clearSearch() {
        setSearchInput("");
        setSearch("");
        setPage(0);
    }

    const hasResults = data && data.content && data.content.length > 0;
    const totalPages = data?.totalPages ?? 0;

    return (
        <DashboardLayout title="Discover Startups">
            <div className="space-y-6">
                <PageHeader
                    title="Discover Startups"
                    description="Browse and filter startups on the platform."
                    actions={
                        <select
                            value={sort}
                            onChange={(e) => { setSort(e.target.value); setPage(0); }}
                            className="input-field text-sm"
                        >
                            {SORT_OPTIONS.map((o) => (
                                <option key={o.value} value={o.value}>{o.label}</option>
                            ))}
                        </select>
                    }
                />

                {/* Search bar */}
                <Card>
                    <form onSubmit={(e) => { e.preventDefault(); setSearch(searchInput.trim()); setPage(0); }}
                          className="flex gap-2">
                        <div className="relative flex-1">
                            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                            <input
                                type="text"
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                placeholder="Search startups by name, description, or industry…"
                                className="input-field pl-9"
                            />
                            {searchInput && (
                                <button
                                    type="button"
                                    onClick={clearSearch}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-400 hover:bg-slate-100"
                                    aria-label="Clear search"
                                >
                                    <X className="h-4 w-4" />
                                </button>
                            )}
                        </div>
                    </form>
                </Card>

                <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
                    {/* Filter sidebar */}
                    <div className="lg:col-span-1">
                        <FilterPanel onApply={applyFilters} onClear={clearFilters} disabled={loading} />
                    </div>

                    {/* Results */}
                    <div className="space-y-4 lg:col-span-3">
                        {loading ? (
                            <LoadingState message="Loading startups…" />
                        ) : error ? (
                            <ErrorState
                                title="Could not load startups"
                                message={error}
                                action={<Button onClick={load}>Retry</Button>}
                            />
                        ) : !hasResults ? (
                            <EmptyState
                                icon={Compass}
                                title="No startups found"
                                description="Try adjusting your search or clearing filters."
                                action={<Button variant="secondary" onClick={() => { clearFilters(); clearSearch(); }}>Clear filters</Button>}
                            />
                        ) : (
                            <>
                                <Card>
                                    <div className="flex items-center justify-between">
                                        <p className="text-sm text-slate-600">
                                            <span className="font-medium text-navy-800">{data.totalElements}</span>{" "}
                                            startup{data.totalElements === 1 ? "" : "s"} found
                                        </p>
                                        {(search || Object.keys(filters).length > 0) && (
                                            <Button variant="secondary"
                                                    onClick={() => { clearFilters(); clearSearch(); }}>
                                                Clear filters
                                            </Button>
                                        )}
                                    </div>
                                </Card>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                    {data.content.map((s) => (
                                        <StartupCard
                                            key={s.id}
                                            startup={s}
                                            onViewDetails={setSelectedStartup}
                                            onExpressInterest={openInterest}
                                            requestStatus={collaborationsByStartup[s.id] || null}
                                            isSaved={savedIds.has(s.id)}
                                            onToggleSave={handleToggleSave}
                                            savingSave={savingId === s.id}
                                        />
                                    ))}
                                </div>

                                {totalPages > 1 && (
                                    <Card>
                                        <div className="flex items-center justify-between">
                                            <Button
                                                variant="secondary"
                                                disabled={data.first || loading}
                                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                            >
                                                <ChevronLeft className="h-4 w-4" /> Previous
                                            </Button>
                                            <span className="text-sm text-slate-600">
                                                Page {data.currentPage + 1} of {totalPages}
                                            </span>
                                            <Button
                                                variant="secondary"
                                                disabled={data.last || loading}
                                                onClick={() => setPage((p) => p + 1)}
                                            >
                                                Next <ChevronRight className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    </Card>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>

            {selectedStartup && (
                <StartupDetailModal
                    startup={selectedStartup}
                    onClose={() => setSelectedStartup(null)}
                    onExpressInterest={openInterest}
                />
            )}

            {interestStartup && (
                <ExpressInterestModal
                    startup={interestStartup}
                    submitting={submittingInterest}
                    error={interestError}
                    onSubmit={handleSendInterest}
                    onClose={() => { if (!submittingInterest) setInterestStartup(null); }}
                />
            )}
        </DashboardLayout>
    );
}