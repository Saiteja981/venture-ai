import { navigationForRole } from "../../config/navigation.js";
import SidebarItem from "./SidebarItem.jsx";
import UserMenu from "./UserMenu.jsx";
import { useNotificationContext } from "../../context/NotificationContext.jsx";

export default function Sidebar({ role, onNavigate }) {
    const sections = navigationForRole(role);
    const { unreadCount } = useNotificationContext();

    return (
        <aside className="flex h-full w-64 flex-col bg-navy-800 text-white">
            <div className="flex items-center gap-3 border-b border-white/10 px-5 py-5">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-white font-bold">
                    V
                </div>
                <div className="min-w-0">
                    <div className="truncate text-sm font-semibold text-white">Venture AI</div>
                    <div className="truncate text-xs text-slate-400">AI-Powered VC Platform</div>
                </div>
            </div>

            <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-5">
                {sections.map((section) => (
                    <div key={section.section}>
                        <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                            {section.section}
                        </div>
                        <div className="space-y-1">
                            {section.items.map((item) => {
                                const badge =
                                    item.badgeKey === "unreadNotifications" ? unreadCount : 0;
                                return (
                                    <div key={item.path} onClick={onNavigate}>
                                        <SidebarItem
                                            label={item.label}
                                            path={item.path}
                                            icon={item.icon}
                                            badge={badge}
                                        />
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                ))}
            </nav>

            <div className="border-t border-white/10 p-3">
                <UserMenu />
            </div>
        </aside>
    );
}