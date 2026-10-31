import { useState } from "react";
import Card from "../common/Card.jsx";
import Button from "../common/Button.jsx";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { Download, FileText, Table } from "lucide-react";

/**
 * One row of the export center. `onExport(format)` should return a Promise
 * that resolves when the download has been triggered.
 */
export default function ExportCard({ title, description, onExport, formats = ["pdf", "csv"] }) {
    const toast = useToast();
    const [busy, setBusy] = useState(null);

    async function handle(format) {
        setBusy(format);
        try {
            await onExport(format);
        } catch (err) {
            const msg = messageForError(err) || "Export failed.";
            toast.error(msg);
        } finally {
            setBusy(null);
        }
    }

    return (
        <Card className="!p-4">
            <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                    <h4 className="text-sm font-semibold text-navy-800">{title}</h4>
                    {description && (
                        <p className="mt-1 text-xs text-slate-500">{description}</p>
                    )}
                </div>
                <div className="flex flex-shrink-0 gap-2">
                    {formats.includes("pdf") && (
                        <Button
                            variant="secondary"
                            disabled={busy !== null}
                            onClick={() => handle("pdf")}
                        >
                            <FileText className="h-4 w-4" />
                            {busy === "pdf" ? "Preparing…" : "PDF"}
                        </Button>
                    )}
                    {formats.includes("csv") && (
                        <Button
                            variant="secondary"
                            disabled={busy !== null}
                            onClick={() => handle("csv")}
                        >
                            <Table className="h-4 w-4" />
                            {busy === "csv" ? "Preparing…" : "CSV"}
                        </Button>
                    )}
                </div>
            </div>
        </Card>
    );
}