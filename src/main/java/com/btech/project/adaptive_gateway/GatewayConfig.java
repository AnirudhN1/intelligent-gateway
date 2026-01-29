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
                            // SYNTHETIC LOAD: Busy-wait for 50ms to force CPU usage
                            long start = System.currentTimeMillis();
                            while (System.currentTimeMillis() - start < 50) {
                                Math.sqrt(Math.random()); // Burn cycles
                            }
                            return chain.filter(exchange);
                        }))
                        .uri("https://www.google.com")) // Still forwards, but works first
                .route("google_search", r -> r.path("/search/**")
                        .uri("https://www.google.com"))
                .build();
    }
}