import { CheckCircle2, AlertCircle, Info, X } from "lucide-react";

export default function Notification({ tone = "success", children, onClose }) {
    const styles = {
        success: "border-emerald-200 bg-emerald-50 text-emerald-800",
        error: "border-red-200 bg-red-50 text-red-800",
        info: "border-slate-200 bg-white text-navy-800",
    };
    const Icon =
        tone === "error" ? AlertCircle : tone === "info" ? Info : CheckCircle2;

    return (
        <div
            role="status"
            className={`pointer-events-auto flex items-start gap-3 rounded-lg border px-4 py-3 shadow-card ${styles[tone] ?? styles.info}`}
        >
            <Icon className="mt-0.5 h-4 w-4 flex-shrink-0" />
            <div className="min-w-0 flex-1 text-sm">{children}</div>
            <button
                type="button"
                onClick={onClose}
                className="rounded-md p-1 hover:bg-black/5"
                aria-label="Dismiss"
            >
                <X className="h-3.5 w-3.5" />
            </button>
        </div>
    );
}