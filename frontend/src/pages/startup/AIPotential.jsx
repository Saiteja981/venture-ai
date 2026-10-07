import { useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import AILoadingState from "../../components/ai/AILoadingState.jsx";
import AIErrorState from "../../components/ai/AIErrorState.jsx";
import ScoreCard from "../../components/ai/ScoreCard.jsx";
import RiskBadge from "../../components/ai/RiskBadge.jsx";
import AIPageHeader from "../../components/ai/AIPageHeader.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useStartupProfile } from "../../hooks/useStartupProfile.js";
import { messageForError } from "../../utils/errorHandler.js";
import { deriveStartupFeatures } from "../../utils/aiDerivedFeatures.js";
import { predictStartup } from "../../services/api/aiApi.js";
import { Brain, Sparkles, Rocket } from "lucide-react";

export default function AIPotential() {
    const toast = useToast();
    const { loading, error, startup, reload } = useStartupProfile();

    const [predicting, setPredicting] = useState(false);
    const [prediction, setPrediction] = useState(null);
    const [predictError, setPredictError] = useState("");

    async function handleAnalyze() {
        if (!startup) return;
        setPredicting(true);
        setPredictError("");
        try {
            const payload = deriveStartupFeatures(startup);
            const result = await predictStartup(payload);
            setPrediction(result);
        } catch (err) {
            const msg =
                err?.isNetworkError || err?.isTimeout
                    ? "AI service is currently unavailable. Please try again later."
                    : messageForError(err) || "Prediction failed.";
            setPredictError(msg);
            toast.error(msg);
        } finally {
            setPredicting(false);
        }
    }

    return (
        <DashboardLayout title="AI Potential & Risk">
            <div className="space-y-6">
                <AIPageHeader
                    icon={Brain}
                    title="AI Potential & Risk"
                    description="Analyze your startup using Venture AI's deep-learning prediction model. Scores are estimates based on your startup profile."
                />

                {loading ? (
                    <LoadingState message="Loading your startup profile…" />
                ) : error ? (
                    <AIErrorState message={error} onRetry={reload} />
                ) : !startup ? (
                    <EmptyState
                        icon={Rocket}
                        title="No startup profile yet"
                        description="Create your startup profile before running AI analysis."
                        action={
                            <Link to="/startup/profile" className="btn-primary">
                                Create Startup Profile
                            </Link>
                        }
                    />
                ) : (
                    <>
                        <Card>
                            <div className="flex items-center justify-between">
                                <div>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        {startup.companyName}
                                    </h3>
                                    <p className="mt-1 text-xs text-slate-500">
                                        The following profile will be sent to the model.
                                    </p>
                                </div>
                                <Button
                                    onClick={handleAnalyze}
                                    disabled={predicting}
                                >
                                    {predicting ? "Analyzing…" : "Analyze Startup"}
                                </Button>
                            </div>

                            <dl className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
                                <div>
                                    <dt className="text-xs text-slate-500">Industry</dt>
                                    <dd className="mt-1 text-sm text-navy-800">{startup.industry}</dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Stage</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.startupStage}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Team Size</dt>
                                    <dd className="mt-1 text-sm text-navy-800">{startup.teamSize}</dd>
                                </div>
                                <div>
                                    <dt className="text-xs text-slate-500">Customers</dt>
                                    <dd className="mt-1 text-sm text-navy-800">
                                        {startup.customerCount}
                                    </dd>
                                </div>
                            </dl>
                        </Card>

                        {predicting && <AILoadingState message="Analyzing startup…" />}

                        {!predicting && predictError && (
                            <AIErrorState
                                message={predictError}
                                onRetry={handleAnalyze}
                            />
                        )}

                        {!predicting && prediction && (
                            <>
                                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                                    <ScoreCard
                                        score={prediction.potential_score}
                                        label="AI Potential Score"
                                    />
                                    <Card>
                                        <div className="flex items-center justify-between">
                                            <h3 className="text-base font-semibold text-navy-800">
                                                Risk Category
                                            </h3>
                                            <RiskBadge category={prediction.risk_category} />
                                        </div>
                                        <div className="mt-4 flex items-center gap-3">
                                            <Sparkles className="h-5 w-5 text-brand-600" />
                                            <div>
                                                <p className="text-sm text-navy-800">
                                                    Model version: {prediction.model_version}
                                                </p>
                                                <p className="mt-1 text-xs text-slate-500">
                                                    Categorical risk prediction from the same model.
                                                </p>
                                            </div>
                                        </div>
                                    </Card>
                                </div>

                                <Card>
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Disclaimer
                                    </h3>
                                    <p className="mt-2 text-sm text-slate-600">
                                        AI-generated insights are analytical estimates based on the
                                        provided startup information and should not be considered
                                        financial or investment advice.
                                    </p>
                                </Card>
                            </>
                        )}
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}