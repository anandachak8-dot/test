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
    private static int roundCounter = 0;

    public NseDownloader() {
        // No-op
    }

    public NseResponse fetchData() throws IOException {
        roundCounter++;
        System.out.println("Fetching data from local sample file: " + SAMPLE_DATA_FILE + " (Round " + roundCounter + ")");
        File file = new File(SAMPLE_DATA_FILE);
        if (!file.exists()) {
            throw new IOException("Sample data file not found: " + SAMPLE_DATA_FILE);
        }
        String content = new String(Files.readAllBytes(file.toPath()));
        NseResponse response = objectMapper.readValue(content, NseResponse.class);
        response.setTimestamp(System.currentTimeMillis());

        // Simulate data changes
        simulateDataChange(response);

        return response;
    }

    private void simulateDataChange(NseResponse response) {
        if (response == null || response.getFiltered() == null || response.getFiltered().getData() == null) {
            return;
        }

        for (Data data : response.getFiltered().getData()) {
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

            // On round 4, introduce a large change to test the alert
            if (roundCounter == 4 && data.getCallOption() != null) {
                System.out.println(">>> Introducing a large OI change for testing alerts. <<<");
                double oi = data.getCallOption().getOpenInterest();
                data.getCallOption().setOpenInterest(oi * 1.5); // 50% increase
            }
        }
    }
}
