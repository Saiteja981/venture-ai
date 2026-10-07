import axiosClient from "../api/axiosClient.js";

export async function loginRequest({ email, password }) {
    const response = await axiosClient.post("/auth/login", { email, password });
    return response.data;
}

export async function registerRequest({ name, email, password, role }) {
    const response = await axiosClient.post("/auth/register", {
        name,
        email,
        password,
        role,
    });
    return response.data;
}