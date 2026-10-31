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

/* ---------- Phase 27: Google sign-in ---------- */

/**
 * Exchange a Google ID token for either:
 *  - a full auth payload (existing Google user, or new user with role), or
 *  - { requiresRoleSelection: true } when a new user needs to pick a role.
 *
 * The backend verifies the token's signature, audience, and issuer — the
 * frontend never trusts the token's contents directly.
 */
export async function googleAuthRequest(idToken, role = null) {
    const body = { idToken };
    if (role) body.role = role;
    const response = await axiosClient.post("/auth/google", body);
    return response.data;
}