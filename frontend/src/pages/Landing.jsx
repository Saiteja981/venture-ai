import { Link } from "react-router-dom";
import AppLayout from "../components/layout/AppLayout.jsx";
import { Sparkles, Brain, Users } from "lucide-react";

export default function Landing() {
    return (
        <AppLayout>
            <section className="mx-auto max-w-4xl py-16 text-center sm:py-24">
        <span className="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 ring-1 ring-inset ring-brand-100">
          <Sparkles className="h-3.5 w-3.5" />
          AI-Powered Venture Platform
        </span>

                <h1 className="mt-6 text-4xl font-bold tracking-tight text-navy-800 sm:text-5xl lg:text-6xl">
                    Where Startups <span className="text-brand-600">Meet Capital</span>
                </h1>

                <p className="mx-auto mt-6 max-w-2xl text-lg text-slate-600">
                    An AI-powered collaboration platform that connects founders with investors
                    through deep learning-driven insights, intelligent recommendations, and
                    data analytics.
                </p>

                <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
                    <Link to="/register" className="btn-primary px-6 py-3 text-base">
                        Get Started
                    </Link>
                    <Link to="/login" className="btn-secondary px-6 py-3 text-base">
                        Sign In
                    </Link>
                </div>
            </section>

            <section className="mx-auto max-w-6xl pb-16">
                <div className="grid gap-6 sm:grid-cols-3">
                    <div className="card card-hover">
                        <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                            <Brain className="h-5 w-5" />
                        </div>
                        <h3 className="text-base font-semibold text-navy-800">
                            Startup Intelligence
                        </h3>
                        <p className="mt-2 text-sm text-slate-600">
                            Analyze pitch decks, predict startup potential, and surface risks using
                            deep learning.
                        </p>
                    </div>

                    <div className="card card-hover">
                        <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-cyan-50 text-cyan-600">
                            <Users className="h-5 w-5" />
                        </div>
                        <h3 className="text-base font-semibold text-navy-800">Smart Matching</h3>
                        <p className="mt-2 text-sm text-slate-600">
                            Two-tower recommendation models match investors to startups based on
                            preferences and fit.
                        </p>
                    </div>

                    <div className="card card-hover">
                        <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
                            <Sparkles className="h-5 w-5" />
                        </div>
                        <h3 className="text-base font-semibold text-navy-800">
                            Analytics & Insights
                        </h3>
                        <p className="mt-2 text-sm text-slate-600">
                            Track performance, evaluate risk, and make data-informed collaboration
                            decisions.
                        </p>
                    </div>
                </div>
            </section>
        </AppLayout>
    );
}