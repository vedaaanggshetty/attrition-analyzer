import type { ReactNode } from "react";

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div>
        {eyebrow && (
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-neutral-400">{eyebrow}</p>
        )}
        <h1 className="font-display text-3xl font-semibold tracking-tight text-brand-900 sm:text-4xl">{title}</h1>
        {description && <p className="mt-2 max-w-2xl text-sm text-neutral-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}
