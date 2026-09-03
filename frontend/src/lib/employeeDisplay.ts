import type { Employee } from "./employeeApi";
import type { AttritionRisk } from "../types";

// Employee Service doesn't expose a risk score, so this derives one
// client-side from the same signals HR already sees on the record (overtime,
// years since promotion, compensation). Deterministic - no randomness - so
// the same employee always shows the same risk badge.
export function attritionRisk(employee: Pick<Employee, "overTime" | "yearsSinceLastPromotion" | "salary">): AttritionRisk {
  const score =
    (employee.overTime === "Yes" ? 0.35 : 0) +
    (employee.yearsSinceLastPromotion > 5 ? 0.25 : 0) +
    (employee.salary < 55000 ? 0.25 : 0);
  if (score > 0.55) return "High";
  if (score > 0.3) return "Medium";
  return "Low";
}

const AVATAR_COLORS = ["bg-brand-900", "bg-brand-500", "bg-blue-800", "bg-sky-700", "bg-blue-950"];

function hashString(id: string): number {
  let hash = 0;
  for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
  return hash;
}

// Deterministic color from the employee's id, so the same person always
// gets the same avatar color across pages without the backend needing to
// supply one.
export function avatarColorFor(id: string): string {
  return AVATAR_COLORS[hashString(id) % AVATAR_COLORS.length];
}

const DEPARTMENT_CHIP_STYLES = [
  "bg-brand-50 text-brand-700",
  "bg-violet-50 text-violet-700",
  "bg-emerald-50 text-emerald-700",
  "bg-amber-50 text-amber-700",
  "bg-pink-50 text-pink-700",
  "bg-teal-50 text-teal-700",
];

// Same idea as avatarColorFor, but keyed on the department name so every
// employee in the same department reads consistently across the table.
export function departmentChipStyleFor(department: string): string {
  return DEPARTMENT_CHIP_STYLES[hashString(department) % DEPARTMENT_CHIP_STYLES.length];
}
