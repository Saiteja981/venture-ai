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