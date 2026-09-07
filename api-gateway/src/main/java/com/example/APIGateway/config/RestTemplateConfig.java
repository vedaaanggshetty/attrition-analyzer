package com.example.APIGateway.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * A load-balanced RestTemplate resolves a plain Eureka service id in a URL
 * (e.g. {@code http://authentication-service/...}) to a real instance
 * address, the same way the Gateway's own {@code lb://} routes do - used
 * only to fetch each service's OpenAPI document for the aggregated Swagger
 * page (see AggregatedOpenApiCustomizer), never for proxying real traffic.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
