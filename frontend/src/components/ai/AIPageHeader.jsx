export default function AIPageHeader({ title, description, icon: Icon, badge }) {
    return (
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-4">
                {Icon && (
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                        <Icon className="h-5 w-5" />
                    </div>
                )}
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight text-navy-800">
                        {title}
                    </h1>
                    {description && (
                        <p className="mt-1 max-w-2xl text-sm text-slate-500">
                            {description}
                        </p>
                    )}
                </div>
            </div>
            {badge && <div className="flex items-center gap-2">{badge}</div>}
        </div>
    );
}