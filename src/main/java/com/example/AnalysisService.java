package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AnalysisService {

    private static final double ALERT_THRESHOLD = 30.0; // 30%
    private static final int[] INTERVALS_IN_MINUTES = {3, 5, 10, 15};

    public static void analyze(Connection connection) {
        System.out.println("--- Starting Analysis at " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ---");
        try {
            long latestTimestamp = getLatestTimestamp(connection);
            if (latestTimestamp == 0) {
                System.out.println("Not enough data to perform analysis.");
                return;
            }

            Map<Double, OiTotals> latestOiPerStrike = getOiPerStrikeForTimestamp(connection, latestTimestamp);

            for (int minutesAgo : INTERVALS_IN_MINUTES) {
                long historicalTimestamp = findHistoricalTimestamp(connection, latestTimestamp, minutesAgo);
                if (historicalTimestamp != 0) {
                    Map<Double, OiTotals> historicalOiPerStrike = getOiPerStrikeForTimestamp(connection, historicalTimestamp);
                    System.out.printf("Comparing with data from ~%d minutes ago...\n", minutesAgo);
                    compareAndAlert(latestOiPerStrike, historicalOiPerStrike, minutesAgo);
                } else {
                    System.out.printf("No data found for ~%d minutes ago. Skipping comparison.\n", minutesAgo);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("--- Analysis Complete ---");
    }

    private static void compareAndAlert(Map<Double, OiTotals> latestData, Map<Double, OiTotals> historicalData, int minutesAgo) {
        for (Map.Entry<Double, OiTotals> entry : latestData.entrySet()) {
            double strikePrice = entry.getKey();
            OiTotals latestOIs = entry.getValue();

            if (historicalData.containsKey(strikePrice)) {
                OiTotals historicalOIs = historicalData.get(strikePrice);
                checkAndAlert(strikePrice, "Call", latestOIs.getTotalCallOi(), historicalOIs.getTotalCallOi(), minutesAgo);
                checkAndAlert(strikePrice, "Put", latestOIs.getTotalPutOi(), historicalOIs.getTotalPutOi(), minutesAgo);
            }
        }
    }

    private static void checkAndAlert(double strikePrice, String optionType, double newOI, double oldOI, int minutesAgo) {
        if (oldOI == 0) {
            if (newOI > 0) {
                System.out.printf("ALERT: Strike Price %.2f - %s OI is new (was 0) at %s.\n",
                        strikePrice, optionType, new SimpleDateFormat("HH:mm:ss").format(new Date()));
            }
            return;
        }

        double percentChange = ((newOI - oldOI) / oldOI) * 100;

        if (Math.abs(percentChange) > ALERT_THRESHOLD) {
            String direction = percentChange > 0 ? "increased" : "decreased";
            System.out.printf("!!! ALERT !!! Strike Price %.2f - %s OI has %s by %.2f%% in the last %d minutes (at %s).\n",
                    strikePrice, optionType, direction, Math.abs(percentChange), minutesAgo, new SimpleDateFormat("HH:mm:ss").format(new Date()));
        }
    }

    private static long getLatestTimestamp(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(timestamp) FROM OPTION_DATA")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    private static long findHistoricalTimestamp(Connection conn, long latestTimestamp, int minutesAgo) throws SQLException {
        long targetTimestamp = latestTimestamp - (long) minutesAgo * 60 * 1000;
        String sql = "SELECT MAX(timestamp) FROM OPTION_DATA WHERE timestamp <= ? AND timestamp < ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, targetTimestamp);
            pstmt.setLong(2, latestTimestamp);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    private static Map<Double, OiTotals> getOiPerStrikeForTimestamp(Connection conn, long timestamp) throws SQLException {
        Map<Double, OiTotals> oiPerStrike = new HashMap<>();
        String sql = "SELECT strikePrice, optionType, openInterest FROM OPTION_DATA WHERE timestamp = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, timestamp);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                double strikePrice = rs.getDouble(1);
                String optionType = rs.getString(2);
                double openInterest = rs.getDouble(3);

                oiPerStrike.putIfAbsent(strikePrice, new OiTotals(0, 0));
                OiTotals totals = oiPerStrike.get(strikePrice);

                if ("CE".equals(optionType)) {
                    totals.setTotalCallOi(openInterest);
                } else if ("PE".equals(optionType)) {
                    totals.setTotalPutOi(openInterest);
                }
            }
        }
        return oiPerStrike;
    }

    private static class OiTotals {
        private double totalCallOi;
        private double totalPutOi;

        public OiTotals(double totalCallOi, double totalPutOi) {
            this.totalCallOi = totalCallOi;
            this.totalPutOi = totalPutOi;
        }

        public double getTotalCallOi() { return totalCallOi; }
        public void setTotalCallOi(double totalCallOi) { this.totalCallOi = totalCallOi; }
        public double getTotalPutOi() { return totalPutOi; }
        public void setTotalPutOi(double totalPutOi) { this.totalPutOi = totalPutOi; }
    }
}
