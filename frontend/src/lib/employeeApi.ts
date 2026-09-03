import { apiRequest, ApiError } from "./apiClient";

// Mirrors employee-service's EmployeeDto / AttritionAnalysisDto - the
// Gateway-facing contract (see EmployeeController).

export interface Employee {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  gender: string;
  age: number;
  businessTravel: string;
  department: string;
  distanceFromHomeKm: number;
  state: string;
  ethnicity: string;
  education: number;
  educationField: string;
  jobRole: string;
  maritalStatus: string;
  salary: number;
  stockOptionLevel: number;
  overTime: "Yes" | "No";
  hireDate: string;
  attrition: "Yes" | "No";
  yearsAtCompany: number;
  yearsInMostRecentRole: number;
  yearsSinceLastPromotion: number;
  yearsWithCurrManager: number;
}

export interface AttritionAnalysis {
  groupLabel: string;
  totalEmployees: number;
  attritionCount: number;
  attritionRate: number;
}

export interface FlagEmployeeResponse {
  eventId: string;
  employeeId: string;
  employeeName: string;
  department: string;
  comment: string;
  hrUserEmail: string;
  flaggedAt: string;
}

// GET /employees/analysis/** is the one route Guests can reach without a
// JWT (US-21) - apiRequest still attaches a token if one happens to be
// present, which is harmless since the endpoint ignores auth either way.
export function getAllEmployees(): Promise<Employee[]> {
  return apiRequest<Employee[]>("/employees");
}

export function getEmployeeById(id: string): Promise<Employee | null> {
  return apiRequest<Employee>(`/employees/${id}`).catch((err: unknown) => {
    if (err instanceof ApiError && err.status === 404) return null;
    throw err;
  });
}

export function flagEmployee(id: string, comment: string): Promise<FlagEmployeeResponse> {
  return apiRequest<FlagEmployeeResponse>(`/employees/${id}/flag`, { method: "POST", body: { comment } });
}

export function getAttritionByDepartment(): Promise<AttritionAnalysis[]> {
  return apiRequest<AttritionAnalysis[]>("/employees/analysis/department", { authenticated: false });
}

export function getAttritionByJobRole(): Promise<AttritionAnalysis[]> {
  return apiRequest<AttritionAnalysis[]>("/employees/analysis/job-role", { authenticated: false });
}

export function getAttritionByCompensation(): Promise<AttritionAnalysis[]> {
  return apiRequest<AttritionAnalysis[]>("/employees/analysis/compensation", { authenticated: false });
}

export function getAttritionByDemographics(): Promise<AttritionAnalysis[]> {
  return apiRequest<AttritionAnalysis[]>("/employees/analysis/demographics", { authenticated: false });
}

export function getAttritionByWorkLifeBalance(): Promise<AttritionAnalysis[]> {
  return apiRequest<AttritionAnalysis[]>("/employees/analysis/work-life-balance", { authenticated: false });
}

export function getAttritionByCareerProgression(): Promise<AttritionAnalysis[]> {
  return apiRequest<AttritionAnalysis[]>("/employees/analysis/career-progression", { authenticated: false });
}
