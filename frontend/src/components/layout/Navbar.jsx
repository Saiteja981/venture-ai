import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth.js";
import { redirectPathForRole } from "../../utils/roleRedirect.js";

export default function Navbar() {
    const { isAuthenticated, name, role, logout } = useAuth();
    const navigate = useNavigate();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
    }

    return (
        <header className="border-b border-slate-200 bg-white">
            <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
                <Link to="/" className="flex items-center gap-2">
                    <span className="text-lg font-semibold text-brand-600">Venture AI</span>
                </Link>

                <nav className="flex items-center gap-4">
                    {isAuthenticated ? (
                        <>
              <span className="hidden text-sm text-slate-600 sm:inline">
                {name} <span className="text-slate-400">·</span>{" "}
                  <span className="font-medium text-slate-700">{role}</span>
              </span>
                            <button
                                type="button"
                                onClick={() => navigate(redirectPathForRole(role))}
                                className="btn-secondary"
                            >
                                Dashboard
                            </button>
                            <button type="button" onClick={handleLogout} className="btn-primary">
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="text-sm font-medium text-slate-700 hover:text-brand-600">
                                Login
                            </Link>
                            <Link to="/register" className="btn-primary">
                                Register
                            </Link>
                        </>
                    )}
                </nav>
            </div>
        </header>
    );
}