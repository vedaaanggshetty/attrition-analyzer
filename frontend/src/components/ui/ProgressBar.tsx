import { cx } from "../../lib/utils";

export function ProgressBar({ value, className }: { value: number; className?: string }) {
  return (
    <div className={cx("h-1.5 w-full overflow-hidden rounded-full bg-brand-900/8", className)}>
      <div
        className="h-full rounded-full bg-brand-500 transition-[width] duration-700 ease-out"
        style={{ width: `${Math.min(Math.max(value, 0), 100)}%` }}
      />
    </div>
  );
}
