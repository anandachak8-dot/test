package com.example;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int TOTAL_RUNS = 100;
    private static final long INTERVAL_MINUTES = 1;
    private static final String SYMBOL = "NIFTY"; // Configurable symbol

    public static void main(String[] args) {
        NseDownloader downloader = new NseDownloader();
        DatabaseService dbService = new DatabaseService();

        try {
            dbService.initialize();
            System.out.println("Starting monitoring for symbol: " + SYMBOL);

            for (int i = 0; i < TOTAL_RUNS; i++) {
                System.out.println("Round " + (i + 1) + "/" + TOTAL_RUNS + ": Processing " + SYMBOL);
                NseResponse response = downloader.fetchData(SYMBOL);
                dbService.insertData(response, SYMBOL);
                System.out.println("Data for " + SYMBOL + " stored in H2 database. Underlying Value: " + response.getRecords().getUnderlyingValue());
                AnalysisService.analyze(dbService.getConnection(), SYMBOL, response.getRecords());

                if (i < TOTAL_RUNS - 1) {
                    System.out.println("Waiting for " + INTERVAL_MINUTES + " minute(s)...");
                    Thread.sleep(INTERVAL_MINUTES * 60 * 1000);
                }
            }
        } catch (IOException | SQLException | InterruptedException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } finally {
            dbService.close();
        }
        System.out.println("Monitoring complete.");
    }
}
