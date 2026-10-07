import { useCallback, useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import ConfirmDialog from "../../components/common/ConfirmDialog.jsx";
import StartupForm from "../../components/startup/StartupForm.jsx";
import StartupProfileView from "../../components/startup/StartupProfileView.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    getMyStartups,
    createStartup,
    updateStartup,
    deleteStartup,
} from "../../services/startup/startupService.js";
import { Rocket } from "lucide-react";

export default function StartupProfilePage() {
    const toast = useToast();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [startup, setStartup] = useState(null);
    const [mode, setMode] = useState("view"); // "view" | "create" | "edit"
    const [submitting, setSubmitting] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [deleting, setDeleting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const list = await getMyStartups();
            setStartup(Array.isArray(list) && list.length > 0 ? list[0] : null);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    async function handleCreate(payload) {
        setSubmitting(true);
        try {
            const created = await createStartup(payload);
            setStartup(created);
            setMode("view");
            toast.success("Startup profile created successfully.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setSubmitting(false);
        }
    }

    async function handleUpdate(payload) {
        if (!startup?.id) return;
        setSubmitting(true);
        try {
            const updated = await updateStartup(startup.id, payload);
            setStartup(updated);
            setMode("view");
            toast.success("Startup profile updated successfully.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDelete() {
        if (!startup?.id) return;
        setDeleting(true);
        try {
            await deleteStartup(startup.id);
            setStartup(null);
            setConfirmOpen(false);
            setMode("view");
            toast.success("Startup profile deleted successfully.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setDeleting(false);
        }
    }

    return (
        <DashboardLayout title="My Startup">
            <div className="space-y-6">
                <PageHeader
                    title="My Startup"
                    description="Create and manage your startup profile."
                />

                {loading ? (
                    <LoadingState message="Loading your startup profile…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load startup profile"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !startup && mode === "view" ? (
                    <EmptyState
                        icon={Rocket}
                        title="No startup profile yet"
                        description="Create your startup profile to unlock AI-powered insights and investor matching."
                        action={
                            <Button onClick={() => setMode("create")}>
                                Create Startup Profile
                            </Button>
                        }
                    />
                ) : mode === "create" ? (
                    <Card>
                        <h2 className="text-lg font-semibold text-navy-800">Create Startup Profile</h2>
                        <div className="mt-6">
                            <StartupForm
                                submitting={submitting}
                                onSubmit={handleCreate}
                                onCancel={() => setMode("view")}
                                submitLabel="Create Startup Profile"
                            />
                        </div>
                    </Card>
                ) : mode === "edit" ? (
                    <Card>
                        <h2 className="text-lg font-semibold text-navy-800">Edit Startup Profile</h2>
                        <div className="mt-6">
                            <StartupForm
                                initial={startup}
                                submitting={submitting}
                                onSubmit={handleUpdate}
                                onCancel={() => setMode("view")}
                                submitLabel="Save Changes"
                            />
                        </div>
                    </Card>
                ) : (
                    <StartupProfileView
                        startup={startup}
                        onEdit={() => setMode("edit")}
                        onDelete={() => setConfirmOpen(true)}
                    />
                )}
            </div>

            <ConfirmDialog
                open={confirmOpen}
                title="Delete startup profile?"
                message="Are you sure you want to delete your startup profile? This action cannot be undone."
                confirmLabel="Delete Profile"
                variant="danger"
                loading={deleting}
                onConfirm={handleDelete}
                onCancel={() => setConfirmOpen(false)}
            />
        </DashboardLayout>
    );
}