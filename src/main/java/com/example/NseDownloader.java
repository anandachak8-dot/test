package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

public class NseDownloader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SAMPLE_DATA_FILE = "sample-data.json";
    private final Random random = new Random();
    private int roundCounter = 0;

    public NseDownloader() {
        // No-op
    }

    public NseResponse fetchData(String symbol) throws IOException {
        roundCounter++;
        System.out.println("Fetching data for " + symbol + " from local sample file: " + SAMPLE_DATA_FILE + " (Simulation Round " + roundCounter + ")");
        File file = new File(SAMPLE_DATA_FILE);
        if (!file.exists()) {
            throw new IOException("Sample data file not found: " + SAMPLE_DATA_FILE);
        }
        String content = new String(Files.readAllBytes(file.toPath()));
        // Create a deep copy by serializing and deserializing
        NseResponse response = objectMapper.readValue(content, NseResponse.class);
        String responseAsString = objectMapper.writeValueAsString(response);
        NseResponse deepCopy = objectMapper.readValue(responseAsString, NseResponse.class);

        deepCopy.setTimestamp(System.currentTimeMillis());

        // Simulate data changes on the deep copy
        simulateDataChange(deepCopy, symbol);

        return deepCopy;
    }

    private void simulateDataChange(NseResponse response, String symbol) {
        if (response == null || response.getRecords() == null || response.getRecords().getData() == null) {
            return;
        }

        for (Data data : response.getRecords().getData()) {
            // Simulate small, random fluctuations for all options
            if (data.getCallOption() != null) {
                double oi = data.getCallOption().getOpenInterest();
                double fluctuation = (random.nextDouble() * 0.1) - 0.05; // between -5% and +5%
                data.getCallOption().setOpenInterest(oi * (1 + fluctuation));
            }
            if (data.getPutOption() != null) {
                double oi = data.getPutOption().getOpenInterest();
                double fluctuation = (random.nextDouble() * 0.1) - 0.05; // between -5% and +5%
                data.getPutOption().setOpenInterest(oi * (1 + fluctuation));
            }

            // On round 4, introduce a large change to test the alert for NIFTY
            if (roundCounter == 4 && "NIFTY".equals(symbol) && data.getCallOption() != null) {
                System.out.println(">>> Introducing a large OI change for NIFTY for testing alerts. <<<");
                double oi = data.getCallOption().getOpenInterest();
                data.getCallOption().setOpenInterest(oi * 1.5); // 50% increase
            }
        }
    }
}
