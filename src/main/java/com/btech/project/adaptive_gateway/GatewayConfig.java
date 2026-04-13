package com.btech.project.adaptive_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("stress_test_route", r -> r.path("/stress/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            long start = System.currentTimeMillis();
                            // Increased from 50ms to 200ms to simulate a heavy database query
                            while (System.currentTimeMillis() - start < 200) {
                                Math.sqrt(Math.random());
                            }
                            return chain.filter(exchange);
                        }))
                        .uri("https://www.google.com"))
                .route("google_search", r -> r.path("/search/**")
                        .uri("https://www.google.com"))
                .build();
    }
}