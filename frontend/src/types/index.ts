export type AttritionRisk = "Low" | "Medium" | "High";

export interface Employee {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  avatarColor: string;
  gender: string;
  age: number;
  department: string;
  jobRole: string;
  state: string;
  educationField: string;
  maritalStatus: string;
  salary: number;
  stockOptionLevel: number;
  overTime: "Yes" | "No";
  hireDate: string;
  attrition: "Yes" | "No";
  attritionRisk: AttritionRisk;
  yearsAtCompany: number;
  yearsInMostRecentRole: number;
  yearsSinceLastPromotion: number;
  yearsWithCurrManager: number;
  distanceFromHomeKm: number;
}

export interface AttritionAnalysis {
  groupLabel: string;
  totalEmployees: number;
  attritionCount: number;
  attritionRate: number;
}

export interface KpiSummary {
  totalEmployees: number;
  attritionRate: number;
  highRiskEmployees: number;
  departments: number;
}

export interface FlaggedNotification {
  id: number;
  employeeId: string;
  employeeName: string;
  department: string;
  comment: string;
  createdAt: string;
  hrUserEmail: string;
}

export interface UserProfile {
  userId: string;
  fullName: string;
  email: string;
  phone: string;
  role: "HR User" | "Guest";
  createdAt: string;
}
