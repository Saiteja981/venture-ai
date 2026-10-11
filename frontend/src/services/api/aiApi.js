import axios from "axios";
import axiosClient from "./axiosClient.js";

/* ============================================================
 * Direct FastAPI instance — preserved as the default export for
 * backward compatibility with existing callers (e.g. analyticsService
 * for /api/v1/model-info). New AI feature calls should use the
 * backend proxy via axiosClient instead.
 * ============================================================ */

const AI_BASE_URL =
    import.meta.env.VITE_AI_API_BASE_URL || "http://localhost:8000";

const aiApi = axios.create({
    baseURL: AI_BASE_URL,
    headers: { "Content-Type": "application/json" },
    timeout: 60000,
});

aiApi.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error?.response?.status ?? 0;
        const detail =
            error?.response?.data?.detail ||
            error?.response?.data?.message ||
            error?.message ||
            "AI service error";
        return Promise.reject({
            status,
            message: typeof detail === "string" ? detail : "AI service error",
            isNetworkError: !error?.response,
            isTimeout: error?.code === "ECONNABORTED",
        });
    }
);

/* ============================================================
 * Backend-proxied AI calls (Phase 24).
 *
 * Paths are relative to axiosClient's base URL, which already
 * includes `/api`. Do NOT re-prefix with /api — that produces
 * /api/api/... and 404s.
 * ============================================================ */

export async function predictStartup(features, explain = false) {
    const path = explain
        ? "/ai/predict-startup?explain=true"
        : "/ai/predict-startup";
    const { data } = await axiosClient.post(path, features);
    return data;
}

export async function getRecommendations(investorProfile, topK = 5, explain = true) {
    const path = explain
        ? "/ai/recommendations?explain=true"
        : "/ai/recommendations";
    const { data } = await axiosClient.post(path, {
        investor: investorProfile,
        top_k: topK,
    });
    return data;
}

export async function getInvestorMatches(startupProfile, topK = 5, explain = true) {
    const path = explain
        ? "/ai/investor-matches?explain=true"
        : "/ai/investor-matches";
    const { data } = await axiosClient.post(path, {
        startup: startupProfile,
        top_k: topK,
    });
    return data;
}

export async function analyzePitchDeck(text) {
    const { data } = await axiosClient.post("/ai/analyze-pitch-deck", { text });
    return data;
}

/* ---------- Feedback ---------- */

export async function submitAiFeedback(executionId, { rating, category, comment }) {
    const body = { rating, category };
    if (comment && comment.trim()) body.comment = comment.trim();
    const { data } = await axiosClient.post(
        `/ai/executions/${executionId}/feedback`, body
    );
    return data;
}

/** Returns existing feedback or null when none has been submitted yet. */
export async function getMyAiFeedback(executionId) {
    try {
        const { data } = await axiosClient.get(
            `/ai/executions/${executionId}/feedback`
        );
        return data;
    } catch (err) {
        if (err?.status === 204 || err?.response?.status === 204) return null;
        throw err;
    }
}

/* ============================================================
 * Default export — legacy FastAPI-direct instance.
 * ============================================================ */

export default aiApi;