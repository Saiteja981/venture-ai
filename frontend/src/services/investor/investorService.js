import axiosClient from "../api/axiosClient.js";

export async function getMyInvestorProfile() {
    const { data } = await axiosClient.get("/investors/me");
    return data;
}

export async function createInvestorProfile(payload) {
    const { data } = await axiosClient.post("/investors", payload);
    return data;
}

export async function updateInvestorProfile(payload) {
    const { data } = await axiosClient.put("/investors/me", payload);
    return data;
}

export async function deleteInvestorProfile() {
    await axiosClient.delete("/investors/me");
}
export async function saveStartup(startupId) {
    const { data } = await axiosClient.post(`/investors/me/saved-startups/${startupId}`);
    return data;
}

export async function getSavedStartups() {
    const { data } = await axiosClient.get("/investors/me/saved-startups");
    return data;
}

export async function removeSavedStartup(startupId) {
    await axiosClient.delete(`/investors/me/saved-startups/${startupId}`);
}

export async function getSavedStartupStatus(startupId) {
    const { data } = await axiosClient.get(`/investors/me/saved-startups/${startupId}/status`);
    return data.saved ?? false;
}

export async function getInvestorPortfolio() {
    const { data } = await axiosClient.get("/investors/me/portfolio");
    return data;
}