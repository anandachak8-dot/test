package com.example;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    private static final int ROUNDS = 100;
    private static final long INTERVAL_MS = 3 * 60 * 1000; // 3 minutes
    private static final List<String> downloadedFilePaths = new ArrayList<>();

    public static void main(String[] args) {
        NseDownloader downloader = new NseDownloader();

        for (int i = 0; i < ROUNDS; i++) {
            try {
                System.out.println("Round " + (i + 1) + "/" + ROUNDS + ": Fetching data...");

                // 1. Fetch data
                NseResponse response = downloader.fetchData();
                System.out.println("Data fetched successfully.");

                // 2. Save data to CSV
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String filename = "active_contracts_" + timestamp + ".csv";
                saveToCsv(response, filename);
                downloadedFilePaths.add(filename);
                System.out.println("Data saved to " + filename);

                // 3. Analyze data
                AnalysisService.analyze(downloadedFilePaths);


                // 4. Wait for the next interval
                if (i < ROUNDS - 1) {
                    System.out.println("Waiting for 3 minutes...");
                    Thread.sleep(INTERVAL_MS);
                }

            } catch (IOException e) {
                System.err.println("An error occurred during download or file operation: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("The sleep interval was interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore the interrupted status
                break; // Exit the loop if interrupted
            }
        }
        System.out.println("Monitoring complete after " + ROUNDS + " rounds.");
    }

    private static void saveToCsv(NseResponse nseResponse, String filename) throws IOException {
        if (nseResponse == null || nseResponse.getRecords() == null || nseResponse.getRecords().getData() == null) {
            System.out.println("No data to save.");
            return;
        }

        List<Data> dataList = nseResponse.getRecords().getData();

        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            // Write header
            String[] header = {"StrikePrice", "Call_OI", "Put_OI"};
            writer.writeNext(header);

            // Write data
            for (Data data : dataList) {
                if (data.getCallOption() != null && data.getPutOption() != null) {
                    String[] row = {
                        String.valueOf(data.getStrikePrice()),
                        String.valueOf(data.getCallOption().getOpenInterest()),
                        String.valueOf(data.getPutOption().getOpenInterest())
                    };
                    writer.writeNext(row);
                }
            }
        }
    }
}
