import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import { AUTH_STORAGE_KEY } from "../services/api/axiosClient.js";

export const AuthContext = createContext(null);

const EMPTY = {
    token: null,
    userId: null,
    name: null,
    email: null,
    role: null,
};

function readFromStorage() {
    try {
        const raw = localStorage.getItem(AUTH_STORAGE_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (!parsed?.token || !parsed?.role) return null;
        return {
            token: parsed.token,
            userId: parsed.userId ?? null,
            name: parsed.name ?? null,
            email: parsed.email ?? null,
            role: parsed.role,
        };
    } catch {
        return null;
    }
}

export function AuthProvider({ children }) {
    const [auth, setAuth] = useState(() => readFromStorage() ?? EMPTY);
    const [initializing, setInitializing] = useState(true);

    useEffect(() => {
        setInitializing(false);
    }, []);

    const login = useCallback((authResponse) => {
        const payload = {
            token: authResponse.token,
            userId: authResponse.userId ?? null,
            name: authResponse.name ?? null,
            email: authResponse.email ?? null,
            role: authResponse.role ?? null,
        };
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(payload));
        setAuth(payload);
    }, []);

    const logout = useCallback(() => {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        setAuth(EMPTY);
    }, []);

    const value = useMemo(
        () => ({
            ...auth,
            isAuthenticated: Boolean(auth.token && auth.role),
            initializing,
            login,
            logout,
        }),
        [auth, initializing, login, logout]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}