package com.btech.project.adaptive_gateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResourceMonitor {

    private final MeterRegistry meterRegistry;
    public static double latestCpu = 0.0;

    public ResourceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // This method runs automatically every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void logSystemVitals() {
        try {
            // 1. Fetch CPU Usage (returns 0.0 to 1.0, so we multiply by 100 for %)
            double cpu = meterRegistry.get("system.cpu.usage").gauge().value() * 100;
            latestCpu = cpu; // Save the measured CPU to the static variable

            // 2. Fetch JVM Memory usage in Megabytes (MB)
            double memory = meterRegistry.get("jvm.memory.used").gauge().value() / (1024 * 1024);

            // 3. Print it to your IntelliJ Console
            log.info("HEALTH CHECK >> CPU Usage: {}% | JVM Memory: {} MB",
                    String.format("%.2f", cpu),
                    String.format("%.2f", memory));

        } catch (Exception e) {
            // Sometimes it takes a few seconds for metrics to warm up on startup
            log.warn("Monitoring Agent is warming up...");
        }
    }
}