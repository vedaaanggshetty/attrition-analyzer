package com.example.AuthService.config;

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
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Authentication Service API")
                        .description("Login and JWT issuance. "
                                + "/auth/login is public; every other endpoint requires a Bearer JWT issued by "
                                + "this service.")
                        .version("v1"))
                // Without this, springdoc auto-generates the server URL from the
                // request it saw - inside Docker that's the container's internal
                // hostname (e.g. http://<container-id>:8081), which a browser can
                // never resolve, breaking Swagger UI's "Try it out" with a
                // "Failed to fetch" error. A relative "/" server resolves against
                // wherever the docs are actually being viewed from - this
                // service's own port directly, or the Gateway's aggregated UI -
                // so "Try it out" always calls back to the same origin the page
                // was loaded from.
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
