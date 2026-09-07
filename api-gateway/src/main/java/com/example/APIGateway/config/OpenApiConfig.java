package com.example.APIGateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// The base document the Gateway builds before AggregatedOpenApiCustomizer
// merges in every business service's own paths/schemas/tags - see that
// class for how the endpoints below actually get here.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Attrition Analyzer API")
                        .description("""
                                Every endpoint the frontend calls, through the single entry point it actually \
                                uses - the API Gateway. Grouped below by tag: **Authentication**, **User \
                                Profile**, **Employee**, **Notifications**.

                                Each endpoint's description states whether it's **Public** (no token needed), \
                                **Guest-accessible** (the six attrition-analysis endpoints - public, same as \
                                Public, called out separately since they're what a logged-out Guest can see \
                                on the landing page), or requires a **Bearer JWT** (HR-only).

                                **To try protected endpoints, in this order:**
                                1. `POST /users/register` - create an HR account (Public)
                                2. `POST /auth/login` - log in with that email/password (Public) - copy the \
                                `token` field from the response
                                3. Click **Authorize** (top right) and paste the token as \
                                `Bearer <token>` (or just the raw token - Swagger adds the prefix)
                                4. Now call any protected endpoint - e.g. `GET /users/me`, `GET /employees`, \
                                `GET /employees/{id}`, `POST /employees/{id}/flag`, or any `/notifications/**` \
                                endpoint.
                                """)
                        .version("v1"))
                // "Try it out" must call the Gateway itself - explicit and absolute, not a
                // relative "/", so it always hits port 8080 regardless of how this page
                // is being reached (this is the one address the frontend also uses).
                .servers(List.of(new Server().url("http://localhost:8080")));
    }
}
