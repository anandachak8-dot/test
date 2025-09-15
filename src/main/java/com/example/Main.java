package com.example;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

                // 2. Filter data by the first expiry date
                List<Data> filteredData = filterDataByFirstExpiry(response);
                if (filteredData == null) {
                    System.out.println("Could not filter data. Exiting round.");
                    continue; // Skip to the next round
                }

                // 3. Save filtered data to CSV
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String filename = "active_contracts_" + timestamp + ".csv";
                saveToCsv(filteredData, filename);
                downloadedFilePaths.add(filename);
                System.out.println("Filtered data saved to " + filename);

                // 4. Analyze data
                AnalysisService.analyze(downloadedFilePaths);

                // 5. Wait for the next interval
                if (i < ROUNDS - 1) {
                    System.out.println("Waiting for 3 minutes...");
                    Thread.sleep(INTERVAL_MS);
                }

            } catch (IOException e) {
                System.err.println("An error occurred during download or file operation: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("The sleep interval was interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Monitoring complete after " + ROUNDS + " rounds.");
    }

    private static List<Data> filterDataByFirstExpiry(NseResponse nseResponse) {
        if (nseResponse == null || nseResponse.getFiltered() == null ||
            nseResponse.getFiltered().getExpiryDates() == null || nseResponse.getFiltered().getExpiryDates().isEmpty() ||
            nseResponse.getFiltered().getData() == null) {
            System.out.println("Response is missing necessary data for filtering (filtered, expiryDates, or data).");
            return null;
        }

        String targetExpiryDate = nseResponse.getFiltered().getExpiryDates().get(0);

        List<Data> allData = nseResponse.getFiltered().getData();
        return allData.stream()
                .filter(data -> targetExpiryDate.equals(data.getExpiryDate()))
                .collect(Collectors.toList());
    }

    private static void saveToCsv(List<Data> dataList, String filename) throws IOException {
        if (dataList == null || dataList.isEmpty()) {
            System.out.println("No data to save.");
            return;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            String[] header = {"StrikePrice", "Call_OI", "Put_OI", "ExpiryDate"};
            writer.writeNext(header);

            for (Data data : dataList) {
                if (data.getCallOption() != null && data.getPutOption() != null) {
                    String[] row = {
                        String.valueOf(data.getStrikePrice()),
                        String.valueOf(data.getCallOption().getOpenInterest()),
                        String.valueOf(data.getPutOption().getOpenInterest()),
                        data.getExpiryDate()
                    };
                    writer.writeNext(row);
                }
            }
        }
    }
}
