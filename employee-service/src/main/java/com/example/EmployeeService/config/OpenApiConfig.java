package com.example.EmployeeService.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI employeeServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Service API")
                        .description("Employee data (proxied from the external Survey API), attrition analysis, "
                                + "and flagging. This service has no Spring Security of its own - it relies on "
                                + "the API Gateway for authorization and only reads the JWT's email claim "
                                + "directly to attribute flags/notifications to the caller. A Bearer JWT is "
                                + "required for every endpoint when reached through the Gateway.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
