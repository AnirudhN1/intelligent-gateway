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
                // Rule 1: Forward anything starting with /search to Google
                .route("google_search", r -> r.path("/search/**")
                        .uri("https://www.google.com"))
                // Rule 2: Forward /vit to the VIT website
                .route("vit_route", r -> r.path("/vit/**")
                        .uri("https://vit.ac.in"))
                .build();
    }
}