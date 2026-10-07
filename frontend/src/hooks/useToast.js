import { useContext } from "react";
import { NotificationContext } from "../components/common/NotificationProvider.jsx";

export function useToast() {
    const ctx = useContext(NotificationContext);
    if (ctx === null) {
        throw new Error("useToast must be used inside a NotificationProvider");
    }
    return ctx;
}