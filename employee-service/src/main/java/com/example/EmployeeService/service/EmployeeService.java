package com.example.EmployeeService.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import feign.FeignException;

import com.example.EmployeeService.client.SurveyApiClient;
import com.example.EmployeeService.dto.AttritionAnalysisDto;
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.event.EmployeeFlaggedEvent;
import com.example.EmployeeService.event.EmployeeFlaggedEventProducer;
import com.example.EmployeeService.exception.SurveyApiException;
import com.example.EmployeeService.mapper.EmployeeMapper;

@Service
public class EmployeeService {

	private final SurveyApiClient surveyApiClient;
	private final EmployeeFlaggedEventProducer eventProducer;

	public EmployeeService(SurveyApiClient surveyApiClient, EmployeeFlaggedEventProducer eventProducer) {
		this.surveyApiClient = surveyApiClient;
		this.eventProducer = eventProducer;
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

	/**
	 * Looks up the employee (reusing the existing Survey API lookup/mapping),
	 * then publishes an {@link EmployeeFlaggedEvent} for Notification Service
	 * to consume. Returns empty if the employee doesn't exist, same contract
	 * as {@link #getEmployeeById(String)}.
	 */
	public Optional<EmployeeFlaggedEvent> flagEmployee(String id, String comment, String hrUserEmail) {
		Optional<EmployeeDto> employee = getEmployeeById(id);
		if (employee.isEmpty()) {
			return Optional.empty();
		}

		EmployeeDto dto = employee.get();
		EmployeeFlaggedEvent event = new EmployeeFlaggedEvent(
				UUID.randomUUID(),
				dto.employeeId(),
				dto.firstName() + " " + dto.lastName(),
				dto.department(),
				comment,
				hrUserEmail,
				Instant.now());

		eventProducer.publish(event);
		return Optional.of(event);
	}

	public List<AttritionAnalysisDto> getAttritionByDepartment() {
		return aggregateAttritionBy(this::departmentOrUnknown);
	}

	public List<AttritionAnalysisDto> getAttritionByJobRole() {
		return aggregateAttritionBy(this::jobRoleOrUnknown);
	}

	// "Compensation" isn't defined further in the backlog. Salary is the obvious field;
	// it's bucketed into $50,000 bands since raw salary is continuous. Band width is an
	// engineering decision, not a documented requirement.
	public List<AttritionAnalysisDto> getAttritionByCompensation() {
		return aggregateAttritionBy(this::salaryBand);
	}

	// "Demographics" isn't tied to a specific field in the backlog. Gender is used here
	// as the simplest, lowest-cardinality demographic breakdown - a judgment call, not a
	// documented choice. Other fields (age, ethnicity, marital status, state) are equally
	// plausible and were not selected.
	public List<AttritionAnalysisDto> getAttritionByDemographics() {
		return aggregateAttritionBy(this::genderOrUnknown);
	}

	// The story text explicitly names overtime as the work-life balance factor to use.
	public List<AttritionAnalysisDto> getAttritionByWorkLifeBalance() {
		return aggregateAttritionBy(this::overTimeOrUnknown);
	}

	// "Career progression" isn't tied to a specific field. Years since last promotion is
	// used here as the closest match to "progression" among the available years-based
	// fields - a judgment call, not a documented choice. Bucketed since it's continuous.
	public List<AttritionAnalysisDto> getAttritionByCareerProgression() {
		return aggregateAttritionBy(this::yearsSincePromotionBand);
	}

	private List<AttritionAnalysisDto> aggregateAttritionBy(Function<EmployeeDto, String> groupKeyExtractor) {
		List<EmployeeDto> employees = getAllEmployees();

		Map<String, List<EmployeeDto>> grouped = employees.stream()
				.collect(Collectors.groupingBy(
						groupKeyExtractor,
						TreeMap::new,
						Collectors.toList()));

		List<AttritionAnalysisDto> result = new ArrayList<>();
		for (Map.Entry<String, List<EmployeeDto>> entry : grouped.entrySet()) {
			result.add(toAttritionAnalysisDto(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	private String departmentOrUnknown(EmployeeDto employee) {
		return StringUtils.hasText(employee.department()) ? employee.department() : "Unknown";
	}

	private String jobRoleOrUnknown(EmployeeDto employee) {
		return StringUtils.hasText(employee.jobRole()) ? employee.jobRole() : "Unknown";
	}

	private String genderOrUnknown(EmployeeDto employee) {
		return StringUtils.hasText(employee.gender()) ? employee.gender() : "Unknown";
	}

	private String overTimeOrUnknown(EmployeeDto employee) {
		return StringUtils.hasText(employee.overTime()) ? employee.overTime() : "Unknown";
	}

	private String salaryBand(EmployeeDto employee) {
		Integer salary = employee.salary();
		if (salary == null) {
			return "Unknown";
		}
		int bandStart = (salary / 50000) * 50000;
		int bandEnd = bandStart + 49999;
		return "$" + bandStart + "-$" + bandEnd;
	}

	private String yearsSincePromotionBand(EmployeeDto employee) {
		Integer years = employee.yearsSinceLastPromotion();
		if (years == null) {
			return "Unknown";
		}
		if (years <= 2) {
			return "0-2 years";
		}
		if (years <= 5) {
			return "3-5 years";
		}
		return "6+ years";
	}

	private AttritionAnalysisDto toAttritionAnalysisDto(String groupLabel, List<EmployeeDto> group) {
		long attritionCount = group.stream()
				.filter(employee -> "Yes".equals(employee.attrition()))
				.count();
		double attritionRate = (attritionCount * 100.0) / group.size();
		return new AttritionAnalysisDto(groupLabel, group.size(), (int) attritionCount, attritionRate);
	}
}
