import type {
  AttritionAnalysis,
  Employee,
  FlaggedNotification,
  KpiSummary,
  UserProfile,
} from "../types";

// All mock data lives here so pages/components never hardcode sample values
// inline. Swap this module for real API calls later without touching the UI.

const FIRST_NAMES = [
  "Leonelle", "Marcus", "Priya", "Daniel", "Sofia", "Ethan", "Amara", "Noah",
  "Ines", "Jamal", "Yuki", "Oliver", "Chidi", "Elena", "Ravi", "Hannah",
  "Kwame", "Mei", "Lucas", "Fatima", "Tobias", "Grace", "Andres", "Nadia",
];
const LAST_NAMES = [
  "Simco", "Ortega", "Chen", "Okafor", "Rossi", "Kowalski", "Nakamura",
  "Bianchi", "Silva", "Haddad", "Larsson", "Diallo", "Novak", "Petrova",
  "Kumar", "Reyes", "Andersen", "Popescu", "Osei", "Fischer",
];
const DEPARTMENTS = ["Sales", "Technology", "Human Resources", "Finance", "Marketing", "Operations"];
const JOB_ROLES: Record<string, string[]> = {
  Sales: ["Sales Executive", "Sales Manager", "Account Representative"],
  Technology: ["Software Engineer", "QA Engineer", "DevOps Engineer", "Engineering Manager"],
  "Human Resources": ["HR Business Partner", "Recruiter", "HR Manager"],
  Finance: ["Financial Analyst", "Accountant", "Finance Manager"],
  Marketing: ["Marketing Specialist", "Brand Manager", "Content Strategist"],
  Operations: ["Operations Analyst", "Logistics Coordinator", "Operations Manager"],
};
const STATES = ["CA", "NY", "TX", "IL", "WA", "MA", "CO", "GA"];
const EDUCATION_FIELDS = ["Marketing", "Computer Science", "Business", "Human Resources", "Finance", "Design"];
const MARITAL_STATUS = ["Single", "Married", "Divorced"];
const AVATAR_COLORS = ["bg-brand-900", "bg-brand-500", "bg-blue-800", "bg-sky-700", "bg-blue-950"];

function seededRandom(seed: number) {
  let value = seed;
  return () => {
    value = (value * 9301 + 49297) % 233280;
    return value / 233280;
  };
}

const rand = seededRandom(42);
function pick<T>(arr: T[]): T {
  return arr[Math.floor(rand() * arr.length)];
}
function range(min: number, max: number) {
  return Math.floor(rand() * (max - min + 1)) + min;
}

function buildEmployee(index: number): Employee {
  const department = pick(DEPARTMENTS);
  const jobRole = pick(JOB_ROLES[department]);
  const yearsSinceLastPromotion = range(0, 9);
  const overTime = rand() > 0.68 ? "Yes" : "No";
  const salary = range(38, 165) * 1000;
  const attritionScore =
    (overTime === "Yes" ? 0.35 : 0) +
    (yearsSinceLastPromotion > 5 ? 0.25 : 0) +
    (salary < 55000 ? 0.25 : 0) +
    rand() * 0.3;
  const attrition = attritionScore > 0.55 ? "Yes" : "No";
  const attritionRisk: Employee["attritionRisk"] =
    attritionScore > 0.6 ? "High" : attritionScore > 0.35 ? "Medium" : "Low";

  const hireYear = 2013 + range(0, 11);
  const hireMonth = String(range(1, 12)).padStart(2, "0");
  const hireDay = String(range(1, 28)).padStart(2, "0");

  return {
    id: `emp-${index}`,
    employeeId: `${3000 + index}-${String.fromCharCode(65 + (index % 26))}${range(10, 99)}`,
    firstName: pick(FIRST_NAMES),
    lastName: pick(LAST_NAMES),
    avatarColor: pick(AVATAR_COLORS),
    gender: rand() > 0.5 ? "Female" : "Male",
    age: range(22, 59),
    department,
    jobRole,
    state: pick(STATES),
    educationField: pick(EDUCATION_FIELDS),
    maritalStatus: pick(MARITAL_STATUS),
    salary,
    stockOptionLevel: range(0, 3),
    overTime,
    hireDate: `${hireYear}-${hireMonth}-${hireDay}`,
    attrition,
    attritionRisk,
    yearsAtCompany: 2026 - hireYear,
    yearsInMostRecentRole: Math.min(2026 - hireYear, range(0, 6)),
    yearsSinceLastPromotion: Math.min(2026 - hireYear, yearsSinceLastPromotion),
    yearsWithCurrManager: Math.min(2026 - hireYear, range(0, 7)),
    distanceFromHomeKm: range(1, 45),
  };
}

