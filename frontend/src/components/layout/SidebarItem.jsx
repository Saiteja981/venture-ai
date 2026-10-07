import { NavLink } from "react-router-dom";

export default function SidebarItem({ label, path, icon: Icon }) {
    return (
        <NavLink
            to={path}
            className={({ isActive }) =>
                `group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                        ? "bg-brand-600 text-white"
                        : "text-slate-300 hover:bg-navy-700 hover:text-white"
                }`
            }
        >
            {({ isActive }) => (
                <>
                    {Icon && (
                        <Icon
                            className={`h-4 w-4 flex-shrink-0 ${
                                isActive ? "text-white" : "text-slate-400 group-hover:text-white"
                            }`}
                        />
                    )}
                    <span className="truncate">{label}</span>
                </>
            )}
        </NavLink>
    );
}