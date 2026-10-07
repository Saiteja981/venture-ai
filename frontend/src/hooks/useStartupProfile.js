import { useCallback, useEffect, useState } from "react";
import { getMyStartups } from "../services/startup/startupService.js";
import { messageForError } from "../utils/errorHandler.js";

export function useStartupProfile() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [startup, setStartup] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const list = await getMyStartups();
            setStartup(Array.isArray(list) && list.length > 0 ? list[0] : null);
        } catch (err) {
            setError(messageForError(err));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    return { loading, error, startup, reload: load };
}