package com.btech.project.adaptive_gateway.filter;

import com.btech.project.adaptive_gateway.logic.FuzzyController;
import com.btech.project.adaptive_gateway.metrics.ResourceMonitor;
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
    private final FuzzyController fuzzyController;

    public AdaptiveLoadShedderFilter(ReactiveStringRedisTemplate redisTemplate, FuzzyController fuzzyController) {
        this.redisTemplate = redisTemplate;
        this.fuzzyController = fuzzyController;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Identify Criticality ("Flash Sale" Logic)
        boolean isCritical = path.contains("/payment") || path.contains("/checkout");

        // 2. Separate the Redis state so standard traffic doesn't steal the payment quota
        String redisKey = isCritical ? "quota_critical" : "quota_standard";

        // 3. Calculate dynamic limit from the Fuzzy Engine
        int baseLimit = fuzzyController.calculateDynamicLimit(ResourceMonitor.latestCpu);

        // 4. Critical routes get 5x the allowance of standard routes during a crisis
        int effectiveLimit = isCritical ? (baseLimit * 5) : baseLimit;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(10)).thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > effectiveLimit) {
                        log.warn("SHEDDING >> Route: {} | Priority: {} | CPU: {}% | Rejecting request.",
                                path, isCritical ? "HIGH" : "LOW", String.format("%.2f", ResourceMonitor.latestCpu));
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    // Prevent dashboard polling from spamming the console logs
                    if (!path.contains("/api/stats")) {
                        log.info("GATEKEEPER >> Allowed Route: {} | Priority: {} | Count: {}/{}",
                                path, isCritical ? "HIGH" : "LOW", count, effectiveLimit);
                    }

                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}