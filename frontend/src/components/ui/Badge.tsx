import type { HTMLAttributes } from "react";
import { cx } from "../../lib/utils";
import type { AttritionRisk } from "../../types";

const RISK_STYLES: Record<AttritionRisk, string> = {
  Low: "bg-brand-50 text-brand-900",
  Medium: "bg-amber-100 text-amber-700",
  High: "bg-red-100 text-red-700",
};

const RISK_DOT_STYLES: Record<AttritionRisk, string> = {
  Low: "bg-brand-500",
  Medium: "bg-amber-500",
  High: "bg-red-500",
};

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cx(
        "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold tracking-wide",
        className
      )}
      {...props}
    />
  );
}

export function RiskBadge({ risk }: { risk: AttritionRisk }) {
  return (
    <Badge className={cx("gap-1.5", RISK_STYLES[risk])}>
      <span className={cx("h-1.5 w-1.5 rounded-full", RISK_DOT_STYLES[risk])} />
      {risk} risk
    </Badge>
  );
}
