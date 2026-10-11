import axiosClient from "../api/axiosClient.js";

export async function listUsers({ page = 0, size = 20, search, role, status } = {}) {
    const params = { page, size };
    if (search) params.search = search;
    if (role) params.role = role;
    if (status) params.status = status;
    const { data } = await axiosClient.get("/admin/users", { params });
    return data;
}

export async function getUser(id) {
    const { data } = await axiosClient.get(`/admin/users/${id}`);
    return data;
}

export async function updateUserStatus(id, status) {
    const { data } = await axiosClient.patch(`/admin/users/${id}/status`, { status });
    return data;
}

export async function listAuditLogs({ page = 0, size = 20, action, outcome, actorId } = {}) {
    const params = { page, size };
    if (action) params.action = action;
    if (outcome) params.outcome = outcome;
    if (actorId) params.actorId = actorId;
    const { data } = await axiosClient.get("/admin/audit-logs", { params });
    return data;
}
/* ---------- Phase 24: Startups ---------- */

export async function listStartups({ page = 0, size = 20, search, industry, stage } = {}) {
    const params = { page, size };
    if (search) params.search = search;
    if (industry) params.industry = industry;
    if (stage) params.stage = stage;
    const { data } = await axiosClient.get("/admin/startups", { params });
    return data;
}

export async function getStartup(id) {
    const { data } = await axiosClient.get(`/admin/startups/${id}`);
    return data;
}

/* ---------- Phase 24: Investors ---------- */

export async function listInvestors({ page = 0, size = 20, search, investorType, riskPreference } = {}) {
    const params = { page, size };
    if (search) params.search = search;
    if (investorType) params.investorType = investorType;
    if (riskPreference) params.riskPreference = riskPreference;
    const { data } = await axiosClient.get("/admin/investors", { params });
    return data;
}

export async function getInvestor(id) {
    const { data } = await axiosClient.get(`/admin/investors/${id}`);
    return data;
}

/* ---------- Phase 24: Stats + audit ---------- */

export async function getAdminStats() {
    const { data } = await axiosClient.get("/admin/stats");
    return data;
}

export async function listRecentAuditLogs(size = 5) {
    const { data } = await axiosClient.get("/admin/audit-logs", {
        params: { page: 0, size },
    });
    return data;
}