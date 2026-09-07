package com.example.EmployeeService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A note attached to an employee, published for Notification Service to record")
public record FlagEmployeeRequest(

		@Schema(description = "Why this employee is being flagged", example = "Flight risk, discuss retention")
		@NotBlank(message = "Comment is required")
		@Size(max = 1000, message = "Comment must be at most 1000 characters")
		String comment,

		// The flagging HR user's display name, sent by the frontend (it already
		// holds this from the profile it fetched at login) since the JWT itself
		// only carries an email claim, not a name - see Notification's
		// hrUserName field for why this is captured once here rather than
		// looked up cross-service later.
		@Schema(description = "Display name of the HR user sending this, if known", example = "Jordan Lee")
		String hrUserName) {
}
