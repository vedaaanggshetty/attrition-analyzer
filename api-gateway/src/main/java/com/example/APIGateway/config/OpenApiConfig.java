package com.example.APIGateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The Gateway has no business endpoints of its own, so this is the actual
// landing page contributors see at /swagger-ui.html (set as the default via
// springdoc.swagger-ui.urls-primary-name) - a real overview with working
// links to each service, not an empty title. Swagger UI renders this
// description as Markdown.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .description("""
                                Single entry point for the frontend - routes every request below to the right \
                                business service. This Gateway has no business endpoints of its own.

                                Use the **dropdown above** (top-left) to switch between services' full API docs \
                                without leaving this page, or open a service directly on its own port:

                                - [Authentication Service](http://localhost:8081/swagger-ui.html) - login, JWT issuance (port 8081)
                                - [User Profile Service](http://localhost:8082/swagger-ui.html) - registration, profile view/update (port 8082)
                                - [Employee Service](http://localhost:8083/swagger-ui.html) - employee data, attrition analysis, flagging (port 8083)
                                - [Notification Service](http://localhost:8084/swagger-ui.html) - shared HR notifications (port 8084)

                                All business routes are also reachable through this Gateway at \
                                `http://localhost:8080` (e.g. `POST /auth/login`), the same address the frontend uses.
                                """)
                        .version("v1"));
    }
}
