import Card from "./Card.jsx";

export default function EmptyState({ icon: Icon, title, description, action }) {
    return (
        <Card className="flex flex-col items-center py-14 text-center">
            {Icon && (
                <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                    <Icon className="h-7 w-7" />
                </div>
            )}
            <h3 className="text-lg font-semibold text-navy-800">{title}</h3>
            {description && (
                <p className="mt-2 max-w-md text-sm text-slate-500">{description}</p>
            )}
            {action && <div className="mt-6">{action}</div>}
        </Card>
    );
}