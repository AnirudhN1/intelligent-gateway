package com.btech.project.adaptive_gateway.logic;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FuzzyController {

    // Define our "Crisp" Output Singletons (The limits for each state)
    private static final int LIMIT_LOW_STRESS = 50;
    private static final int LIMIT_MED_STRESS = 20;
    private static final int LIMIT_HIGH_STRESS = 5;

    public int calculateDynamicLimit(double cpuUsage) {

        // STEP 1: Fuzzification (Calculate Degree of Membership)
        double muLow = getLowStressMembership(cpuUsage);
        double muMed = getMediumStressMembership(cpuUsage);
        double muHigh = getHighStressMembership(cpuUsage);

        // STEP 2 & 3: Inference and Defuzzification (Weighted Average)
        // Formula: (w1*x1 + w2*x2 + w3*x3) / (w1 + w2 + w3)
        double numerator = (muLow * LIMIT_LOW_STRESS) +
                (muMed * LIMIT_MED_STRESS) +
                (muHigh * LIMIT_HIGH_STRESS);

        double denominator = muLow + muMed + muHigh;

        // Fallback protection (though mathematically impossible with our sets)
        if (denominator == 0.0) return LIMIT_HIGH_STRESS;

        int dynamicLimit = (int) Math.round(numerator / denominator);

        log.debug("FUZZY MATH >> CPU: {}% | muLow: {}, muMed: {}, muHigh: {} | Calculated Limit: {}",
                cpuUsage, String.format("%.2f", muLow), String.format("%.2f", muMed), String.format("%.2f", muHigh), dynamicLimit);

        // Ensure we never drop below 1 request per second to keep the gateway alive
        return Math.max(1, dynamicLimit);
    }

    // --- MEMBERSHIP FUNCTIONS (Trapezoidal / Triangular Overlaps) ---

    private double getLowStressMembership(double cpu) {
        if (cpu <= 30.0) return 1.0;
        if (cpu > 30.0 && cpu < 60.0) return (60.0 - cpu) / 30.0; // Ramps down
        return 0.0;
    }

    private double getMediumStressMembership(double cpu) {
        // 1. Guard clause: Filter out completely out-of-bounds values
        if (cpu <= 30.0 || cpu >= 85.0) return 0.0;

        // 2. If it reaches here, we GUARANTEE it is > 30.0 and < 85.0
        if (cpu <= 60.0) {
            return (cpu - 30.0) / 30.0; // Ramps up (30 to 60)
        } else {
            return (85.0 - cpu) / 25.0; // Ramps down (60 to 85)
        }
    }

    private double getHighStressMembership(double cpu) {
        if (cpu <= 60.0) return 0.0;
        if (cpu > 60.0 && cpu < 85.0) return (cpu - 60.0) / 25.0;  // Ramps up
        if (cpu >= 85.0) return 1.0;
        return 0.0;
    }
}