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

        log.info("SENTRYGATE // INTERNAL_ATTACK_SEQUENCE_START");

        for (int i = 0; i < 500; i++) {
            webClient.get()
                    .uri("http://localhost:8080/stress/test")
                    .retrieve()
                    .onStatus(status -> status.value() == 429, response -> {
                        // We handle the 429 quietly instead of throwing an error
                        return reactor.core.publisher.Mono.empty();
                    })
                    .toBodilessEntity()
                    .subscribe(
                            success -> log.debug("Request Allowed"),
                            error -> {} // This silences the "Operator called default onErrorDropped"
                    );
        }
        return "Attack simulation running...";
    }
}