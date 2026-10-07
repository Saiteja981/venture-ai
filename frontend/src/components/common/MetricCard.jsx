import Card from "./Card.jsx";

export default function MetricCard({ label, value, hint, icon: Icon, tone = "brand" }) {
    const toneClasses = {
        brand: "bg-brand-50 text-brand-600",
        accent: "bg-cyan-50 text-cyan-600",
        success: "bg-emerald-50 text-emerald-600",
        warning: "bg-amber-50 text-amber-600",
        danger: "bg-red-50 text-red-600",
        navy: "bg-slate-100 text-navy-800",
    }[tone];

    return (
        <Card hover className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
                <span className="text-sm text-slate-500">{label}</span>
                {Icon && (
                    <span className={`flex h-8 w-8 items-center justify-center rounded-lg ${toneClasses}`}>
            <Icon className="h-4 w-4" />
          </span>
                )}
            </div>
            <div>
                <div className="text-2xl font-semibold text-navy-800">{value}</div>
                {hint && <div className="mt-0.5 text-xs text-slate-400">{hint}</div>}
            </div>
        </Card>
    );
}