import { useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import Button from "../../components/common/Button.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import ExportCard from "../../components/reports/ExportCard.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useAuth } from "../../hooks/useAuth.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    Bell, Lock, User as UserIcon, Check, Sliders, Download,
} from "lucide-react";
import {
    getPreferences,
    updatePreferences,
    changePassword,
} from "../../services/settings/settingsService.js";
import {
    downloadStartupProfile,
    downloadStartupAiAnalysis,
    downloadInvestorSavedStartups,
    downloadInvestorPortfolio,
} from "../../services/reports/reportService.js";

const PREF_ITEMS = [
    { key: "notifyCollab", label: "Collaboration updates",
        hint: "Requests, acceptances, rejections" },
    { key: "notifyMessages", label: "Messages",
        hint: "New direct messages" },
    { key: "notifyAiEvents", label: "AI event notifications",
        hint: "Predictions, recommendations, pitch-deck analysis" },
    { key: "notifyUpdates", label: "Platform updates",
        hint: "Startup and investor profile changes, system notices" },
];

const TOP_K_OPTIONS = [
    { value: 3, label: "3" },
    { value: 5, label: "5" },
    { value: 10, label: "10" },
    { value: 20, label: "20" },
];

function Toggle({ checked, onChange, disabled }) {
    return (
        <button
            type="button"
            role="switch"
            aria-checked={checked}
            disabled={disabled}
            onClick={() => onChange(!checked)}
            className={`relative h-6 w-11 flex-shrink-0 rounded-full transition ${
                checked ? "bg-brand-600" : "bg-slate-300"
            } ${disabled ? "opacity-50" : ""}`}
        >
            <span
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition ${
                    checked ? "left-5" : "left-0.5"
                }`}
            />
        </button>
    );
}

export default function Settings() {
    const toast = useToast();
    const { name, email, role } = useAuth();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [prefs, setPrefs] = useState(null);
    const [savingPrefs, setSavingPrefs] = useState(false);

    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [pwError, setPwError] = useState("");
    const [savingPw, setSavingPw] = useState(false);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const data = await getPreferences();
                if (!cancelled) setPrefs(data);
            } catch (err) {
                if (!cancelled) setError(messageForError(err) || "Could not load preferences.");
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, []);

    async function handleToggle(key, value) {
        const previous = prefs;
        const next = { ...prefs, [key]: value };
        setPrefs(next);
        setSavingPrefs(true);
        try {
            const saved = await updatePreferences(next);
            setPrefs(saved);
            toast.success("Preferences saved.");
        } catch (err) {
            setPrefs(previous);
            toast.error(messageForError(err) || "Could not save preferences.");
        } finally {
            setSavingPrefs(false);
        }
    }

    async function handleChangePassword(e) {
        e.preventDefault();
        setPwError("");
        if (!currentPassword || !newPassword) {
            setPwError("Both fields are required.");
            return;
        }
        if (newPassword.length < 8) {
            setPwError("New password must be at least 8 characters.");
            return;
        }
        if (newPassword !== confirmPassword) {
            setPwError("New password and confirmation do not match.");
            return;
        }
        setSavingPw(true);
        try {
            await changePassword(currentPassword, newPassword);
            toast.success("Password changed.");
            setCurrentPassword("");
            setNewPassword("");
            setConfirmPassword("");
        } catch (err) {
            setPwError(messageForError(err) || "Could not change password.");
        } finally {
            setSavingPw(false);
        }
    }

    return (
        <DashboardLayout title="Settings">
            <div className="space-y-6">
                <PageHeader
                    title="Settings"
                    description="Manage your account preferences and security."
                />

                {loading ? (
                    <LoadingState message="Loading settings…" />
                ) : error ? (
                    <ErrorState title="Could not load settings" message={error} />
                ) : (
                    <>
                        {/* Account info (read-only) */}
                        <Card>
                            <div className="flex items-start gap-3">
                                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                                    <UserIcon className="h-5 w-5" />
                                </div>
                                <div className="min-w-0 flex-1">
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Account
                                    </h3>
                                    <dl className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
                                        <div>
                                            <dt className="text-xs text-slate-500">Name</dt>
                                            <dd className="mt-1 text-sm text-navy-800">{name ?? "—"}</dd>
                                        </div>
                                        <div>
                                            <dt className="text-xs text-slate-500">Email</dt>
                                            <dd className="mt-1 text-sm text-navy-800">{email ?? "—"}</dd>
                                        </div>
                                        <div>
                                            <dt className="text-xs text-slate-500">Role</dt>
                                            <dd className="mt-1">
                                                <Badge tone="brand">{role ?? "—"}</Badge>
                                            </dd>
                                        </div>
                                    </dl>
                                    <p className="mt-4 text-xs text-slate-500">
                                        Profile fields are managed from your profile page.
                                        Role changes are not available through settings.
                                    </p>
                                </div>
                            </div>
                        </Card>

                        {/* Notification preferences */}
                        <Card>
                            <div className="flex items-start gap-3">
                                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-amber-50 text-amber-600">
                                    <Bell className="h-5 w-5" />
                                </div>
                                <div className="min-w-0 flex-1">
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Notifications
                                    </h3>
                                    <p className="mt-1 text-xs text-slate-500">
                                        Choose which events generate in-app notifications.
                                    </p>
                                    <ul className="mt-5 divide-y divide-slate-100">
                                        {PREF_ITEMS.map((item) => (
                                            <li
                                                key={item.key}
                                                className="flex items-center justify-between gap-3 py-3"
                                            >
                                                <div>
                                                    <div className="text-sm font-medium text-navy-800">
                                                        {item.label}
                                                    </div>
                                                    <div className="mt-0.5 text-xs text-slate-500">
                                                        {item.hint}
                                                    </div>
                                                </div>
                                                <Toggle
                                                    checked={!!prefs?.[item.key]}
                                                    onChange={(v) => handleToggle(item.key, v)}
                                                    disabled={savingPrefs}
                                                />
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            </div>
                        </Card>

                        {/* Investor-only: recommendation display */}
                        {role === "INVESTOR" && (
                            <Card>
                                <div className="flex items-start gap-3">
                                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                                        <Sliders className="h-5 w-5" />
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <h3 className="text-base font-semibold text-navy-800">
                                            Recommendations
                                        </h3>
                                        <p className="mt-1 text-xs text-slate-500">
                                            How many startup recommendations to show at once.
                                            The two-tower model returns a ranked list; this
                                            only controls the page size.
                                        </p>
                                        <div className="mt-5">
                                            <label className="block text-xs text-slate-500 mb-2">
                                                Recommendations per request
                                            </label>
                                            <div className="inline-flex rounded-lg border border-slate-200 bg-white p-1">
                                                {TOP_K_OPTIONS.map((opt) => {
                                                    const current =
                                                        prefs?.recommendationsPerPage ?? 5;
                                                    const active = current === opt.value;
                                                    return (
                                                        <button
                                                            key={opt.value}
                                                            type="button"
                                                            disabled={savingPrefs}
                                                            onClick={() => handleToggle(
                                                                "recommendationsPerPage",
                                                                opt.value)}
                                                            className={`min-w-[48px] rounded-md px-3 py-1.5 text-sm font-medium transition ${
                                                                active
                                                                    ? "bg-brand-600 text-white"
                                                                    : "text-slate-600 hover:bg-slate-100"
                                                            }`}
                                                        >
                                                            {opt.label}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                            <p className="mt-2 text-[11px] text-slate-400">
                                                Default is 5.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </Card>
                        )}

                        {/* Password change */}
                        <Card>
                            <div className="flex items-start gap-3">
                                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-red-50 text-red-600">
                                    <Lock className="h-5 w-5" />
                                </div>
                                <div className="min-w-0 flex-1">
                                    <h3 className="text-base font-semibold text-navy-800">
                                        Change password
                                    </h3>
                                    <p className="mt-1 text-xs text-slate-500">
                                        Minimum 8 characters. You will need your current password.
                                    </p>
                                    <form onSubmit={handleChangePassword} className="mt-5 space-y-4 max-w-md">
                                        <div>
                                            <label className="block text-xs text-slate-500 mb-1">
                                                Current password
                                            </label>
                                            <input
                                                type="password"
                                                autoComplete="current-password"
                                                value={currentPassword}
                                                onChange={(e) => setCurrentPassword(e.target.value)}
                                                className="input-field"
                                                disabled={savingPw}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs text-slate-500 mb-1">
                                                New password
                                            </label>
                                            <input
                                                type="password"
                                                autoComplete="new-password"
                                                value={newPassword}
                                                onChange={(e) => setNewPassword(e.target.value)}
                                                className="input-field"
                                                disabled={savingPw}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs text-slate-500 mb-1">
                                                Confirm new password
                                            </label>
                                            <input
                                                type="password"
                                                autoComplete="new-password"
                                                value={confirmPassword}
                                                onChange={(e) => setConfirmPassword(e.target.value)}
                                                className="input-field"
                                                disabled={savingPw}
                                            />
                                        </div>
                                        {pwError && (
                                            <p className="text-xs text-red-600">{pwError}</p>
                                        )}
                                        <div className="flex justify-end">
                                            <Button type="submit" disabled={savingPw}>
                                                <Check className="h-4 w-4" />
                                                {savingPw ? "Saving…" : "Change password"}
                                            </Button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </Card>

                        {/* Phase 27: Reports & Export */}
                        {(role === "STARTUP" || role === "INVESTOR") && (
                            <Card className="!p-0 overflow-hidden">
                                <div className="flex items-start gap-3 border-b border-slate-100 px-5 py-4">
                                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
                                        <Download className="h-5 w-5" />
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <h3 className="text-base font-semibold text-navy-800">
                                            Reports & Export
                                        </h3>
                                        <p className="mt-1 text-xs text-slate-500">
                                            Download your account data. Reports contain
                                            persisted records only.
                                        </p>
                                    </div>
                                </div>
                                <div className="space-y-3 p-5">
                                    {role === "STARTUP" && (
                                        <>
                                            <ExportCard
                                                title="Startup Profile"
                                                description="Your company profile and available metrics."
                                                onExport={downloadStartupProfile}
                                            />
                                            <ExportCard
                                                title="AI Analysis History"
                                                description="Execution metadata for your AI analyses. Prediction scores are not persisted and are not included."
                                                onExport={downloadStartupAiAnalysis}
                                            />
                                        </>
                                    )}
                                    {role === "INVESTOR" && (
                                        <>
                                            <ExportCard
                                                title="Saved Startups"
                                                description="Every startup you have bookmarked."
                                                onExport={downloadInvestorSavedStartups}
                                            />
                                            <ExportCard
                                                title="Collaboration Portfolio"
                                                description="Startups with an accepted collaboration request."
                                                onExport={downloadInvestorPortfolio}
                                            />
                                        </>
                                    )}
                                </div>
                            </Card>
                        )}
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}