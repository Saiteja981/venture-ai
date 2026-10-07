import Card from "../common/Card.jsx";

/**
 * Wrapper for any recharts-based chart.
 * Renders a titled Card with a fixed-height chart area.
 */
export default function ChartCard({
                                      title,
                                      subtitle,
                                      height = 260,
                                      children,
                                      action,
                                  }) {
    return (
        <Card>
            <div className="flex items-start justify-between">
                <div>
                    <h3 className="text-base font-semibold text-navy-800">{title}</h3>
                    {subtitle && (
                        <p className="mt-1 text-xs text-slate-500">{subtitle}</p>
                    )}
                </div>
                {action}
            </div>
            <div className="mt-4" style={{ height }}>
                {children}
            </div>
        </Card>
    );
}