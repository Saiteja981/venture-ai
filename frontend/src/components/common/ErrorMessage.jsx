export default function ErrorMessage({ children, className = "" }) {
    if (!children) return null;
    return (
        <div
            role="alert"
            className={`rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 ${className}`}
        >
            {children}
        </div>
    );
}