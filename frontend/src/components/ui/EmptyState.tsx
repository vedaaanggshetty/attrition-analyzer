import type { ReactNode } from "react";

export function EmptyState({
  title,
  description,
  icon,
  action,
}: {
  title: string;
  description?: string;
  icon?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-brand-900/15 px-6 py-16 text-center">
      {icon && <div className="text-neutral-400">{icon}</div>}
      <p className="text-base font-semibold text-brand-900">{title}</p>
      {description && <p className="max-w-xs text-sm text-neutral-500">{description}</p>}
      {action}
    </div>
  );
}
