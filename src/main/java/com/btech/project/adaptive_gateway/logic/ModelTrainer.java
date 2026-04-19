package com.btech.project.adaptive_gateway.logic;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.LastTimeStep;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModelTrainer {

    public static final int WINDOW_SIZE = 5;

    public static void main(String[] args) throws Exception {
        System.out.println("🤖 SENTRYGATE AI DOJO // INITIALIZING...");

// 1. Read CSV Data
        List<String> lines = Files.readAllLines(Paths.get("telemetry.csv"));
        List<Double> cpuReadings = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            if (parts.length == 2) {
                double val = Double.parseDouble(parts[1]);

                // --- THE SANITY CHECK ---
                // Skip negative glitches and cap at 100%
                if (val < 0) continue;
                if (val > 100) val = 100.0;

                cpuReadings.add(val / 100.0);
            }
        }

        System.out.println("📊 Loaded " + cpuReadings.size() + " telemetry records.");

        int numSamples = cpuReadings.size() - WINDOW_SIZE;

        // Input remains 3D: [BatchSize, Features, TimeSteps]
        INDArray input = Nd4j.zeros(numSamples, 1, WINDOW_SIZE);
        // THE FIX: Labels become 2D: [BatchSize, OutputValues]
        INDArray labels = Nd4j.zeros(numSamples, 1);

        for (int i = 0; i < numSamples; i++) {
            for (int j = 0; j < WINDOW_SIZE; j++) {
                input.putScalar(new int[]{i, 0, j}, cpuReadings.get(i + j));
            }
            // Populate the 2D label
            labels.putScalar(new int[]{i, 0}, cpuReadings.get(i + WINDOW_SIZE));
        }

        DataSet trainingData = new DataSet(input, labels);

        // 3. THE FIX: Many-to-One Architecture
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .updater(new Adam(0.01))
                .list()
                // Wrap the LSTM in LastTimeStep
                .layer(new LastTimeStep(new LSTM.Builder().nIn(1).nOut(16)
                        .activation(Activation.TANH)
                        .weightInit(WeightInit.XAVIER)
                        .build()))
                // Use a standard FeedForward OutputLayer instead of RnnOutputLayer
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(16).nOut(1).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        // Print score every 10 iterations
        model.setListeners(new ScoreIterationListener(10));

        System.out.println("🧠 BEGINNING NEURAL NETWORK TRAINING...");
        int epochs = 150;
        for (int i = 0; i < epochs; i++) {
            model.fit(trainingData);
        }

        // 4. Save the Brain
        File locationToSave = new File("sentrygate_lstm.zip");
        model.save(locationToSave, false);

        System.out.println("✅ TRAINING COMPLETE. Model saved to: " + locationToSave.getAbsolutePath());
    }
}