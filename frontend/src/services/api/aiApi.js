import axios from "axios";

const AI_BASE_URL =
    import.meta.env.VITE_AI_API_BASE_URL || "http://localhost:8000";

const aiApi = axios.create({
    baseURL: AI_BASE_URL,
    headers: { "Content-Type": "application/json" },
    timeout: 60000, // BERT inference + cold model load can be slow
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

export async function predictStartup(features) {
    const { data } = await aiApi.post("/api/v1/predict/startup", features);
    return data;
}

export async function getRecommendations(investorProfile, topK = 5) {
    const { data } = await aiApi.post("/api/v1/recommendations", {
        investor: investorProfile,
        top_k: topK,
    });
    return data;
}

export async function analyzePitchDeck(text) {
    const { data } = await aiApi.post("/api/v1/analyze/pitch-deck", { text });
    return data;
}

export default aiApi;