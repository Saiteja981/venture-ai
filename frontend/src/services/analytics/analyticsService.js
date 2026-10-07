import axiosClient from "../api/axiosClient.js";
import aiApi from "../api/aiApi.js";

/**
 * Admin platform-wide stats (users, startups, investors + distributions).
 * Requires ADMIN role — enforced by SecurityConfig + @PreAuthorize.
 */
export async function getAdminStats() {
    const { data } = await axiosClient.get("/admin/stats");
    return data;
}

/**
 * Read-only model metadata + evaluation metrics from the FastAPI service.
 * Public endpoint — no auth header needed.
 */
export async function getModelInfo() {
    const { data } = await aiApi.get("/api/v1/model-info");
    return data;
}