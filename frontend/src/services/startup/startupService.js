import axiosClient from "../api/axiosClient.js";

export async function getMyStartups() {
    const { data } = await axiosClient.get("/startups/my");
    return data;
}

export async function getMyStartup(id) {
    const { data } = await axiosClient.get(`/startups/${id}`);
    return data;
}

export async function createStartup(payload) {
    const { data } = await axiosClient.post("/startups", payload);
    return data;
}

export async function updateStartup(id, payload) {
    const { data } = await axiosClient.put(`/startups/${id}`, payload);
    return data;
}

export async function deleteStartup(id) {
    await axiosClient.delete(`/startups/${id}`);
}
export async function discoverStartups(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
            query.append(key, value);
        }
    });
    const qs = query.toString();
    const { data } = await axiosClient.get(`/startups/discover${qs ? "?" + qs : ""}`);
    return data;
}