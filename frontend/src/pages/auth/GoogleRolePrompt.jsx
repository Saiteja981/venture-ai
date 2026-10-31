import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import AppLayout from "../../components/layout/AppLayout.jsx";
import Button from "../../components/common/Button.jsx";
import ErrorMessage from "../../components/common/ErrorMessage.jsx";
import { googleAuthRequest } from "../../services/auth/authService.js";
import { useAuth } from "../../hooks/useAuth.js";
import { messageForError } from "../../utils/errorHandler.js";
import { redirectPathForRole } from "../../utils/roleRedirect.js";
import { Rocket, Building2 } from "lucide-react";

export default function GoogleRolePrompt() {
    const navigate = useNavigate();
    const location = useLocation();
    const { login, isAuthenticated } = useAuth();

    const idToken = location.state?.idToken;

    const [role, setRole] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    if (isAuthenticated) {
        return <Navigate to="/" replace />;
    }
    if (!idToken) {
        return <Navigate to="/login" replace />;
    }

    async function handleContinue() {
        if (!role) {
            setError("Please choose an account type.");
            return;
        }
        setLoading(true);
        setError("");
        try {
            const result = await googleAuthRequest(idToken, role);
            if (!result.auth) {
                setError("Could not complete sign-up. Please try again.");
                return;
            }
            login(result.auth);
            navigate(redirectPathForRole(result.auth.role), { replace: true });
        } catch (err) {
            setError(messageForError(err) || "Could not complete sign-up.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <AppLayout>
            <div className="mx-auto max-w-md py-12">
                <div className="card">
                    <h1 className="text-2xl font-bold">Choose your account type</h1>
                    <p className="mt-1 text-sm text-slate-600">
                        This determines your default dashboard and available features.
                        It cannot be changed later through Settings.
                    </p>

                    <div className="mt-6 space-y-3">
                        <button
                            type="button"
                            onClick={() => setRole("STARTUP")}
                            className={`flex w-full items-start gap-3 rounded-lg border p-4 text-left transition ${
                                role === "STARTUP"
                                    ? "border-brand-500 bg-brand-50"
                                    : "border-slate-200 hover:border-slate-300"
                            }`}
                        >
                            <div className="rounded-lg bg-brand-100 p-2 text-brand-700">
                                <Rocket className="h-5 w-5" />
                            </div>
                            <div>
                                <div className="text-sm font-semibold text-navy-800">
                                    Startup Founder
                                </div>
                                <div className="mt-0.5 text-xs text-slate-500">
                                    Create a startup profile, run AI analysis, find investors.
                                </div>
                            </div>
                        </button>

                        <button
                            type="button"
                            onClick={() => setRole("INVESTOR")}
                            className={`flex w-full items-start gap-3 rounded-lg border p-4 text-left transition ${
                                role === "INVESTOR"
                                    ? "border-brand-500 bg-brand-50"
                                    : "border-slate-200 hover:border-slate-300"
                            }`}
                        >
                            <div className="rounded-lg bg-amber-100 p-2 text-amber-700">
                                <Building2 className="h-5 w-5" />
                            </div>
                            <div>
                                <div className="text-sm font-semibold text-navy-800">
                                    Investor
                                </div>
                                <div className="mt-0.5 text-xs text-slate-500">
                                    Discover startups, save portfolio, express interest.
                                </div>
                            </div>
                        </button>
                    </div>

                    <ErrorMessage>{error}</ErrorMessage>

                    <Button
                        onClick={handleContinue}
                        disabled={loading || !role}
                        className="mt-6 w-full"
                    >
                        {loading ? "Creating account…" : "Continue"}
                    </Button>
                </div>
            </div>
        </AppLayout>
    );
}