import { cx } from "../../lib/utils";

/**
 * Shared logo mark. Wordmark styling (serif italic accent) follows the
 * reference design prompt's logo treatment, adapted to our full product name.
 */
export function Wordmark({ dark = false }: { dark?: boolean }) {
  return (
    <span className={cx("font-display text-base font-semibold tracking-tight whitespace-nowrap", dark && "text-white")}>
      Attrition <em className="font-serif italic tracking-tight">Analyzer</em>
    </span>
  );
}
