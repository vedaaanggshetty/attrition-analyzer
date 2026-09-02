package com.example.EmployeeService.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.EmployeeService.client.SurveyEmployeeResponse;
import com.example.EmployeeService.dto.EmployeeDto;

class EmployeeMapperTest {

	@Test
	void mapsSurveyResponseToEmployeeDto() {
		SurveyEmployeeResponse response = new SurveyEmployeeResponse("3012-1A41", "Leonelle", "Simco", "Female", 30,
				"Some Travel", "Sales", 27, "IL", "White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1,
				"No", "2012-01-03", "No", 10, 4, 9, 7, "5a94");

		EmployeeDto dto = EmployeeMapper.toEmployeeDto(response);

		EmployeeDto expected = new EmployeeDto("5a94", "3012-1A41", "Leonelle", "Simco", "Female", 30, "Some Travel",
				"Sales", 27, "IL", "White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1, "No",
				"2012-01-03", "No", 10, 4, 9, 7);
		assertThat(dto).isEqualTo(expected);
	}
}
