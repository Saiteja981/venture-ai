export default function TextareaField({
                                          label,
                                          name,
                                          value,
                                          onChange,
                                          required = false,
                                          error,
                                          helpText,
                                          rows = 4,
                                          maxLength,
                                          ...rest
                                      }) {
    return (
        <div>
            <label htmlFor={name} className="block text-sm font-medium text-slate-700">
                {label} {required && <span className="text-red-500">*</span>}
            </label>
            <textarea
                id={name}
                name={name}
                rows={rows}
                value={value ?? ""}
                onChange={onChange}
                required={required}
                maxLength={maxLength}
                className={`input-field mt-1 ${error ? "border-red-400 focus:border-red-500 focus:ring-red-200" : ""}`}
                {...rest}
            />
            <div className="mt-1 flex justify-between">
                {helpText && !error && (
                    <p className="text-xs text-slate-500">{helpText}</p>
                )}
                {maxLength && (
                    <p className="text-xs text-slate-400">
                        {(value ?? "").length}/{maxLength}
                    </p>
                )}
            </div>
            {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
        </div>
    );
}