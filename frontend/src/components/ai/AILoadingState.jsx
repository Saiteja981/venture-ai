import { Loader2 } from "lucide-react";
import Card from "../common/Card.jsx";

export default function AILoadingState({ message = "Analyzing…" }) {
    return (
        <Card className="flex flex-col items-center py-16 text-center">
            <Loader2 className="h-6 w-6 animate-spin text-brand-600" />
            <p className="mt-3 text-sm text-slate-600">{message}</p>
            <p className="mt-1 text-xs text-slate-400">
                This may take a few seconds on the first request.
            </p>
        </Card>
    );
}