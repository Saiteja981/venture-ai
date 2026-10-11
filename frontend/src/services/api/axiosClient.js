import axios from "axios";

const BASE_URL = "http://localhost:8082/api";
export const AUTH_STORAGE_KEY = "venture_ai_auth";

const axiosClient = axios.create({
    baseURL: BASE_URL,
    headers: { "Content-Type": "application/json" },
    timeout: 20000,
});

axiosClient.interceptors.request.use((config) => {
    try {
        const raw = localStorage.getItem(AUTH_STORAGE_KEY);
        if (raw) {
            const parsed = JSON.parse(raw);
            if (parsed?.token) {
                config.headers.Authorization = `Bearer ${parsed.token}`;
            }
        }
    } catch {
        // Ignore malformed storage.
    }
    return config;
});

axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        const normalized = {
            status: error?.response?.status ?? 0,
            message:
                error?.response?.data?.message ||
                error?.response?.data?.error ||
                error?.message ||
                "Something went wrong",
            data: error?.response?.data ?? null,
            isNetworkError: !error?.response,
        };
        return Promise.reject(normalized);
    }
);

export default axiosClient;