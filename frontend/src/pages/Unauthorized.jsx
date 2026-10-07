import { Link } from "react-router-dom";
import AppLayout from "../components/layout/AppLayout.jsx";
import { ShieldAlert } from "lucide-react";

export default function Unauthorized() {
    return (
        <AppLayout>
            <section className="mx-auto flex max-w-md flex-col items-center py-24 text-center">
                <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-red-500">
                    <ShieldAlert className="h-8 w-8" />
                </div>
                <h1 className="text-3xl font-bold text-navy-800">Access Denied</h1>
                <p className="mt-3 text-slate-600">
                    You do not have permission to view this page with your current role.
                </p>
                <div className="mt-8">
                    <Link to="/" className="btn-primary">
                        Return Home
                    </Link>
                </div>
            </section>
        </AppLayout>
    );
}