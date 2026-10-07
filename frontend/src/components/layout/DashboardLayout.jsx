import { useState } from "react";
import { useAuth } from "../../hooks/useAuth.js";
import Sidebar from "./Sidebar.jsx";
import MobileSidebar from "./MobileSidebar.jsx";
import TopBar from "./TopBar.jsx";
import Footer from "./Footer.jsx";

export default function DashboardLayout({ title, children }) {
    const { role } = useAuth();
    const [mobileOpen, setMobileOpen] = useState(false);

    return (
        <div className="flex min-h-screen bg-slate-50">
            {/* Desktop sidebar */}
            <div className="hidden lg:flex lg:flex-shrink-0">
                <Sidebar role={role} />
            </div>

            {/* Mobile sidebar */}
            <MobileSidebar
                open={mobileOpen}
                role={role}
                onClose={() => setMobileOpen(false)}
            />

            {/* Main area */}
            <div className="flex min-w-0 flex-1 flex-col">
                <TopBar title={title} onOpenMenu={() => setMobileOpen(true)} />
                <main className="flex-1 px-4 py-6 sm:px-6 lg:px-8">
                    <div className="mx-auto w-full max-w-7xl">{children}</div>
                </main>
                <Footer />
            </div>
        </div>
    );
}