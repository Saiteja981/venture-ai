export default function Badge({ children, tone = "brand", className = "" }) {
    const tones = {
        brand: "bg-brand-50 text-brand-700 ring-brand-200",
        navy: "bg-slate-100 text-navy-800 ring-slate-200",
        success: "bg-emerald-50 text-emerald-700 ring-emerald-200",
        warning: "bg-amber-50 text-amber-700 ring-amber-200",
        danger: "bg-red-50 text-red-700 ring-red-200",
        accent: "bg-cyan-50 text-cyan-700 ring-cyan-200",
    };
    return (
        <span
            className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5
                  text-xs font-medium ring-1 ring-inset ${tones[tone] ?? tones.brand} ${className}`}
        >
      {children}
    </span>
    );
}