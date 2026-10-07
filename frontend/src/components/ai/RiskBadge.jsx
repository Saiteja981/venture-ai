const TONES = {
    LOW: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    MEDIUM: "bg-amber-50 text-amber-700 ring-amber-200",
    HIGH: "bg-red-50 text-red-700 ring-red-200",
};

export default function RiskBadge({ category }) {
    const value = String(category || "").toUpperCase();
    const cls = TONES[value] ?? "bg-slate-100 text-slate-700 ring-slate-200";
    return (
        <span
            className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ring-1 ring-inset ${cls}`}
        >
      {value || "UNKNOWN"}
    </span>
    );
}