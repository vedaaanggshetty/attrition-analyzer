package com.example.EmployeeService.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
                // See authentication-service's OpenApiConfig for why this is needed -
                // without it, the auto-generated server URL is the Docker container's
                // internal hostname, which breaks Swagger UI's "Try it out" in a browser.
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
