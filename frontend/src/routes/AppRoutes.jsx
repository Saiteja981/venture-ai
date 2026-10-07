import { Navigate, Route, Routes } from "react-router-dom";
import Landing from "../pages/Landing.jsx";
import Unauthorized from "../pages/Unauthorized.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";
import StartupDashboard from "../pages/startup/Dashboard.jsx";
import InvestorDashboard from "../pages/investor/Dashboard.jsx";
import AdminDashboard from "../pages/admin/Dashboard.jsx";
import StartupProfilePage from "../pages/startup/Profile.jsx";
import InvestorProfilePage from "../pages/investor/Profile.jsx";
import AIPotential from "../pages/startup/AIPotential.jsx";
import PitchDeck from "../pages/startup/PitchDeck.jsx";
import Recommended from "../pages/investor/Recommended.jsx";
import StartupAnalytics from "../pages/startup/Analytics.jsx";
import InvestorInsights from "../pages/investor/Insights.jsx";
import PlatformAnalytics from "../pages/admin/PlatformAnalytics.jsx";
import AIAnalytics from "../pages/admin/AIAnalytics.jsx";
import ProtectedRoute from "../components/auth/ProtectedRoute.jsx";
import DashboardLayout from "../components/layout/DashboardLayout.jsx";
import Placeholder from "../components/layout/Placeholder.jsx";

function ComingSoon({ title, description }) {
    return (
        <DashboardLayout title={title}>
            <Placeholder title={title} description={description} />
        </DashboardLayout>
    );
}

export default function AppRoutes() {
    return (
        <Routes>
            {/* Public */}
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/unauthorized" element={<Unauthorized />} />

            {/* Startup */}
            <Route
                path="/startup/dashboard"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <StartupDashboard />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/profile"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <StartupProfilePage />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/analytics"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <StartupAnalytics />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/potential"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <AIPotential />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/ai-potential"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <AIPotential />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/pitch-deck"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <PitchDeck />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/matches"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <ComingSoon
                            title="Investor Matches"
                            description="The recommendation model matches investors to startups. Investor-facing recommendations are available on the investor dashboard."
                        />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/investor-matches"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <ComingSoon
                            title="Investor Matches"
                            description="The recommendation model matches investors to startups. Investor-facing recommendations are available on the investor dashboard."
                        />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/notifications"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <ComingSoon title="Notifications" />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/settings"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <ComingSoon title="Settings" />
                    </ProtectedRoute>
                }
            />

            {/* Investor */}
            <Route
                path="/investor/dashboard"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <InvestorDashboard />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/profile"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <InvestorProfilePage />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/discover"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <ComingSoon title="Discover Startups" description="Browse startups on the platform." />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/recommended"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <Recommended />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/portfolio"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <ComingSoon title="Portfolio" description="Track your investments." />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/insights"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <InvestorInsights />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/notifications"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <ComingSoon title="Notifications" />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/settings"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <ComingSoon title="Settings" />
                    </ProtectedRoute>
                }
            />

            {/* Admin */}
            <Route
                path="/admin/dashboard"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <AdminDashboard />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/users"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <ComingSoon title="Users" description="Manage platform users." />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/startups"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <ComingSoon title="Startups" description="Manage startups." />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/investors"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <ComingSoon title="Investors" description="Manage investors." />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/ai-analytics"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <AIAnalytics />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/platform-analytics"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <PlatformAnalytics />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/settings"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <ComingSoon title="Settings" />
                    </ProtectedRoute>
                }
            />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}