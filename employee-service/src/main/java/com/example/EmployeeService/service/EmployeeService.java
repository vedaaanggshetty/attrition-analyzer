package com.example.EmployeeService.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import feign.FeignException;

import com.example.EmployeeService.client.SurveyApiClient;
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.exception.SurveyApiException;
import com.example.EmployeeService.mapper.EmployeeMapper;

@Service
public class EmployeeService {

	private final SurveyApiClient surveyApiClient;

	public EmployeeService(SurveyApiClient surveyApiClient) {
		this.surveyApiClient = surveyApiClient;
	}

	public List<EmployeeDto> getAllEmployees() {
		try {
			return surveyApiClient.getAllEmployees().stream()
					.map(EmployeeMapper::toEmployeeDto)
					.toList();
		} catch (FeignException ex) {
			throw new SurveyApiException("Failed to retrieve employees from Survey API", ex);
		}
	}

	public List<EmployeeDto> findByProperty(String propertyName, String value) {
		try {
			return surveyApiClient.findByProperty(Map.of(propertyName, value)).stream()
					.map(EmployeeMapper::toEmployeeDto)
					.toList();
		} catch (FeignException ex) {
			throw new SurveyApiException("Failed to query Survey API for " + propertyName, ex);
		}
	}

	public Optional<EmployeeDto> getEmployeeById(String id) {
		try {
			return Optional.of(EmployeeMapper.toEmployeeDto(surveyApiClient.getEmployeeById(id)));
		} catch (FeignException.NotFound ex) {
			return Optional.empty();
		} catch (FeignException ex) {
			throw new SurveyApiException("Failed to retrieve employee " + id + " from Survey API", ex);
		}
	}
}
