import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useAuth } from "../hooks/useAuth.js";
import { getUnreadCount } from "../services/notification/notificationService.js";

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
    const { isAuthenticated } = useAuth();
    const [unreadCount, setUnreadCount] = useState(0);

    const refresh = useCallback(async () => {
        if (!isAuthenticated) {
            setUnreadCount(0);
            return;
        }
        try {
            const n = await getUnreadCount();
            setUnreadCount(n);
        } catch {
            setUnreadCount(0);
        }
    }, [isAuthenticated]);

    useEffect(() => {
        refresh();
    }, [refresh]);

    const value = useMemo(
        () => ({ unreadCount, refresh, setUnreadCount }),
        [unreadCount, refresh]
    );

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotificationContext() {
    const ctx = useContext(NotificationContext);
    if (ctx === null) {
        throw new Error("useNotificationContext must be used inside a NotificationProvider");
    }
    return ctx;
}