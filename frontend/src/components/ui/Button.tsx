import type { ButtonHTMLAttributes } from "react";
import { cx } from "../../lib/utils";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost";
  size?: "sm" | "md" | "lg";
}

const VARIANT_STYLES: Record<NonNullable<ButtonProps["variant"]>, string> = {
  primary:
    "bg-brand-900 text-white hover:bg-brand-700 hover:-translate-y-px hover:shadow-[0_4px_20px_rgba(0,0,0,0.35)] active:scale-[0.98]",
  secondary: "shadow-control bg-white text-brand-900 border border-brand-900/15 hover:border-brand-900/40 hover:shadow-surface",
  ghost: "text-brand-900 hover:bg-brand-50",
};

const SIZE_STYLES: Record<NonNullable<ButtonProps["size"]>, string> = {
  sm: "px-4 py-2 text-sm",
  md: "px-5 py-2.5 text-sm",
  lg: "px-7 py-3.5 text-base",
};

export function Button({ className, variant = "primary", size = "md", ...props }: ButtonProps) {
  return (
    <button
      className={cx(
        "inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-50",
        VARIANT_STYLES[variant],
        SIZE_STYLES[size],
        className
      )}
      {...props}
    />
  );
}
