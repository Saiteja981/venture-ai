import Card from "../common/Card.jsx";
import Button from "../common/Button.jsx";
import { AlertTriangle } from "lucide-react";

export default function AIErrorState({ message, onRetry }) {
    return (
        <Card className="flex flex-col items-center py-14 text-center">
            <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-500">
                <AlertTriangle className="h-7 w-7" />
            </div>
            <h3 className="text-lg font-semibold text-navy-800">
                AI service is currently unavailable
            </h3>
            <p className="mt-2 max-w-md text-sm text-slate-500">
                {message || "Please try again later."}
            </p>
            {onRetry && (
                <div className="mt-6">
                    <Button onClick={onRetry}>Retry</Button>
                </div>
            )}
        </Card>
    );
}