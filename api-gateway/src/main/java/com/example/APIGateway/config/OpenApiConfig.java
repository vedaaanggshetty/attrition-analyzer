package com.example.APIGateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The Gateway has no business endpoints of its own - this just gives its
// (near-empty) local doc a title, so it reads as "the Gateway itself" rather
// than an unlabeled entry, when picked from the aggregated dropdown.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .description("Single entry point for the frontend - routes to the business services listed "
                                + "in the dropdown above. This Gateway has no business endpoints of its own.")
                        .version("v1"));
    }
}
