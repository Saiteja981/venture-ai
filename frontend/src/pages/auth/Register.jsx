import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import AppLayout from "../../components/layout/AppLayout.jsx";
import Button from "../../components/common/Button.jsx";
import ErrorMessage from "../../components/common/ErrorMessage.jsx";
import { registerRequest } from "../../services/auth/authService.js";
import { useAuth } from "../../hooks/useAuth.js";
import { messageForError } from "../../utils/errorHandler.js";
import { redirectPathForRole } from "../../utils/roleRedirect.js";

const ROLE_OPTIONS = [
    { value: "STARTUP", label: "Startup" },
    { value: "INVESTOR", label: "Investor" },
];

export default function Register() {
    const navigate = useNavigate();
    const { login, isAuthenticated, role } = useAuth();

    const [form, setForm] = useState({
        name: "",
        email: "",
        password: "",
        role: "STARTUP",
    });
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

        if (!form.name.trim() || !form.email.trim() || !form.password) {
            setError("Name, email, and password are required.");
            return;
        }
        if (form.password.length < 8) {
            setError("Password must be at least 8 characters.");
            return;
        }
        if (!["STARTUP", "INVESTOR"].includes(form.role)) {
            setError("Please select a valid role.");
            return;
        }

        setLoading(true);
        try {
            const auth = await registerRequest({
                name: form.name.trim(),
                email: form.email.trim(),
                password: form.password,
                role: form.role,
            });
            login(auth);
            navigate(redirectPathForRole(auth.role), { replace: true });
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
                    <h1 className="text-2xl font-bold">Create your account</h1>
                    <p className="mt-1 text-sm text-slate-600">
                        Join the platform as a startup or investor.
                    </p>

                    <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
                        <div>
                            <label htmlFor="name" className="block text-sm font-medium text-slate-700">
                                Name
                            </label>
                            <input
                                id="name"
                                name="name"
                                type="text"
                                autoComplete="name"
                                required
                                value={form.name}
                                onChange={handleChange}
                                className="input-field mt-1"
                            />
                        </div>

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
                                autoComplete="new-password"
                                required
                                minLength={8}
                                value={form.password}
                                onChange={handleChange}
                                className="input-field mt-1"
                            />
                            <p className="mt-1 text-xs text-slate-500">At least 8 characters.</p>
                        </div>

                        <div>
                            <span className="block text-sm font-medium text-slate-700">Role</span>
                            <div className="mt-2 grid grid-cols-2 gap-2">
                                {ROLE_OPTIONS.map((opt) => (
                                    <label
                                        key={opt.value}
                                        className={`flex cursor-pointer items-center justify-center rounded-lg border px-3 py-2 text-sm ${
                                            form.role === opt.value
                                                ? "border-brand-500 bg-brand-50 text-brand-700"
                                                : "border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                                        }`}
                                    >
                                        <input
                                            type="radio"
                                            name="role"
                                            value={opt.value}
                                            checked={form.role === opt.value}
                                            onChange={handleChange}
                                            className="sr-only"
                                        />
                                        {opt.label}
                                    </label>
                                ))}
                            </div>
                        </div>

                        <ErrorMessage>{error}</ErrorMessage>

                        <Button type="submit" disabled={loading} className="w-full">
                            {loading ? "Creating account…" : "Create Account"}
                        </Button>
                    </form>

                    <p className="mt-6 text-center text-sm text-slate-600">
                        Already have an account?{" "}
                        <Link to="/login" className="font-medium text-brand-600 hover:text-brand-700">
                            Sign in
                        </Link>
                    </p>
                </div>
            </div>
        </AppLayout>
    );
}