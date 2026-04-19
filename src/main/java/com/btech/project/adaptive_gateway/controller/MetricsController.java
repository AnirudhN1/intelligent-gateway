package com.btech.project.adaptive_gateway.controller;

import com.btech.project.adaptive_gateway.logic.LSTMPredictor;
import com.btech.project.adaptive_gateway.metrics.ResourceMonitor;
import com.btech.project.adaptive_gateway.logic.FuzzyController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@Slf4j
public class MetricsController {

    private final FuzzyController fuzzyController;
    // The Global State (Volatile ensures thread-safety across concurrent requests)
    public static volatile String ACTIVE_MODE = "REACTIVE";

    public MetricsController(FuzzyController fuzzyController) {
        this.fuzzyController = fuzzyController;
    }

    @GetMapping("/api/stats")
    public Map<String, Object> getStats() {
        double cpu = ResourceMonitor.latestCpu;
        int currentLimit;

        // Ensure the dashboard limit matches the actual active brain
        if ("PROACTIVE".equals(ACTIVE_MODE)) {
            double[] window = ResourceMonitor.cpuHistory.stream().mapToDouble(Double::doubleValue).toArray();
            // If we don't have 5 readings yet, fallback to reactive math
            currentLimit = window.length == 5 ?
                    fuzzyController.calculateDynamicLimit(LSTMPredictor.latestPrediction) :
                    fuzzyController.calculateDynamicLimit(cpu);
        } else {
            currentLimit = fuzzyController.calculateDynamicLimit(cpu);
            LSTMPredictor.latestPrediction = cpu; // Sync them in reactive mode
        }

        return Map.of(
                "cpu", cpu,
                "limit", currentLimit,
                "prediction", LSTMPredictor.latestPrediction, // Send the AI forecast!
                "mode", ACTIVE_MODE
        );
    }

    // NEW: Endpoint to toggle the brain
    @PostMapping("/api/mode")
    public String setMode(@RequestParam String mode) {
        if (mode.equals("REACTIVE") || mode.equals("PROACTIVE")) {
            ACTIVE_MODE = mode;
            log.info("SYSTEM OVERRIDE >> Brain switched to {} MODE", ACTIVE_MODE);
        }
        return ACTIVE_MODE;
    }

    @GetMapping("/api/stress-test")
    public String runStressTest() {
        org.springframework.web.reactive.function.client.WebClient webClient =
                org.springframework.web.reactive.function.client.WebClient.create();

        log.info("SENTRYGATE // INTERNAL_ATTACK_SEQUENCE_START (SUSTAINED & HEAVY)");

        reactor.core.publisher.Flux.interval(java.time.Duration.ofMillis(10))
                .onBackpressureDrop() // <--- BACKPRESSURE SHIELD: Drop requests if the client is choking
                .take(1000)
                // INCREASED CONCURRENCY: Allow up to 1000 simultaneous network calls
                .flatMap(i -> webClient.get()
                                .uri("http://localhost:8080/stress/test")
                                .retrieve()
                                .onStatus(status -> status.value() == 429, response -> reactor.core.publisher.Mono.empty())
                                .toBodilessEntity()
                                .onErrorResume(e -> reactor.core.publisher.Mono.empty()),
                        1000
                )
                .subscribe(
                        success -> {},
                        error -> log.error("Attack sequence error: " + error.getMessage()),
                        () -> log.info("SENTRYGATE // ATTACK_SEQUENCE_COMPLETE")
                );
        return "Sustained heavy attack simulation running... watch the dashboard.";
    }
}