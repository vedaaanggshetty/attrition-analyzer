import { cx } from "../../lib/utils";

export function Skeleton({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return <div className={cx("animate-pulse rounded-lg bg-brand-900/8", className)} style={style} />;
}
