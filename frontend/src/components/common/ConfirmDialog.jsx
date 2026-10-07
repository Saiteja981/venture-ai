import Button from "./Button.jsx";

export default function ConfirmDialog({
                                          open,
                                          title,
                                          message,
                                          confirmLabel = "Confirm",
                                          cancelLabel = "Cancel",
                                          variant = "danger",
                                          loading = false,
                                          onConfirm,
                                          onCancel,
                                      }) {
    if (!open) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
                className="absolute inset-0 bg-navy-900/60"
                onClick={loading ? undefined : onCancel}
                aria-hidden="true"
            />
            <div className="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <h3 className="text-lg font-semibold text-navy-800">{title}</h3>
                <p className="mt-2 text-sm text-slate-600">{message}</p>
                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onCancel} disabled={loading}>
                        {cancelLabel}
                    </Button>
                    <Button variant={variant} onClick={onConfirm} disabled={loading}>
                        {loading ? "Please wait…" : confirmLabel}
                    </Button>
                </div>
            </div>
        </div>
    );
}