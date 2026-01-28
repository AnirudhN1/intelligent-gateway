package com.btech.project.adaptive_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Component
@Slf4j
public class AdaptiveLoadShedderFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    // We start with a static limit of 5 requests per 10 seconds for testing
    private static final int STATIC_LIMIT = 5;

    public AdaptiveLoadShedderFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = "request_count";

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    // If it's the first request in the window, set an expiry
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofSeconds(10)).thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > STATIC_LIMIT) {
                        log.warn("LOAD SHEDDING >> Limit reached ({}). Rejecting request.", count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS); // Error 429
                        return exchange.getResponse().setComplete();
                    }

                    log.info("GATEKEEPER >> Request allowed. Count: {}", count);
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -1; // Ensures this runs before routing logic
    }
}