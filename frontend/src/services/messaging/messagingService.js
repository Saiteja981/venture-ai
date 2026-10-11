import axiosClient from "../api/axiosClient.js";

export async function getConversations() {
    const { data } = await axiosClient.get("/conversations");
    return data;
}

export async function createOrGetConversation(collaborationId) {
    const { data } = await axiosClient.post(`/collaborations/${collaborationId}/conversation`);
    return data;
}

export async function getConversationMessages(conversationId) {
    const { data } = await axiosClient.get(`/conversations/${conversationId}/messages`);
    return data;
}

export async function sendMessage(conversationId, content) {
    const { data } = await axiosClient.post(`/conversations/${conversationId}/messages`, { content });
    return data;
}

export async function markConversationAsRead(conversationId) {
    const { data } = await axiosClient.patch(`/conversations/${conversationId}/read`);
    return data;
}