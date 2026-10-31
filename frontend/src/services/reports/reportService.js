import axiosClient from "../api/axiosClient.js";

/**
 * Fetches a report as a Blob and triggers a browser download using the
 * filename returned by the server (via Content-Disposition).
 */
async function downloadReport(url) {
    const response = await axiosClient.get(url, { responseType: "blob" });
    const disposition = response.headers["content-disposition"] || "";
    const match = disposition.match(/filename="?([^"]+)"?/);
    const filename = match ? match[1] : "report";

    const blobUrl = URL.createObjectURL(response.data);
    const a = document.createElement("a");
    a.href = blobUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(blobUrl);
}

/* ---------- Startup ---------- */

export const downloadStartupProfile = (format = "pdf") =>
    downloadReport(`/reports/startup/profile?format=${format}`);

export const downloadStartupAiAnalysis = (format = "pdf") =>
    downloadReport(`/reports/startup/ai-analysis?format=${format}`);

/* ---------- Investor ---------- */

export const downloadInvestorSavedStartups = (format = "csv") =>
    downloadReport(`/reports/investor/saved-startups?format=${format}`);

export const downloadInvestorPortfolio = (format = "csv") =>
    downloadReport(`/reports/investor/portfolio?format=${format}`);

/* ---------- Admin ---------- */

export const downloadAdminAnalytics = (format = "pdf") =>
    downloadReport(`/admin/reports/analytics?format=${format}`);

export const downloadAdminAuditLogs = (format = "csv") =>
    downloadReport(`/admin/reports/audit-logs?format=${format}`);

export const downloadAdminAiExecutions = (format = "pdf") =>
    downloadReport(`/admin/reports/ai-executions?format=${format}`);