import type { HTMLAttributes } from "react";
import { cx } from "../../lib/utils";

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

/**
 * Reflects the employee's actual `attrition` field from the Survey API
 * ("Yes"/"No") - not a derived score. This is the same value US-11–US-16
 * aggregate over on the backend (attritionCount / totalEmployees per group).
 */
export function AttritionBadge({ attrition }: { attrition: "Yes" | "No" }) {
  return attrition === "Yes" ? (
    <Badge className="gap-1.5 bg-red-100 text-red-700">
      <span className="h-1.5 w-1.5 rounded-full bg-red-500" />
      Attrition: Yes
    </Badge>
  ) : (
    <Badge className="gap-1.5 bg-neutral-100 text-neutral-600">
      <span className="h-1.5 w-1.5 rounded-full bg-neutral-400" />
      Active
    </Badge>
  );
}
