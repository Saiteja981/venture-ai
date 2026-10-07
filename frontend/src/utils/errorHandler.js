export function messageForError(err) {
    if (!err) return "Something went wrong.";
    if (err.isNetworkError) return "Cannot reach the server. Please try again.";
    switch (err.status) {
        case 400:
            return err.message || "Invalid input. Please check the form.";
        case 401:
            return "Your session is invalid or has expired. Please log in again.";
        case 403:
            return "You are not authorized to perform this action.";
        case 404:
            return "The requested resource was not found.";
        case 409:
            return err.message || "This resource already exists.";
        case 500:
            return "The server encountered an error. Please try again later.";
        default:
            return err.message || "Something went wrong.";
    }
}