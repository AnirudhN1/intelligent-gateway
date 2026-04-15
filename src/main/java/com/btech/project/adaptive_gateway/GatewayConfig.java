package com.btech.project.adaptive_gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // HIGH PRIORITY: Payment Service (Simulated fast backend)
                .route("payment_route", r -> r.path("/payment/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            long start = System.currentTimeMillis();
                            while (System.currentTimeMillis() - start < 50) { Math.sqrt(Math.random()); }
                            return chain.filter(exchange);
                        }))
                        .uri("https://httpbin.org/get")) // A safe mock API

                // LOW PRIORITY: Review/Search Service (The target of our stress test)
                .route("stress_test_route", r -> r.path("/stress/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            long start = System.currentTimeMillis();
                            while (System.currentTimeMillis() - start < 200) { Math.sqrt(Math.random()); }
                            return chain.filter(exchange);
                        }))
                        .uri("https://httpbin.org/get"))
                .build();
    }
}