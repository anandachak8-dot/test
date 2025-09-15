package com.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisService {

    private static final double ALERT_THRESHOLD = 30.0; // 30%

    public static void analyze(List<String> filePaths) {
        if (filePaths.size() < 2) {
            System.out.println("Not enough data to perform analysis. Need at least 2 files.");
            return;
        }

        System.out.println("--- Starting Analysis ---");

        try {
            String latestFile = filePaths.get(filePaths.size() - 1);
            Map<Double, double[]> latestData = readCsvToMap(latestFile);

            // Compare with up to 3 previous files
            for (int i = 1; i <= 3 && (filePaths.size() - 1 - i) >= 0; i++) {
                String historicalFile = filePaths.get(filePaths.size() - 1 - i);
                Map<Double, double[]> historicalData = readCsvToMap(historicalFile);
                int minutesAgo = i * 3;

                compareData(latestData, historicalData, minutesAgo);
            }

        } catch (IOException | CsvValidationException e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("--- Analysis Complete ---");
    }

    private static void compareData(Map<Double, double[]> latestData, Map<Double, double[]> historicalData, int minutesAgo) {
        for (Map.Entry<Double, double[]> entry : latestData.entrySet()) {
            double strikePrice = entry.getKey();
            double[] latestOIs = entry.getValue();

            if (historicalData.containsKey(strikePrice)) {
                double[] historicalOIs = historicalData.get(strikePrice);
                // 0: Call OI, 1: Put OI

                // Compare Call OI
                checkAndAlert(strikePrice, "Call", latestOIs[0], historicalOIs[0], minutesAgo);
                // Compare Put OI
                checkAndAlert(strikePrice, "Put", latestOIs[1], historicalOIs[1], minutesAgo);
            }
        }
    }

    private static void checkAndAlert(double strikePrice, String optionType, double newOI, double oldOI, int minutesAgo) {
        if (oldOI == 0) {
            if (newOI > 0) {
                System.out.printf("ALERT: Strike Price %.2f - %s OI is new (was 0) in the last %d minutes.\n", strikePrice, optionType, minutesAgo);
            }
            return;
        }

        double percentChange = ((newOI - oldOI) / oldOI) * 100;

        if (Math.abs(percentChange) > ALERT_THRESHOLD) {
            String direction = percentChange > 0 ? "increased" : "decreased";
            System.out.printf("ALERT: Strike Price %.2f - %s OI has %s by %.2f%% in the last %d minutes. (From %.0f to %.0f)\n",
                    strikePrice, optionType, direction, Math.abs(percentChange), minutesAgo, oldOI, newOI);
        }
    }

    private static Map<Double, double[]> readCsvToMap(String filePath) throws IOException, CsvValidationException {
        Map<Double, double[]> dataMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            reader.readNext(); // Skip header

            while ((nextLine = reader.readNext()) != null) {
                // Expects 4 columns now: StrikePrice, Call_OI, Put_OI, ExpiryDate
                if (nextLine.length < 3) continue; // Skip malformed rows
                try {
                    double strikePrice = Double.parseDouble(nextLine[0]);
                    double callOI = Double.parseDouble(nextLine[1]);
                    double putOI = Double.parseDouble(nextLine[2]);
                    dataMap.put(strikePrice, new double[]{callOI, putOI});
                } catch (NumberFormatException e) {
                    // Ignore rows with malformed numbers
                }
            }
        }
        return dataMap;
    }
}
