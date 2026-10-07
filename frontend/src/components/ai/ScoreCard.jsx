import Card from "../common/Card.jsx";

function toneForScore(score) {
    if (score >= 70) return { bar: "bg-emerald-500", text: "text-emerald-700", label: "Strong" };
    if (score >= 40) return { bar: "bg-amber-500", text: "text-amber-700", label: "Moderate" };
    return { bar: "bg-red-500", text: "text-red-700", label: "Low" };
}

export default function ScoreCard({ score, label = "AI Potential Score" }) {
    const safe = Math.max(0, Math.min(100, Number(score) || 0));
    const tone = toneForScore(safe);

    return (
        <Card>
            <div className="flex items-center justify-between">
                <h3 className="text-base font-semibold text-navy-800">{label}</h3>
                <span className={`text-sm font-medium ${tone.text}`}>{tone.label}</span>
            </div>

            <div className="mt-4 flex items-baseline gap-2">
        <span className="text-4xl font-semibold text-navy-800">
          {safe.toFixed(1)}
        </span>
                <span className="text-sm text-slate-400">/ 100</span>
            </div>

            <div className="mt-5">
                <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                    <div
                        className={`h-full rounded-full ${tone.bar} transition-all`}
                        style={{ width: `${safe}%` }}
                    />
                </div>
            </div>
        </Card>
    );
}