package com.example.EmployeeService.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.EmployeeService.client.SurveyEmployeeResponse;
import com.example.EmployeeService.dto.EmployeeDto;

class EmployeeMapperTest {

	@Test
	void mapsEveryVerifiedFieldFromSurveyResponseToEmployeeDto() {
		SurveyEmployeeResponse response = new SurveyEmployeeResponse("3012-1A41", "Leonelle", "Simco", "Female", 30,
				"Some Travel", "Sales", 27, "IL",
				"White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1, "No", "2012-01-03", "No", 10, 4, 9,
				7, "5a94");

		EmployeeDto dto = EmployeeMapper.toEmployeeDto(response);

		assertThat(dto.id()).isEqualTo("5a94");
		assertThat(dto.employeeId()).isEqualTo("3012-1A41");
		assertThat(dto.firstName()).isEqualTo("Leonelle");
		assertThat(dto.lastName()).isEqualTo("Simco");
		assertThat(dto.gender()).isEqualTo("Female");
		assertThat(dto.age()).isEqualTo(30);
		assertThat(dto.businessTravel()).isEqualTo("Some Travel");
		assertThat(dto.department()).isEqualTo("Sales");
		assertThat(dto.distanceFromHomeKm()).isEqualTo(27);
		assertThat(dto.state()).isEqualTo("IL");
		assertThat(dto.ethnicity()).isEqualTo("White");
		assertThat(dto.education()).isEqualTo(5);
		assertThat(dto.educationField()).isEqualTo("Marketing");
		assertThat(dto.jobRole()).isEqualTo("Sales Executive");
		assertThat(dto.maritalStatus()).isEqualTo("Divorced");
		assertThat(dto.salary()).isEqualTo(102059);
		assertThat(dto.stockOptionLevel()).isEqualTo(1);
		assertThat(dto.overTime()).isEqualTo("No");
		assertThat(dto.hireDate()).isEqualTo("2012-01-03");
		assertThat(dto.attrition()).isEqualTo("No");
		assertThat(dto.yearsAtCompany()).isEqualTo(10);
		assertThat(dto.yearsInMostRecentRole()).isEqualTo(4);
		assertThat(dto.yearsSinceLastPromotion()).isEqualTo(9);
		assertThat(dto.yearsWithCurrManager()).isEqualTo(7);
	}
}
