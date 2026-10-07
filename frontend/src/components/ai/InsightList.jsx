import Card from "../common/Card.jsx";

export default function InsightList({ title, items, emptyText = "None." }) {
    return (
        <Card>
            <h3 className="text-base font-semibold text-navy-800">{title}</h3>
            {items.length === 0 ? (
                <p className="mt-3 text-sm text-slate-500">{emptyText}</p>
            ) : (
                <ul className="mt-3 space-y-2">
                    {items.map((item, i) => (
                        <li
                            key={i}
                            className="flex items-start gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700"
                        >
                            <span className="mt-1 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-amber-500" />
                            <span>{item}</span>
                        </li>
                    ))}
                </ul>
            )}
        </Card>
    );
}