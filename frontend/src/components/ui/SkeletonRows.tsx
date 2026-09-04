import { cx } from "../../lib/utils";
import { Skeleton } from "./Skeleton";

/** A stack of placeholder rows shown while a list is still loading. */
export function SkeletonRows({
  count,
  rowClassName = "h-6 w-full",
  gapClassName = "gap-4",
}: {
  count: number;
  rowClassName?: string;
  gapClassName?: string;
}) {
  return (
    <div className={cx("flex flex-col", gapClassName)}>
      {Array.from({ length: count }, (_, i) => (
        <Skeleton key={i} className={rowClassName} />
      ))}
    </div>
  );
}
