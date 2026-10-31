import { Navigate, Route, Routes } from "react-router-dom";
import Landing from "../pages/Landing.jsx";
import Unauthorized from "../pages/Unauthorized.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";
import GoogleRolePrompt from "../pages/auth/GoogleRolePrompt.jsx";
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
import NotificationsPage from "../pages/common/Notifications.jsx";
import ProtectedRoute from "../components/auth/ProtectedRoute.jsx";
import DashboardLayout from "../components/layout/DashboardLayout.jsx";
import Placeholder from "../components/layout/Placeholder.jsx";
import Discover from "../pages/investor/Discover.jsx";
import InvestorCollaborations from "../pages/investor/Collaborations.jsx";
import StartupCollaborations from "../pages/startup/Collaborations.jsx";
import Messages from "../pages/common/Messages.jsx";
import SavedStartups from "../pages/investor/SavedStartups.jsx";
import Portfolio from "../pages/investor/Portfolio.jsx";
import AdminUsers from "../pages/admin/Users.jsx";
import AuditLogs from "../pages/admin/AuditLogs.jsx";
import InvestorMatches from "../pages/startup/InvestorMatches.jsx";
import AiMonitoring from "../pages/admin/AiMonitoring.jsx";
import AdminStartups from "../pages/admin/Startups.jsx";
import AdminInvestors from "../pages/admin/Investors.jsx";
import Settings from "../pages/common/Settings.jsx";

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
            <Route path="/auth/google/complete" element={<GoogleRolePrompt />} />

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
                path="/startup/collaborations"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <StartupCollaborations />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/messages"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <Messages />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/messages/:conversationId"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <Messages />
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
                        <InvestorMatches />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/investor-matches"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <InvestorMatches />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/notifications"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <NotificationsPage />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/startup/settings"
                element={
                    <ProtectedRoute allowedRoles={["STARTUP"]}>
                        <Settings />
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
                        <Discover />
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
                        <Portfolio />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/saved-startups"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <SavedStartups />
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
                path="/investor/collaborations"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <InvestorCollaborations />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/messages"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <Messages />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/messages/:conversationId"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <Messages />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/notifications"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <NotificationsPage />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/investor/settings"
                element={
                    <ProtectedRoute allowedRoles={["INVESTOR"]}>
                        <Settings />
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
                        <AdminUsers />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/audit-logs"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <AuditLogs />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/startups"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <AdminStartups />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/admin/investors"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <AdminInvestors />
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
                path="/admin/ai-monitoring"
                element={
                    <ProtectedRoute allowedRoles={["ADMIN"]}>
                        <AiMonitoring />
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
                        <Settings />
                    </ProtectedRoute>
                }
            />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}