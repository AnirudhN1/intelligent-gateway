package com.btech.project.adaptive_gateway.filter;

import com.btech.project.adaptive_gateway.logic.FuzzyController; // NEW
import com.btech.project.adaptive_gateway.metrics.ResourceMonitor; // NEW
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
    private final FuzzyController fuzzyController; // 1. Define the Brain

    // 2. Inject both Redis and the Fuzzy Controller here
    public AdaptiveLoadShedderFilter(ReactiveStringRedisTemplate redisTemplate, FuzzyController fuzzyController) {
        this.redisTemplate = redisTemplate;
        this.fuzzyController = fuzzyController;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = "request_count";

        // 3. Get the dynamic limit based on current CPU
        int dynamicLimit = fuzzyController.calculateDynamicLimit(ResourceMonitor.latestCpu);

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofSeconds(10)).thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    // 4. Check against the Dynamic Limit instead of a static number
                    if (count > dynamicLimit) {
                        log.warn("ADAPTIVE SHEDDING >> Limit reached ({}). Current CPU: {}%. Rejecting.",
                                dynamicLimit, String.format("%.2f", ResourceMonitor.latestCpu));
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    log.info("GATEKEEPER >> Allowed. Count: {}/{}", count, dynamicLimit);
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}