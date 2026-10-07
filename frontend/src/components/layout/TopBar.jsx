import { Bell, Menu } from "lucide-react";
import Badge from "../common/Badge.jsx";
import { useAuth } from "../../hooks/useAuth.js";

export default function TopBar({ title, onOpenMenu }) {
    const { name, role } = useAuth();

    const toneForRole =
        role === "ADMIN" ? "danger" : role === "INVESTOR" ? "accent" : "brand";

    return (
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur">
            <div className="flex h-16 items-center gap-3 px-4 sm:px-6 lg:px-8">
                {/* Mobile menu button */}
                <button
                    type="button"
                    onClick={onOpenMenu}
                    className="rounded-md p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
                    aria-label="Open menu"
                >
                    <Menu className="h-5 w-5" />
                </button>

                {/* Title */}
                <h2 className="truncate text-base font-semibold text-navy-800 sm:text-lg">
                    {title}
                </h2>

                {/* Right side */}
                <div className="ml-auto flex items-center gap-3">
                    <button
                        type="button"
                        className="relative rounded-md p-2 text-slate-500 hover:bg-slate-100 hover:text-navy-800"
                        aria-label="Notifications"
                    >
                        <Bell className="h-5 w-5" />
                        <span className="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-brand-600" />
                    </button>

                    <div className="hidden items-center gap-2 sm:flex">
                        <span className="text-sm font-medium text-navy-800">{name}</span>
                        <Badge tone={toneForRole}>{role}</Badge>
                    </div>

                    <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-600 text-sm font-semibold text-white">
                        {name ? name.charAt(0).toUpperCase() : "?"}
                    </div>
                </div>
            </div>
        </header>
    );
}