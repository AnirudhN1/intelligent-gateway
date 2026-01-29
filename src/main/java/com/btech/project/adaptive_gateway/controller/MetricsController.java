package com.btech.project.adaptive_gateway.controller;

import com.btech.project.adaptive_gateway.metrics.ResourceMonitor;
import com.btech.project.adaptive_gateway.logic.FuzzyController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
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
}