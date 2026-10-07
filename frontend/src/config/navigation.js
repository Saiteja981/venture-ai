import {
    LayoutDashboard,
    Rocket,
    BarChart3,
    Brain,
    FileText,
    Users,
    Bell,
    Settings,
    UserCircle2,
    Compass,
    Sparkles,
    Briefcase,
    LineChart,
    Building2,
} from "lucide-react";

export const startupNavigation = [
    {
        section: "Main",
        items: [
            {
                label: "Dashboard",
                path: "/startup/dashboard",
                icon: LayoutDashboard,
            },
        ],
    },
    {
        section: "Startup",
        items: [
            { label: "My Startup", path: "/startup/profile", icon: Rocket },
            { label: "Startup Analytics", path: "/startup/analytics", icon: BarChart3 },
            { label: "AI Potential & Risk", path: "/startup/ai-potential", icon: Brain },
            { label: "Pitch Deck", path: "/startup/pitch-deck", icon: FileText },
            { label: "Investor Matches", path: "/startup/matches", icon: Users },
        ],
    },
    {
        section: "Communication",
        items: [
            { label: "Notifications", path: "/startup/notifications", icon: Bell },
        ],
    },
    {
        section: "System",
        items: [
            { label: "Settings", path: "/startup/settings", icon: Settings },
        ],
    },
];

export const investorNavigation = [
    {
        section: "Main",
        items: [
            { label: "Dashboard", path: "/investor/dashboard", icon: LayoutDashboard },
        ],
    },
    {
        section: "Investor",
        items: [
            { label: "My Profile", path: "/investor/profile", icon: UserCircle2 },
            { label: "Discover Startups", path: "/investor/discover", icon: Compass },
            { label: "Recommended Startups", path: "/investor/recommended", icon: Sparkles },
            { label: "Portfolio", path: "/investor/portfolio", icon: Briefcase },
            { label: "AI Insights", path: "/investor/insights", icon: Brain },
        ],
    },
    {
        section: "Communication",
        items: [
            { label: "Notifications", path: "/investor/notifications", icon: Bell },
        ],
    },
    {
        section: "System",
        items: [
            { label: "Settings", path: "/investor/settings", icon: Settings },
        ],
    },
];

export const adminNavigation = [
    {
        section: "Main",
        items: [
            { label: "Dashboard", path: "/admin/dashboard", icon: LayoutDashboard },
        ],
    },
    {
        section: "Management",
        items: [
            { label: "Users", path: "/admin/users", icon: Users },
            { label: "Startups", path: "/admin/startups", icon: Rocket },
            { label: "Investors", path: "/admin/investors", icon: Building2 },
        ],
    },
    {
        section: "Analytics",
        items: [
            { label: "AI Analytics", path: "/admin/ai-analytics", icon: Brain },
            { label: "Platform Analytics", path: "/admin/platform-analytics", icon: LineChart },
        ],
    },
    {
        section: "System",
        items: [
            { label: "Settings", path: "/admin/settings", icon: Settings },
        ],
    },
];

export function navigationForRole(role) {
    switch (role) {
        case "STARTUP":
            return startupNavigation;
        case "INVESTOR":
            return investorNavigation;
        case "ADMIN":
            return adminNavigation;
        default:
            return [];
    }
}