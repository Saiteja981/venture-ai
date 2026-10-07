import { createContext, useCallback, useMemo, useState } from "react";
import Notification from "./Notification.jsx";

export const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const dismiss = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const push = useCallback((message, tone = "success", duration = 4000) => {
        const id = Date.now() + Math.random();
        setToasts((prev) => [...prev, { id, message, tone }]);
        if (duration > 0) {
            setTimeout(() => dismiss(id), duration);
        }
        return id;
    }, [dismiss]);

    const value = useMemo(
        () => ({
            success: (msg) => push(msg, "success"),
            error: (msg) => push(msg, "error", 6000),
            info: (msg) => push(msg, "info"),
        }),
        [push]
    );

    return (
        <NotificationContext.Provider value={value}>
            {children}
            <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-80 flex-col gap-2">
                {toasts.map((t) => (
                    <Notification
                        key={t.id}
                        tone={t.tone}
                        onClose={() => dismiss(t.id)}
                    >
                        {t.message}
                    </Notification>
                ))}
            </div>
        </NotificationContext.Provider>
    );
}