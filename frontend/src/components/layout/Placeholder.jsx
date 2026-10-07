import Card from "../common/Card.jsx";
import { Sparkles } from "lucide-react";

export default function Placeholder({ title, description }) {
    return (
        <Card className="flex flex-col items-center justify-center py-16 text-center">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                <Sparkles className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-semibold text-navy-800">{title}</h3>
            <p className="mt-2 max-w-md text-sm text-slate-500">
                {description || "This section will be implemented in a later phase."}
            </p>
        </Card>
    );
}