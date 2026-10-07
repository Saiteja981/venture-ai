export default function Button({
                                   children,
                                   variant = "primary",
                                   type = "button",
                                   disabled = false,
                                   className = "",
                                   ...rest
                               }) {
    const variantClass =
        variant === "secondary"
            ? "btn-secondary"
            : variant === "danger"
                ? "btn-danger"
                : "btn-primary";

    return (
        <button
            type={type}
            disabled={disabled}
            className={`${variantClass} ${className}`}
            {...rest}
        >
            {children}
        </button>
    );
}