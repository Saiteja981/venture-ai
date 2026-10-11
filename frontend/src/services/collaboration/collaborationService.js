import axiosClient from "../api/axiosClient.js";

export async function createCollaborationRequest(startupId, message) {
    const { data } = await axiosClient.post("/collaborations", { startupId, message });
    return data;
}

export async function getInvestorCollaborations() {
    const { data } = await axiosClient.get("/collaborations/investor");
    return data;
}

export async function getStartupCollaborations() {
    const { data } = await axiosClient.get("/collaborations/startup");
    return data;
}

export async function getCollaborationById(id) {
    const { data } = await axiosClient.get(`/collaborations/${id}`);
    return data;
}

export async function acceptCollaboration(id) {
    const { data } = await axiosClient.patch(`/collaborations/${id}/accept`);
    return data;
}

export async function rejectCollaboration(id) {
    const { data } = await axiosClient.patch(`/collaborations/${id}/reject`);
    return data;
}

export async function withdrawCollaboration(id) {
    const { data } = await axiosClient.patch(`/collaborations/${id}/withdraw`);
    return data;
}