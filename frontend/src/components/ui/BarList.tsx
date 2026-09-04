import type { AttritionAnalysis } from "../../types";
import { ProgressBar } from "./ProgressBar";

export function BarList({ data }: { data: AttritionAnalysis[] }) {
  const max = Math.max(...data.map((d) => d.attritionRate), 1);
  return (
    <ul className="flex flex-col gap-4">
      {data.map((row) => (
        <li key={row.groupLabel} className="flex flex-col gap-1.5">
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="font-medium text-ink-900">{row.groupLabel}</span>
            <span className="shrink-0 text-neutral-500">
              {row.attritionRate}%{" "}
              <span className="text-neutral-400">
                ({row.attritionCount}/{row.totalEmployees})
              </span>
            </span>
          </div>
          <ProgressBar value={(row.attritionRate / max) * 100} />
        </li>
      ))}
    </ul>
  );
}
