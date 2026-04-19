package com.btech.project.adaptive_gateway.logic;

import com.btech.project.adaptive_gateway.metrics.ResourceMonitor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import java.io.File;

@Component
@Slf4j
public class LSTMPredictor {
    public static double latestPrediction = 0.0;
    private MultiLayerNetwork model;
    private final FuzzyController fuzzyController;

    // --- SMOOTHING PARAMETERS ---
    private double smoothedPrediction = 0.0;
    private static final double ALPHA = 0.2; // The "Shock Absorber" (Lower = Smoother)

    public LSTMPredictor(FuzzyController fuzzyController) {
        this.fuzzyController = fuzzyController;
    }

    @PostConstruct
    public void init() {
        try {
            File modelFile = new File("sentrygate_lstm.zip");
            if (modelFile.exists()) {
                model = ModelSerializer.restoreMultiLayerNetwork(modelFile);
                log.info("🤖 SENTRYGATE AI >> LSTM Brain Loaded.");
            }
        } catch (Exception e) {
            log.error("Failed to load LSTM model", e);
        }
    }

    public int predictDynamicLimit(double[] slidingWindow) {
        if (model == null) return 50;

        try {
            INDArray input = Nd4j.zeros(1, 1, ModelTrainer.WINDOW_SIZE);
            for (int i = 0; i < ModelTrainer.WINDOW_SIZE; i++) {
                input.putScalar(new int[]{0, 0, i}, slidingWindow[i] / 100.0);
            }

            INDArray output = model.output(input);
            double rawPrediction = output.getDouble(0) * 100.0;

            // TREND AMPLIFICATION: Calculate the slope and boost it
            double currentCpu = slidingWindow[slidingWindow.length - 1];
            double trend = rawPrediction - currentCpu;

            // If the trend is going up, multiply it. If it's going down, trust it.
            double boostFactor = (trend > 0) ? 3.0 : 1.0;
            latestPrediction = currentCpu + (trend * boostFactor);

            // Clamp between 0 and 100
            latestPrediction = Math.max(0, Math.min(100, latestPrediction));

            return fuzzyController.calculateDynamicLimit(latestPrediction);

        } catch (Exception e) {
            log.error("AI Prediction Error", e);
            return 50;
        }
    }
}