import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Badge from "../../components/common/Badge.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import { useAuth } from "../../hooks/useAuth.js";
import { Users, Rocket, Building2, Brain, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";

export default function AdminDashboard() {
    const { name } = useAuth();

    const stats = [
        { label: "Users", value: "—", hint: "Total platform users", icon: Users, tone: "brand" },
        { label: "Startups", value: "—", hint: "Registered startups", icon: Rocket, tone: "accent" },
        { label: "Investors", value: "—", hint: "Registered investors", icon: Building2, tone: "success" },
        { label: "AI Jobs", value: "—", hint: "Model inference runs", icon: Brain, tone: "warning" },
    ];

    return (
        <DashboardLayout title="Admin Dashboard">
            <div className="space-y-6">
                <PageHeader
                    title={`Welcome back, ${name || "admin"}`}
                    description="Oversight of users, startups, investors, and platform analytics."
                />

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    {stats.map((s) => {
                        const Icon = s.icon;
                        return (
                            <Card key={s.label} hover className="flex flex-col gap-3">
                                <div className="flex items-center justify-between">
                                    <span className="text-sm text-slate-500">{s.label}</span>
                                    <span
                                        className={`flex h-8 w-8 items-center justify-center rounded-lg ${
                                            s.tone === "accent"
                                                ? "bg-cyan-50 text-cyan-600"
                                                : s.tone === "warning"
                                                    ? "bg-amber-50 text-amber-600"
                                                    : s.tone === "success"
                                                        ? "bg-emerald-50 text-emerald-600"
                                                        : "bg-brand-50 text-brand-600"
                                        }`}
                                    >
                    <Icon className="h-4 w-4" />
                  </span>
                                </div>
                                <div>
                                    <div className="text-2xl font-semibold text-navy-800">{s.value}</div>
                                    <div className="mt-0.5 text-xs text-slate-400">{s.hint}</div>
                                </div>
                            </Card>
                        );
                    })}
                </div>

                <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                    <Card className="lg:col-span-2">
                        <div className="flex items-center justify-between">
                            <h3 className="text-base font-semibold text-navy-800">
                                Platform Activity
                            </h3>
                            <Badge tone="navy">Live soon</Badge>
                        </div>
                        <p className="mt-4 text-sm text-slate-500">
                            Platform metrics and recent events will appear here once the analytics
                            backend is integrated.
                        </p>
                    </Card>

                    <Card>
                        <h3 className="text-base font-semibold text-navy-800">AI Analytics</h3>
                        <p className="mt-4 text-sm text-slate-500">
                            Model usage and prediction statistics will appear here.
                        </p>
                    </Card>
                </div>

                <Card>
                    <h3 className="text-base font-semibold text-navy-800">Quick Actions</h3>
                    <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
                        <Link
                            to="/admin/users"
                            className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40"
                        >
                            <span className="text-sm font-medium text-navy-800">Manage Users</span>
                            <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                        </Link>
                        <Link
                            to="/admin/startups"
                            className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40"
                        >
                            <span className="text-sm font-medium text-navy-800">Manage Startups</span>
                            <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                        </Link>
                        <Link
                            to="/admin/platform-analytics"
                            className="group flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 hover:border-brand-300 hover:bg-brand-50/40"
                        >
                            <span className="text-sm font-medium text-navy-800">View Analytics</span>
                            <ArrowRight className="h-4 w-4 text-slate-400 group-hover:text-brand-600" />
                        </Link>
                    </div>
                </Card>
            </div>
        </DashboardLayout>
    );
}