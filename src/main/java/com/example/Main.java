package com.example;

import java.io.IOException;
import java.sql.SQLException;

public class Main {

    private static final int ROUNDS = 100;
    private static final long INTERVAL_MS = 60 * 1000; // 1 minute

    public static void main(String[] args) {
        NseDownloader downloader = new NseDownloader();
        DatabaseService dbService = new DatabaseService();

        try {
            dbService.initialize();

            for (int i = 0; i < ROUNDS; i++) {
                System.out.println("Round " + (i + 1) + "/" + ROUNDS + ": Fetching data...");

                // 1. Fetch data
                NseResponse response = downloader.fetchData();
                System.out.println("Data fetched.");

                // 2. Store data in H2 database
                dbService.insertData(response);
                System.out.println("Data stored in H2 database.");

                // 3. Analyze data
                AnalysisService.analyze(dbService.getConnection());

                // 4. Wait for the next interval
                if (i < ROUNDS - 1) {
                    System.out.println("Waiting for 1 minute...");
                    Thread.sleep(INTERVAL_MS);
                }
            }

        } catch (IOException | SQLException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("The sleep interval was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            try {
                dbService.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Monitoring complete after " + ROUNDS + " rounds.");
    }
}
