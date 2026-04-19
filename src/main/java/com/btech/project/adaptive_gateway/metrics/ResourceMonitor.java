package com.btech.project.adaptive_gateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Service
@Slf4j
public class ResourceMonitor {
    private long lastLogTime = 0;
    private final MeterRegistry meterRegistry;
    public static double latestCpu = 0.0;
    // NEW: The Sliding Window memory for the AI
    public static final java.util.concurrent.ConcurrentLinkedDeque<Double> cpuHistory = new java.util.concurrent.ConcurrentLinkedDeque<>();

    // The target file for our ML dataset
    private static final Path CSV_PATH = Paths.get("telemetry.csv");

    public ResourceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeCsv();
    }

    private void initializeCsv() {
        try {
            if (!Files.exists(CSV_PATH)) {
                // Write the CSV Headers
                Files.writeString(CSV_PATH, "timestamp,cpu_percentage\n", StandardOpenOption.CREATE);
                log.info("TELEMETRY >> Created new telemetry.csv for ML training data collection.");
            }
        } catch (IOException e) {
            log.error("TELEMETRY >> Failed to initialize CSV", e);
        }
    }

    // Runs every 1 second
    @Scheduled(fixedRate = 1000)
    public void logSystemVitals() {
        // GHOST LOCK: Prevent multiple writes in the same second
        long now = System.currentTimeMillis();
        if (now - lastLogTime < 900) return;
        lastLogTime = now;
        try {
            double cpu = meterRegistry.get("system.cpu.usage").gauge().value() * 100;
            // GLITCH FIX: If the hardware glitches to -100, reuse the last known safe value
            if (cpu < 0 || Double.isNaN(cpu)) {
                cpu = latestCpu;
            }
            latestCpu = cpu;

            // --- SLIDING WINDOW LOGIC ---
            cpuHistory.addLast(cpu);
            if (cpuHistory.size() > 5) {
                cpuHistory.removeFirst(); // Keep exactly the last 5 readings
            }
            // ----------------------------

            double memory = meterRegistry.get("jvm.memory.used").gauge().value() / (1024 * 1024);

            log.info("HEALTH CHECK >> CPU Usage: {}% | JVM Memory: {} MB",
                    String.format("%.2f", cpu),
                    String.format("%.2f", memory));

            // ML DATA COLLECTION: Append current state to the flight recorder
            String csvRow = Instant.now().toEpochMilli() + "," + String.format("%.2f", cpu) + "\n";
            Files.writeString(CSV_PATH, csvRow, StandardOpenOption.APPEND);

        } catch (Exception e) {
            log.warn("Monitoring Agent is warming up...");
        }
    }
}