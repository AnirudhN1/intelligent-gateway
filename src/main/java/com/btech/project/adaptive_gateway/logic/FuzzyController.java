package com.btech.project.adaptive_gateway.logic;

import org.springframework.stereotype.Component;

@Component
public class FuzzyController {

    public int calculateDynamicLimit(double cpuUsage) {
        // Base limit for a healthy system
        if (cpuUsage < 30.0) return 50;

        // Moderate load: Start tightening the belt
        if (cpuUsage >= 30.0 && cpuUsage < 70.0) return 20;

        // High load: Panic mode / Heavy load shedding
        if (cpuUsage >= 70.0 && cpuUsage < 90.0) return 5;

        // Critical: Shutdown almost everything to keep the kernel alive
        return 1;
    }
}