import { useCallback, useEffect, useState } from "react";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import PageHeader from "../../components/common/PageHeader.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import ConfirmDialog from "../../components/common/ConfirmDialog.jsx";
import InvestorForm from "../../components/investor/InvestorForm.jsx";
import InvestorProfileView from "../../components/investor/InvestorProfileView.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    getMyInvestorProfile,
    createInvestorProfile,
    updateInvestorProfile,
    deleteInvestorProfile,
} from "../../services/investor/investorService.js";
import { UserCircle2 } from "lucide-react";

const NOT_FOUND = 404;

export default function InvestorProfilePage() {
    const toast = useToast();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [investor, setInvestor] = useState(null);
    const [mode, setMode] = useState("view");
    const [submitting, setSubmitting] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [deleting, setDeleting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await getMyInvestorProfile();
            setInvestor(data);
        } catch (err) {
            if (err?.status === NOT_FOUND) {
                setInvestor(null);
            } else {
                setError(messageForError(err));
            }
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
            const created = await createInvestorProfile(payload);
            setInvestor(created);
            setMode("view");
            toast.success("Investor profile created successfully.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setSubmitting(false);
        }
    }

    async function handleUpdate(payload) {
        setSubmitting(true);
        try {
            const updated = await updateInvestorProfile(payload);
            setInvestor(updated);
            setMode("view");
            toast.success("Investor profile updated successfully.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDelete() {
        setDeleting(true);
        try {
            await deleteInvestorProfile();
            setInvestor(null);
            setConfirmOpen(false);
            setMode("view");
            toast.success("Investor profile deleted successfully.");
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setDeleting(false);
        }
    }

    return (
        <DashboardLayout title="My Profile">
            <div className="space-y-6">
                <PageHeader
                    title="My Profile"
                    description="Manage your investor profile and preferences."
                />

                {loading ? (
                    <LoadingState message="Loading your investor profile…" />
                ) : error ? (
                    <ErrorState
                        title="Could not load investor profile"
                        message={error}
                        action={<Button onClick={load}>Retry</Button>}
                    />
                ) : !investor && mode === "view" ? (
                    <EmptyState
                        icon={UserCircle2}
                        title="No investor profile yet"
                        description="Create your investor profile to discover startups matched to your investment preferences."
                        action={
                            <Button onClick={() => setMode("create")}>
                                Create Investor Profile
                            </Button>
                        }
                    />
                ) : mode === "create" ? (
                    <Card>
                        <h2 className="text-lg font-semibold text-navy-800">
                            Create Investor Profile
                        </h2>
                        <div className="mt-6">
                            <InvestorForm
                                submitting={submitting}
                                onSubmit={handleCreate}
                                onCancel={() => setMode("view")}
                                submitLabel="Create Investor Profile"
                            />
                        </div>
                    </Card>
                ) : mode === "edit" ? (
                    <Card>
                        <h2 className="text-lg font-semibold text-navy-800">
                            Edit Investor Profile
                        </h2>
                        <div className="mt-6">
                            <InvestorForm
                                initial={investor}
                                submitting={submitting}
                                onSubmit={handleUpdate}
                                onCancel={() => setMode("view")}
                                submitLabel="Save Changes"
                            />
                        </div>
                    </Card>
                ) : (
                    <InvestorProfileView
                        investor={investor}
                        onEdit={() => setMode("edit")}
                        onDelete={() => setConfirmOpen(true)}
                    />
                )}
            </div>

            <ConfirmDialog
                open={confirmOpen}
                title="Delete investor profile?"
                message="Are you sure you want to delete your investor profile? This action cannot be undone."
                confirmLabel="Delete Profile"
                variant="danger"
                loading={deleting}
                onConfirm={handleDelete}
                onCancel={() => setConfirmOpen(false)}
            />
        </DashboardLayout>
    );
}