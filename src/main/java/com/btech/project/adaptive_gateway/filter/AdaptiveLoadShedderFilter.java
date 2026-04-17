package com.btech.project.adaptive_gateway.filter;

import com.btech.project.adaptive_gateway.controller.MetricsController;
import com.btech.project.adaptive_gateway.logic.FuzzyController;
import com.btech.project.adaptive_gateway.logic.LSTMPredictor;
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
    private final LSTMPredictor lstmPredictor; // Inject the ML Brain

    public AdaptiveLoadShedderFilter(ReactiveStringRedisTemplate redisTemplate,
                                     FuzzyController fuzzyController,
                                     LSTMPredictor lstmPredictor) {
        this.redisTemplate = redisTemplate;
        this.fuzzyController = fuzzyController;
        this.lstmPredictor = lstmPredictor;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean isCritical = path.contains("/payment") || path.contains("/checkout");
        String redisKey = isCritical ? "quota_critical" : "quota_standard";

        // THE SWITCH: Ask the active brain for the limit
        int baseLimit;
        if ("PROACTIVE".equals(MetricsController.ACTIVE_MODE)) {
            baseLimit = lstmPredictor.predictDynamicLimit(ResourceMonitor.latestCpu);
        } else {
            baseLimit = fuzzyController.calculateDynamicLimit(ResourceMonitor.latestCpu);
        }

        int effectiveLimit = isCritical ? (baseLimit * 5) : baseLimit;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) return redisTemplate.expire(redisKey, Duration.ofSeconds(10)).thenReturn(count);
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > effectiveLimit) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() { return -1; }
}