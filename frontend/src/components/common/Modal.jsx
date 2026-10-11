import { useEffect } from "react";
import { X } from "lucide-react";

/**
 * Accessible, layout-neutral modal dialog.
 *
 * - Renders an overlay backdrop with a centered panel.
 * - Closes on backdrop click, Escape key, or the top-right X button.
 * - Locks body scroll while open.
 * - Does NOT trap focus (acceptable for the current admin use cases).
 */
export default function Modal({ open, onClose, title, children, maxWidth = "max-w-2xl" }) {
    useEffect(() => {
        if (!open) return;
        const prev = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        function onKey(e) {
            if (e.key === "Escape") onClose?.();
        }
        window.addEventListener("keydown", onKey);

        return () => {
            document.body.style.overflow = prev;
            window.removeEventListener("keydown", onKey);
        };
    }, [open, onClose]);

    if (!open) return null;

    return (
        <div
            className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/40 p-4 sm:p-8"
            onClick={onClose}
            role="dialog"
            aria-modal="true"
            aria-label={title || "Dialog"}
        >
            <div
                className={`relative w-full ${maxWidth} rounded-xl bg-white shadow-xl`}
                onClick={(e) => e.stopPropagation()}
            >
                <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
                    <h2 className="text-base font-semibold text-navy-800">
                        {title}
                    </h2>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                        aria-label="Close"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>
                <div className="px-5 py-5">
                    {children}
                </div>
            </div>
        </div>
    );
}