export const employees: Employee[] = Array.from({ length: 84 }, (_, i) => buildEmployee(i + 1));

function aggregate(groupKey: (e: Employee) => string): AttritionAnalysis[] {
  const groups = new Map<string, Employee[]>();
  for (const employee of employees) {
    const key = groupKey(employee);
    groups.set(key, [...(groups.get(key) ?? []), employee]);
  }
  return Array.from(groups.entries())
    .map(([groupLabel, group]) => {
      const attritionCount = group.filter((e) => e.attrition === "Yes").length;
      return {
        groupLabel,
        totalEmployees: group.length,
        attritionCount,
        attritionRate: Math.round((attritionCount / group.length) * 1000) / 10,
      };
    })
    .sort((a, b) => b.attritionRate - a.attritionRate);
}

function salaryBand(e: Employee): string {
  const bandStart = Math.floor(e.salary / 50000) * 50000;
  return `$${bandStart / 1000}k-$${(bandStart + 49999) / 1000}k`;
}
function promotionBand(e: Employee): string {
  if (e.yearsSinceLastPromotion <= 2) return "0-2 years";
  if (e.yearsSinceLastPromotion <= 5) return "3-5 years";
  return "6+ years";
}

export const attritionByDepartment = aggregate((e) => e.department);
export const attritionByJobRole = aggregate((e) => e.jobRole);
export const attritionByCompensation = aggregate(salaryBand).sort((a, b) =>
  a.groupLabel.localeCompare(b.groupLabel, undefined, { numeric: true })
);
export const attritionByDemographics = aggregate((e) => e.gender);
export const attritionByWorkLifeBalance = aggregate((e) => (e.overTime === "Yes" ? "Overtime" : "No Overtime"));
export const attritionByCareerProgression = aggregate(promotionBand).sort((a, b) =>
  a.groupLabel.localeCompare(b.groupLabel, undefined, { numeric: true })
);

export const kpiSummary: KpiSummary = {
  totalEmployees: employees.length,
  attritionRate:
    Math.round((employees.filter((e) => e.attrition === "Yes").length / employees.length) * 1000) / 10,
  highRiskEmployees: employees.filter((e) => e.attritionRisk === "High").length,
  departments: DEPARTMENTS.length,
};

export const notifications: FlaggedNotification[] = [
  {
    id: 1,
    employeeId: employees[3].employeeId,
    employeeName: `${employees[3].firstName} ${employees[3].lastName}`,
    department: employees[3].department,
    comment: "Overtime spiked for three straight sprints - check in before it becomes a resignation.",
    createdAt: "2026-08-28T09:14:00Z",
    hrUserEmail: "hr@attritionanalyzer.com",
  },
  {
    id: 2,
    employeeId: employees[11].employeeId,
    employeeName: `${employees[11].firstName} ${employees[11].lastName}`,
    department: employees[11].department,
    comment: "No promotion in 6 years despite strong reviews. Flight risk.",
    createdAt: "2026-08-26T15:40:00Z",
    hrUserEmail: "hr@attritionanalyzer.com",
  },
  {
    id: 3,
    employeeId: employees[27].employeeId,
    employeeName: `${employees[27].firstName} ${employees[27].lastName}`,
    department: employees[27].department,
    comment: "Requested compensation review after competing offer.",
    createdAt: "2026-08-22T11:05:00Z",
    hrUserEmail: "hr@attritionanalyzer.com",
  },
];

export const currentUser: UserProfile = {
  userId: "usr-0142",
  fullName: "Jordan Blake",
  email: "jordan.blake@attritionanalyzer.com",
  phone: "+1 (555) 019-4482",
  role: "HR User",
  createdAt: "2025-02-11T00:00:00Z",
};
