import { useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import Button from "../../components/common/Button.jsx";
import Badge from "../../components/common/Badge.jsx";
import { getModelInfo } from "../../services/analytics/analyticsService.js";
import { messageForError } from "../../utils/errorHandler.js";
import { Brain, Sparkles, FileText, Info } from "lucide-react";

/**
 * Renders a flat object of key/value pairs as a definition list.
 * Numbers are formatted; strings are rendered verbatim; objects are
 * stringified to avoid React errors.
 */
function MetricTable({ obj, emptyText = "No metrics recorded." }) {
    const entries = Object.entries(obj || {}).filter(
        ([, v]) => v !== null && v !== undefined && v !== ""
    );
    if (entries.length === 0) {
        return <p className="text-sm text-slate-500">{emptyText}</p>;
    }
    return (
        <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {entries.map(([key, value]) => (
                <div
                    key={key}
                    className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2"
                >
                    <dt className="text-xs text-slate-500">{key}</dt>
                    <dd className="mt-0.5 text-sm font-medium text-navy-800 break-words">
                        {typeof value === "number"
                            ? Number.isInteger(value)
                                ? value
                                : value.toFixed(4)
                            : typeof value === "object"
                                ? JSON.stringify(value)
                                : String(value)}
                    </dd>
                </div>
            ))}
        </dl>
    );
}

function ModelSection({ icon: Icon, title, subtitle, metadata, evaluation, history }) {
    return (
        <Card>
            <div className="flex items-start gap-3">
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                    <Icon className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                    <h3 className="text-base font-semibold text-navy-800">{title}</h3>
                    <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>
                </div>
            </div>

            <div className="mt-5 space-y-5">
                <div>
                    <h4 className="text-sm font-semibold text-navy-800">Metadata</h4>
                    <div className="mt-2">
                        <MetricTable obj={metadata} />
                    </div>
                </div>

                <div>
                    <h4 className="text-sm font-semibold text-navy-800">
                        Evaluation Metrics
                    </h4>
                    <div className="mt-2">
                        <MetricTable obj={evaluation} />
                    </div>
                </div>

                {history && Object.keys(history).length > 0 && (
                    <div>
                        <h4 className="text-sm font-semibold text-navy-800">
                            Training History
                        </h4>
                        <div className="mt-2">
                            <MetricTable obj={history} />
                        </div>
                    </div>
                )}
            </div>
        </Card>
    );
}

export default function AIAnalytics() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [info, setInfo] = useState(null);

    async function load() {
        setLoading(true);
        setError("");
        try {
            const data = await getModelInfo();
            setInfo(data);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    if (loading) {
        return (
            <DashboardLayout title="AI Analytics">
                <LoadingState message="Loading model information…" />
            </DashboardLayout>
        );
    }

    if (error) {
        return (
            <DashboardLayout title="AI Analytics">
                <ErrorState
                    title="Could not load model information"
                    message={error}
                    action={<Button onClick={load}>Retry</Button>}
                />
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout title="AI Analytics">
            <div className="space-y-6">
                <PageHeader
                    title="AI Analytics"
                    description="Model metadata and evaluation metrics persisted during training."
                />

                {/* Synthetic data notice */}
                <Card className="border-amber-200 bg-amber-50">
                    <div className="flex items-start gap-3">
                        <Info className="mt-0.5 h-5 w-5 flex-shrink-0 text-amber-600" />
                        <div>
                            <h3 className="text-sm font-semibold text-amber-900">
                                Important — Synthetic Data
                            </h3>
                            <p className="mt-1 text-sm text-amber-800">
                                {info?.note ||
                                    "All metrics shown are training/evaluation metrics on synthetic development data. They do not represent real-world investment performance."}
                            </p>
                        </div>
                    </div>
                </Card>

                {/* Startup Potential / Risk */}
                <ModelSection
                    icon={Brain}
                    title="Startup Potential & Risk Model"
                    subtitle="MLP classifier for potential score and risk category"
                    metadata={info?.startup_prediction?.metadata}
                    evaluation={info?.startup_prediction?.evaluation}
                    history={info?.startup_prediction?.training_history}
                />

                {/* Two-Tower */}
                <ModelSection
                    icon={Sparkles}
                    title="Two-Tower Recommendation Model"
                    subtitle="Investor → startup compatibility ranking"
                    metadata={info?.two_tower_recommender?.metadata}
                    evaluation={info?.two_tower_recommender?.evaluation}
                    history={info?.two_tower_recommender?.training_history}
                />

                {/* BERT Pitch Analyzer */}
                <ModelSection
                    icon={FileText}
                    title="BERT Pitch Deck Analyzer"
                    subtitle="Section completeness and information gap detection"
                    metadata={info?.pitch_analyzer?.metadata}
                    evaluation={info?.pitch_analyzer?.evaluation}
                />

                {info?.pitch_analyzer?.labels &&
                    Object.keys(info.pitch_analyzer.labels).length > 0 && (
                        <Card>
                            <h3 className="text-base font-semibold text-navy-800">
                                Pitch Analyzer — Section Labels
                            </h3>
                            <div className="mt-3 flex flex-wrap gap-2">
                                {Object.entries(info.pitch_analyzer.labels).map(
                                    ([key, label]) => (
                                        <Badge key={key} tone="navy">
                                            {key}: {String(label)}
                                        </Badge>
                                    )
                                )}
                            </div>
                        </Card>
                    )}
            </div>
        </DashboardLayout>
    );
}