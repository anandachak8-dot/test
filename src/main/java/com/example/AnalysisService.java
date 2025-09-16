package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AnalysisService {

    private static final double ALERT_THRESHOLD = 30.0; // 30%
    private static final int[] INTERVALS_IN_MINUTES = {3, 5, 10, 15};

    public static void analyze(Connection connection) {
        System.out.println("--- Starting Analysis ---");
        try {
            long latestTimestamp = getLatestTimestamp(connection);
            if (latestTimestamp == 0) {
                System.out.println("Not enough data to perform analysis.");
                return;
            }

            OiTotals latestOi = getOiTotalsForTimestamp(connection, latestTimestamp);
            if (latestOi == null) {
                System.out.println("Could not calculate OI for the latest data point.");
                return;
            }

            for (int minutesAgo : INTERVALS_IN_MINUTES) {
                long historicalTimestamp = findHistoricalTimestamp(connection, latestTimestamp, minutesAgo);
                if (historicalTimestamp != 0) {
                    OiTotals historicalOi = getOiTotalsForTimestamp(connection, historicalTimestamp);
                    if (historicalOi != null) {
                        System.out.printf("Comparing with data from ~%d minutes ago...\n", minutesAgo);
                        compareAndAlert(latestOi, historicalOi, minutesAgo);
                    }
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
        String sql = "SELECT MAX(timestamp) FROM OPTION_DATA WHERE timestamp <= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, targetTimestamp);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    private static OiTotals getOiTotalsForTimestamp(Connection conn, long timestamp) throws SQLException {
        String sql = "SELECT optionType, SUM(openInterest) FROM OPTION_DATA WHERE timestamp = ? GROUP BY optionType";
        double totalCallOi = 0;
        double totalPutOi = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, timestamp);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String optionType = rs.getString(1);
                double totalOi = rs.getDouble(2);
                if ("CE".equals(optionType)) {
                    totalCallOi = totalOi;
                } else if ("PE".equals(optionType)) {
                    totalPutOi = totalOi;
                }
            }
        }
        return new OiTotals(totalCallOi, totalPutOi);
    }

    private static void compareAndAlert(OiTotals latestOi, OiTotals historicalOi, int minutesAgo) {
        checkAndAlert("Call", latestOi.getTotalCallOi(), historicalOi.getTotalCallOi(), minutesAgo);
        checkAndAlert("Put", latestOi.getTotalPutOi(), historicalOi.getTotalPutOi(), minutesAgo);
    }

    private static void checkAndAlert(String optionType, double newOI, double oldOI, int minutesAgo) {
        if (oldOI == 0) {
            if (newOI > 0) {
                System.out.printf("ALERT: Total %s OI is new (was 0) in the last %d minutes.\n", optionType, minutesAgo);
            }
            return;
        }

        double percentChange = ((newOI - oldOI) / oldOI) * 100;

        if (Math.abs(percentChange) > ALERT_THRESHOLD) {
            String direction = percentChange > 0 ? "increased" : "decreased";
            System.out.printf("!!! ALERT !!! Total %s OI has %s by %.2f%% in the last %d minutes. (From %.0f to %.0f)\n",
                    optionType, direction, Math.abs(percentChange), minutesAgo, oldOI, newOI);
        } else {
            System.out.printf("INFO: Total %s OI change in last %d minutes is %.2f%% (not over threshold).\n",
                    optionType, minutesAgo, percentChange);
        }
    }

    private static class OiTotals {
        private final double totalCallOi;
        private final double totalPutOi;

        public OiTotals(double totalCallOi, double totalPutOi) {
            this.totalCallOi = totalCallOi;
            this.totalPutOi = totalPutOi;
        }

        public double getTotalCallOi() {
            return totalCallOi;
        }

        public double getTotalPutOi() {
            return totalPutOi;
        }
    }
}
