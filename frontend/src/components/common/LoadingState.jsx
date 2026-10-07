import { Loader2 } from "lucide-react";

export default function LoadingState({ message = "Loading…" }) {
    return (
        <div className="flex flex-col items-center justify-center py-20 text-slate-500">
            <Loader2 className="h-6 w-6 animate-spin text-brand-600" />
            <p className="mt-3 text-sm">{message}</p>
        </div>
    );
}