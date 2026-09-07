package com.example.UserProfileService.config;

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
    public OpenAPI userProfileServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Profile Service API")
                        .description("Registration and profile view/update. "
                                + "/users/register is public; every other endpoint (e.g. /users/me) "
                                + "requires a Bearer JWT issued by Authentication Service.")
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
