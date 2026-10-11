import { useEffect, useState } from "react";
import Button from "../common/Button.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { submitAiFeedback, getMyAiFeedback } from "../../services/api/aiApi.js";
import { Star, MessageSquare, CheckCircle2 } from "lucide-react";

const CATEGORIES = [
    { value: "USEFUL", label: "Useful" },
    { value: "INACCURATE", label: "Inaccurate" },
    { value: "IRRELEVANT", label: "Irrelevant" },
    { value: "CONFUSING", label: "Confusing" },
    { value: "OTHER", label: "Other" },
];

/**
 * Reusable feedback control for an AI execution.
 *
 * `executionId` comes from the AI response's `_execution.id` field. When the
 * backend has no execution ID (proxy persistence failed), this component
 * renders nothing — the AI result itself is still shown.
 */
export default function FeedbackControl({ executionId }) {
    const toast = useToast();
    const [loading, setLoading] = useState(true);
    const [existing, setExisting] = useState(null);
    const [rating, setRating] = useState(0);
    const [category, setCategory] = useState("");
    const [comment, setComment] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!executionId) return;
        let cancelled = false;
        (async () => {
            try {
                const data = await getMyAiFeedback(executionId);
                if (!cancelled) setExisting(data || null);
            } catch {
                // Non-critical: allow the user to submit fresh feedback
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [executionId]);

    if (!executionId) return null;

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        if (rating < 1) {
            setError("Please choose a rating from 1 to 5.");
            return;
        }
        if (!category) {
            setError("Please select a category.");
            return;
        }
        setSubmitting(true);
        try {
            const result = await submitAiFeedback(executionId, {
                rating,
                category,
                comment,
            });
            setExisting(result);
            toast.success("Thanks for your feedback.");
        } catch (err) {
            const msg = messageForError(err) || "Could not submit feedback.";
            setError(msg);
            toast.error(msg);
        } finally {
            setSubmitting(false);
        }
    }

    if (loading) {
        return (
            <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3 text-xs text-slate-400">
                Loading feedback…
            </div>
        );
    }

    if (existing) {
        return (
            <div className="rounded-lg border border-emerald-100 bg-emerald-50/60 p-3">
                <div className="flex items-start gap-3">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 flex-shrink-0 text-emerald-600" />
                    <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-semibold text-emerald-800">
                                Feedback submitted
                            </span>
                            <span className="inline-flex items-center gap-0.5">
                                {[1, 2, 3, 4, 5].map((n) => (
                                    <Star
                                        key={n}
                                        className={`h-3 w-3 ${n <= existing.rating ? "fill-amber-400 text-amber-400" : "text-slate-300"}`}
                                    />
                                ))}
                            </span>
                            <span className="rounded-full bg-white px-2 py-0.5 text-[10px] font-medium text-emerald-700">
                                {existing.category}
                            </span>
                        </div>
                        {existing.comment && (
                            <p className="mt-1 text-xs text-emerald-900/80">
                                {existing.comment}
                            </p>
                        )}
                        <p className="mt-1 text-[10px] text-emerald-700/70">
                            One feedback entry per AI result. Thank you.
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="rounded-lg border border-slate-200 bg-white p-3"
        >
            <div className="flex items-center gap-2">
                <MessageSquare className="h-4 w-4 text-slate-400" />
                <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Rate this result
                </h4>
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-4">
                <div className="flex items-center gap-1" role="radiogroup" aria-label="Rating">
                    {[1, 2, 3, 4, 5].map((n) => (
                        <button
                            key={n}
                            type="button"
                            role="radio"
                            aria-checked={rating === n}
                            aria-label={`${n} star${n === 1 ? "" : "s"}`}
                            onClick={() => setRating(n)}
                            className="rounded p-0.5 hover:bg-slate-100"
                        >
                            <Star
                                className={`h-5 w-5 ${n <= rating ? "fill-amber-400 text-amber-400" : "text-slate-300"}`}
                            />
                        </button>
                    ))}
                </div>
                <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    className="input-field text-sm sm:w-48"
                    aria-label="Feedback category"
                >
                    <option value="">Select category…</option>
                    {CATEGORIES.map((c) => (
                        <option key={c.value} value={c.value}>
                            {c.label}
                        </option>
                    ))}
                </select>
            </div>

            <textarea
                rows={2}
                maxLength={500}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Optional comment (max 500 characters)…"
                className="input-field mt-3 resize-none text-sm"
            />

            {error && (
                <p className="mt-2 text-xs text-red-600">{error}</p>
            )}

            <div className="mt-3 flex items-center justify-between">
                <span className="text-[10px] text-slate-400">
                    Feedback is subjective and not used as training data.
                </span>
                <Button type="submit" disabled={submitting}>
                    {submitting ? "Submitting…" : "Submit feedback"}
                </Button>
            </div>
        </form>
    );
}