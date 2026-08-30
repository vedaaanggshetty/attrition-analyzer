package com.example.EmployeeService.client;

// Component names intentionally match the verified Survey API field names exactly
// (including casing) so the external contract stays traceable field-for-field.
public record SurveyEmployeeResponse(
		String EmployeeID,
		String FirstName,
		String LastName,
		String Gender,
		Integer Age,
		String BusinessTravel,
		String Department,
		Integer DistanceFromHome_km,
		String State,
		String Ethnicity,
		Integer Education,
		String EducationField,
		String JobRole,
		String MaritalStatus,
		Integer Salary,
		Integer StockOptionLevel,
		String OverTime,
		String HireDate,
		String Attrition,
		Integer YearsAtCompany,
		Integer YearsInMostRecentRole,
		Integer YearsSinceLastPromotion,
		Integer YearsWithCurrManager,
		String id) {
}
