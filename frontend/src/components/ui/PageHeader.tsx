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
          <p className="mb-2 text-sm font-semibold uppercase tracking-wider text-neutral-400">{eyebrow}</p>
        )}
        <h1 className="font-serif text-4xl font-semibold italic tracking-tight text-ink-900 sm:text-5xl">{title}</h1>
        {description && <p className="mt-2 max-w-2xl text-base text-neutral-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}
