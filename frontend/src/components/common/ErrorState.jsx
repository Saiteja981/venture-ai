import Card from "./Card.jsx";
import { AlertTriangle } from "lucide-react";

export default function ErrorState({ title = "Something went wrong", message, action }) {
    return (
        <Card className="flex flex-col items-center py-14 text-center">
            <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-500">
                <AlertTriangle className="h-7 w-7" />
            </div>
            <h3 className="text-lg font-semibold text-navy-800">{title}</h3>
            {message && (
                <p className="mt-2 max-w-md text-sm text-slate-500">{message}</p>
            )}
            {action && <div className="mt-6">{action}</div>}
        </Card>
    );
}