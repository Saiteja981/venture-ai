import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth.js";

export default function ProtectedRoute({ children, allowedRoles }) {
    const { isAuthenticated, role, initializing } = useAuth();
    const location = useLocation();

    if (initializing) {
        return (
            <div className="flex min-h-screen items-center justify-center text-slate-500">
                Loading…
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    }

    if (Array.isArray(allowedRoles) && allowedRoles.length > 0) {
        if (!allowedRoles.includes(role)) {
            return <Navigate to="/unauthorized" replace />;
        }
    }

    return children;
}