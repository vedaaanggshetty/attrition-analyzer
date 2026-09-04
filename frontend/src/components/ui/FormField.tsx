import type { InputHTMLAttributes } from "react";
import { cx } from "../../lib/utils";

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export function FormField({ label, error, className, id, ...props }: FormFieldProps) {
  const inputId = id ?? label.toLowerCase().replace(/\s+/g, "-");
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm font-medium text-ink-900">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}-error` : undefined}
        className={cx(
          "shadow-control rounded-xl border bg-white px-4 py-2.5 text-sm text-ink-900 outline-none transition-all duration-200 placeholder:text-neutral-400",
          error
            ? "border-red-400 focus:border-red-500 focus:shadow-surface"
            : "border-brand-900/12 hover:border-brand-900/20 focus:border-brand-900/40 focus:shadow-surface",
          className
        )}
        {...props}
      />
      {error && (
        <p id={`${inputId}-error`} className="text-xs font-medium text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
