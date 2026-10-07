import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import AppLayout from "../../components/layout/AppLayout.jsx";
import Button from "../../components/common/Button.jsx";
import ErrorMessage from "../../components/common/ErrorMessage.jsx";
import { loginRequest } from "../../services/auth/authService.js";
import { useAuth } from "../../hooks/useAuth.js";
import { messageForError } from "../../utils/errorHandler.js";
import { redirectPathForRole } from "../../utils/roleRedirect.js";

export default function Login() {
    const navigate = useNavigate();
    const location = useLocation();
    const { login, isAuthenticated, role } = useAuth();

    const [form, setForm] = useState({ email: "", password: "" });
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    if (isAuthenticated) {
        return <Navigate to={redirectPathForRole(role)} replace />;
    }

    function handleChange(e) {
        setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        if (!form.email.trim() || !form.password.trim()) {
            setError("Email and password are required.");
            return;
        }

        setLoading(true);
        try {
            const auth = await loginRequest({
                email: form.email.trim(),
                password: form.password,
            });
            login(auth);
            const from = location.state?.from;
            navigate(from || redirectPathForRole(auth.role), { replace: true });
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }

    return (
        <AppLayout>
            <div className="mx-auto max-w-md py-12">
                <div className="card">
                    <h1 className="text-2xl font-bold">Welcome back</h1>
                    <p className="mt-1 text-sm text-slate-600">Sign in to your Venture AI account.</p>

                    <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-slate-700">
                                Email
                            </label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                autoComplete="email"
                                required
                                value={form.email}
                                onChange={handleChange}
                                className="input-field mt-1"
                            />
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-slate-700">
                                Password
                            </label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                autoComplete="current-password"
                                required
                                value={form.password}
                                onChange={handleChange}
                                className="input-field mt-1"
                            />
                        </div>

                        <ErrorMessage>{error}</ErrorMessage>

                        <Button type="submit" disabled={loading} className="w-full">
                            {loading ? "Signing in…" : "Sign In"}
                        </Button>
                    </form>

                    <p className="mt-6 text-center text-sm text-slate-600">
                        Don&apos;t have an account?{" "}
                        <Link to="/register" className="font-medium text-brand-600 hover:text-brand-700">
                            Create one
                        </Link>
                    </p>
                </div>
            </div>
        </AppLayout>
    );
}