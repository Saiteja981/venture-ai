import { NavLink } from "react-router-dom";

export default function SidebarItem({ label, path, icon: Icon, badge }) {
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
                    <span className="truncate flex-1">{label}</span>
                    {badge > 0 && (
                        <span
                            className={`inline-flex items-center justify-center rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                                isActive
                                    ? "bg-white text-brand-600"
                                    : "bg-brand-600 text-white"
                            }`}
                        >
              {badge > 99 ? "99+" : badge}
            </span>
                    )}
                </>
            )}
        </NavLink>
    );
}