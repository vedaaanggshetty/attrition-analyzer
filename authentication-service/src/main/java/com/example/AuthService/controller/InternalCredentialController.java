package com.example.AuthService.controller;

import com.example.AuthService.dto.ErrorResponse;
import com.example.AuthService.dto.RegisterCredentialRequest;
import com.example.AuthService.dto.RegisterCredentialResponse;
import com.example.AuthService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INTERNAL service-to-service contract only.
 *
 * {@code POST /internal/credentials} is called by User Profile Service
 * (via Feign) during its public registration flow. It is NOT the frontend's
 * registration endpoint - there is no {@code POST /auth/register} here.
 */
@Tag(name = "Internal Credentials", description = "Service-to-service only - called by User Profile Service via Feign, never by the frontend")
@RestController
@RequestMapping("/internal/credentials")
public class InternalCredentialController {

    private final AuthService authService;

    public InternalCredentialController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Create a credential for a newly registered user",
            description = "Called internally by User Profile Service during public registration - not reachable "
                    + "from the frontend/Gateway. No JWT is required since this is a trusted internal call."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credential created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterCredentialResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email is already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RegisterCredentialResponse> registerCredential(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Email and password for the new credential", required = true)
            @Valid @RequestBody RegisterCredentialRequest request) {
        RegisterCredentialResponse response = authService.registerCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
