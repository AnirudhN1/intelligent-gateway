package com.btech.project.adaptive_gateway.controller;

import com.btech.project.adaptive_gateway.metrics.ResourceMonitor;
import com.btech.project.adaptive_gateway.logic.FuzzyController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@Slf4j
public class MetricsController {

    private final FuzzyController fuzzyController;

    public MetricsController(FuzzyController fuzzyController) {
        this.fuzzyController = fuzzyController;
    }

    @GetMapping("/api/stats")
    public Map<String, Object> getStats() {
        double cpu = ResourceMonitor.latestCpu;
        return Map.of(
                "cpu", cpu,
                "limit", fuzzyController.calculateDynamicLimit(cpu)
        );
    }

    @GetMapping("/api/stress-test")
    public String runStressTest() {
        org.springframework.web.reactive.function.client.WebClient webClient =
                org.springframework.web.reactive.function.client.WebClient.create();

        log.info("SENTRYGATE // INTERNAL_ATTACK_SEQUENCE_START (SUSTAINED)");

        // Fire 1 request every 50ms for 15 seconds (Total ~300 requests)
        reactor.core.publisher.Flux.interval(java.time.Duration.ofMillis(50))
                .take(300)
                .flatMap(i -> webClient.get()
                        .uri("http://localhost:8080/stress/test")
                        .retrieve()
                        .onStatus(status -> status.value() == 429, response -> {
                            return reactor.core.publisher.Mono.empty(); // Swallow the 429s quietly
                        })
                        .toBodilessEntity()
                        .onErrorResume(e -> reactor.core.publisher.Mono.empty()) // Handle closed connections safely
                )
                .subscribe(
                        success -> {},
                        error -> log.error("Attack sequence error: " + error.getMessage()),
                        () -> log.info("SENTRYGATE // ATTACK_SEQUENCE_COMPLETE")
                );

        return "Sustained attack simulation running... watch the dashboard.";
    }

}