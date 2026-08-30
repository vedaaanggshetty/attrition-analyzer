package com.example.EmployeeService.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "survey-api", url = "${survey-api.base-url}")
public interface SurveyApiClient {

	@GetMapping("/survey")
	List<SurveyEmployeeResponse> getAllEmployees();

	// The Survey API filters by a single field=value query parameter (the field name
	// itself is the parameter key, e.g. ?Department=Sales) — verified against the
	// running container to only honor the first parameter, so only one entry is used.
	@GetMapping("/survey")
	List<SurveyEmployeeResponse> findByProperty(@SpringQueryMap Map<String, Object> query);

	@GetMapping("/survey/{id}")
	SurveyEmployeeResponse getEmployeeById(@PathVariable("id") String id);
}
