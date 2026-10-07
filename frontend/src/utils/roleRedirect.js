export function redirectPathForRole(role) {
    switch (role) {
        case "STARTUP":
            return "/startup/dashboard";
        case "INVESTOR":
            return "/investor/dashboard";
        case "ADMIN":
            return "/admin/dashboard";
        default:
            return "/";
    }
}