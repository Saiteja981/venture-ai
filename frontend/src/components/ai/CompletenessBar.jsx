export default function CompletenessBar({ label, value, tone }) {
    const safe = Math.max(0, Math.min(100, Number(value) || 0));
    const barColor = tone || (
        safe >= 70 ? "bg-emerald-500" : safe >= 40 ? "bg-amber-500" : "bg-red-500"
    );

    return (
        <div>
            <div className="flex items-center justify-between text-sm">
                <span className="font-medium text-navy-800">{label}</span>
                <span className="text-slate-500">{safe.toFixed(0)}%</span>
            </div>
            <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                    className={`h-full rounded-full ${barColor} transition-all`}
                    style={{ width: `${safe}%` }}
                />
            </div>
        </div>
    );
}