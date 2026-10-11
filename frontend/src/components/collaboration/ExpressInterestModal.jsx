import { useState } from "react";
import Button from "../common/Button.jsx";
import { X, Send } from "lucide-react";

const DEFAULT_MESSAGE =
    "I am interested in learning more about your startup and exploring potential collaboration opportunities.";

export default function ExpressInterestModal({
                                                 startup,
                                                 submitting,
                                                 error,
                                                 onSubmit,
                                                 onClose,
                                             }) {
    const [message, setMessage] = useState(DEFAULT_MESSAGE);

    if (!startup) return null;

    function handleSubmit(e) {
        e.preventDefault();
        onSubmit(message);
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
                className="absolute inset-0 bg-navy-900/60"
                onClick={submitting ? undefined : onClose}
                aria-hidden="true"
            />
            <div className="relative w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
                <div className="flex items-start justify-between">
                    <div>
                        <h2 className="text-xl font-semibold text-navy-800">
                            Express Interest
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Send a collaboration request to{" "}
                            <span className="font-medium text-navy-800">
                                {startup.companyName}
                            </span>
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={submitting}
                        className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100"
                        aria-label="Close"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="mt-5 space-y-4">
                    <div>
                        <label
                            htmlFor="collab-message"
                            className="block text-sm font-medium text-slate-700"
                        >
                            Message (optional)
                        </label>
                        <textarea
                            id="collab-message"
                            rows={5}
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            maxLength={1000}
                            className="input-field mt-1"
                        />
                        <div className="mt-1 text-right text-xs text-slate-400">
                            {message.length}/1000
                        </div>
                    </div>

                    {error && (
                        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                            {error}
                        </div>
                    )}

                    <div className="flex justify-end gap-2 border-t border-slate-200 pt-4">
                        <Button
                            type="button"
                            variant="secondary"
                            onClick={onClose}
                            disabled={submitting}
                        >
                            Cancel
                        </Button>
                        <Button type="submit" disabled={submitting}>
                            <Send className="h-4 w-4" />
                            {submitting ? "Sending…" : "Send Request"}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}