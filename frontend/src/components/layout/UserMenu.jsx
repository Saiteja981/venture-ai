import { useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import { useAuth } from "../../hooks/useAuth.js";

export default function UserMenu() {
    const { name, role, logout } = useAuth();
    const navigate = useNavigate();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
    }

    return (
        <div className="flex items-center gap-3 rounded-lg bg-navy-900 px-3 py-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-600 text-sm font-semibold text-white">
                {name ? name.charAt(0).toUpperCase() : "?"}
            </div>
            <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-medium text-white">{name || "User"}</div>
                <div className="truncate text-xs text-slate-400">{role || ""}</div>
            </div>
            <button
                type="button"
                onClick={handleLogout}
                title="Logout"
                className="rounded-md p-1.5 text-slate-400 hover:bg-white/10 hover:text-white"
            >
                <LogOut className="h-4 w-4" />
            </button>
        </div>
    );
}