export default function FormField({
                                      label,
                                      name,
                                      type = "text",
                                      value,
                                      onChange,
                                      required = false,
                                      error,
                                      helpText,
                                      placeholder,
                                      ...rest
                                  }) {
    return (
        <div>
            <label htmlFor={name} className="block text-sm font-medium text-slate-700">
                {label} {required && <span className="text-red-500">*</span>}
            </label>
            <input
                id={name}
                name={name}
                type={type}
                value={value ?? ""}
                onChange={onChange}
                required={required}
                placeholder={placeholder}
                className={`input-field mt-1 ${error ? "border-red-400 focus:border-red-500 focus:ring-red-200" : ""}`}
                {...rest}
            />
            {helpText && !error && (
                <p className="mt-1 text-xs text-slate-500">{helpText}</p>
            )}
            {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
        </div>
    );
}