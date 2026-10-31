import axiosClient from "../api/axiosClient.js";

export async function getPreferences() {
    const { data } = await axiosClient.get("/settings/preferences");
    return data;
}

export async function updatePreferences(prefs) {
    const { data } = await axiosClient.put("/settings/preferences", prefs);
    return data;
}

export async function changePassword(currentPassword, newPassword) {
    await axiosClient.post("/settings/change-password", {
        currentPassword,
        newPassword,
    });
}