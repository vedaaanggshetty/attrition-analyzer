// NOTE: this file previously included an `attritionRisk()` / `riskReasonFor()`
// pair that computed a client-side "risk score" (Low/Medium/High) from
// overtime, years-since-promotion, and salary. That score was never part of
// the product backlog - US-11 through US-16 are about grouping and comparing
// attrition rates across six real dimensions (department, job role,
// compensation band, gender, overtime, years-since-promotion band), not
// about scoring individual employees. It's been removed; use the employee's
// actual `attrition` field ("Yes"/"No") wherever a signal is needed.

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
