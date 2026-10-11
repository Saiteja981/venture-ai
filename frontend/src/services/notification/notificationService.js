import axiosClient from "../api/axiosClient.js";

export async function getNotifications() {
    const { data } = await axiosClient.get("/notifications");
    return data;
}

export async function getUnreadCount() {
    const { data } = await axiosClient.get("/notifications/unread-count");
    return data.unread ?? 0;
}

export async function markNotificationAsRead(id) {
    const { data } = await axiosClient.patch(`/notifications/${id}/read`);
    return data;
}

export async function markAllNotificationsAsRead() {
    const { data } = await axiosClient.patch("/notifications/read-all");
    return data.updated ?? 0;
}

export async function deleteNotification(id) {
    await axiosClient.delete(`/notifications/${id}`);
}

export async function reportAiEvent(eventType) {
    try {
        const { data } = await axiosClient.post("/notifications/ai-event", { eventType });
        return data;
    } catch {
        // Non-critical — silently ignore if the notification endpoint fails.
        return null;
    }
}