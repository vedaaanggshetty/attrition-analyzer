package com.example.EmployeeService.mapper;

import com.example.EmployeeService.client.SurveyEmployeeResponse;
import com.example.EmployeeService.dto.EmployeeDto;

public final class EmployeeMapper {

	private EmployeeMapper() {
	}

	public static EmployeeDto toEmployeeDto(SurveyEmployeeResponse response) {
		return new EmployeeDto(
				response.id(),
				response.EmployeeID(),
				response.FirstName(),
				response.LastName(),
				response.Gender(),
				response.Age(),
				response.BusinessTravel(),
				response.Department(),
				response.DistanceFromHome_km(),
				response.State(),
				response.Ethnicity(),
				response.Education(),
				response.EducationField(),
				response.JobRole(),
				response.MaritalStatus(),
				response.Salary(),
				response.StockOptionLevel(),
				response.OverTime(),
				response.HireDate(),
				response.Attrition(),
				response.YearsAtCompany(),
				response.YearsInMostRecentRole(),
				response.YearsSinceLastPromotion(),
				response.YearsWithCurrManager());
	}
}
