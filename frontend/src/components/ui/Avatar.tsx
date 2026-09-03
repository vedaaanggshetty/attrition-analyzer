import { cx, initials } from "../../lib/utils";

export function Avatar({
  firstName,
  lastName,
  color = "bg-brand-900",
  size = "md",
}: {
  firstName: string;
  lastName: string;
  color?: string;
  size?: "sm" | "md" | "lg";
}) {
  const sizeStyles = { sm: "h-8 w-8 text-xs", md: "h-11 w-11 text-sm", lg: "h-16 w-16 text-lg" }[size];
  return (
    <div
      className={cx(
        "flex shrink-0 items-center justify-center rounded-full font-semibold text-white",
        color,
        sizeStyles
      )}
    >
      {initials(firstName, lastName)}
    </div>
  );
}
