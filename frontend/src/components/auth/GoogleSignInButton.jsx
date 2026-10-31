import { GoogleLogin } from "@react-oauth/google";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { googleAuthRequest } from "../../services/auth/authService.js";
import { useAuth } from "../../hooks/useAuth.js";
import { useToast } from "../../hooks/useToast.js";
import { messageForError } from "../../utils/errorHandler.js";
import { redirectPathForRole } from "../../utils/roleRedirect.js";

/**
 * Google sign-in button.
 *
 * Flow:
 *  1. Google popup succeeds → we get a signed ID token
 *  2. POST it to /api/auth/google
 *  3. Response either:
 *      - has an `auth` payload → we log in and go to the dashboard
 *      - has `requiresRoleSelection: true` → we navigate to the role picker
 */
export default function GoogleSignInButton() {
    const navigate = useNavigate();
    const toast = useToast();
    const { login } = useAuth();
    const [loading, setLoading] = useState(false);

    async function handleSuccess(credentialResponse) {
        const rawToken = credentialResponse?.credential;
        if (!rawToken) {
            toast.error("Google sign-in failed. Please try again.");
            return;
        }
        // Constrain to base64url-safe characters — a real Google ID token
        // consists only of these. Prevents any unexpected input from reaching
        // the request layer.
        if (!/^[A-Za-z0-9_.\-]+$/.test(rawToken) || rawToken.length > 4096) {
            toast.error("Google sign-in returned a malformed token.");
            return;
        }
        setLoading(true);
        try {
            const result = await googleAuthRequest(rawToken);
            if (result.requiresRoleSelection) {
                navigate("/auth/google/complete", {
                    state: { idToken: rawToken },
                });
                return;
            }
            login(result.auth);
            toast.success("Signed in with Google.");
            navigate(redirectPathForRole(result.auth.role), { replace: true });
        } catch (err) {
            toast.error(messageForError(err) || "Google sign-in failed.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className={loading ? "pointer-events-none opacity-60" : ""}>
            <GoogleLogin
                onSuccess={handleSuccess}
                onError={() => toast.error("Google sign-in was cancelled.")}
                theme="outline"
                size="large"
                width="100%"
                text="continue_with"
                shape="rectangular"
                logo_alignment="left"
            />
        </div>
    );
}