import { X } from "lucide-react";
import Sidebar from "./Sidebar.jsx";

export default function MobileSidebar({ open, role, onClose }) {
    if (!open) return null;

    return (
        <div className="fixed inset-0 z-40 lg:hidden">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-navy-900/60"
                onClick={onClose}
                aria-hidden="true"
            />
            {/* Drawer */}
            <div className="absolute inset-y-0 left-0 flex w-64 flex-col shadow-xl">
                <button
                    type="button"
                    onClick={onClose}
                    className="absolute right-2 top-2 z-10 rounded-md p-1.5 text-slate-300 hover:bg-white/10 hover:text-white"
                    aria-label="Close menu"
                >
                    <X className="h-4 w-4" />
                </button>
                <Sidebar role={role} onNavigate={onClose} />
            </div>
        </div>
    );
}