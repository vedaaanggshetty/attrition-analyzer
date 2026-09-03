import { forwardRef } from "react";
import type { HTMLAttributes } from "react";
import { cx } from "../../lib/utils";

export const Card = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(function Card(
  { className, ...props },
  ref
) {
  return (
    <div
      ref={ref}
      className={cx(
        "rounded-2xl border border-brand-900/10 bg-white transition-all duration-300 hover:border-brand-900/20",
        className
      )}
      {...props}
    />
  );
});
