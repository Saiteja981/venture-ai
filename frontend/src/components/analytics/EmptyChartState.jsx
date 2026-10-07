import { BarChart3 } from "lucide-react";

export default function EmptyChartState({
                                            message = "Not enough data to display this chart yet.",
                                        }) {
    return (
        <div className="flex h-full flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50 text-center">
            <BarChart3 className="h-6 w-6 text-slate-400" />
            <p className="mt-2 max-w-xs text-xs text-slate-500">{message}</p>
        </div>
    );
}