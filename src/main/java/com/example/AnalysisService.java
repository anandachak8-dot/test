package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisService {

    private static final double ALERT_THRESHOLD = 30.0;
    private static final int[] INTERVALS_IN_MINUTES = {3, 5, 10, 15};

    public static void analyze(Connection connection, String symbol, Records records) {
        System.out.println("--- Starting Analysis for " + symbol + " at " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ---");
        try {
            long latestTimestamp = getLatestTimestamp(connection, symbol);
            if (latestTimestamp == 0) {
                System.out.println("Not enough data to perform analysis for " + symbol);
                return;
            }

            List<Double> relevantStrikes = getRelevantStrikePrices(records.getUnderlyingValue(), records.getStrikePrices());
            System.out.println("Analyzing for strike prices: " + relevantStrikes);

            Map<Double, OiTotals> latestOiPerStrike = getOiPerStrikeForTimestamp(connection, latestTimestamp, symbol, relevantStrikes);

            for (int minutesAgo : INTERVALS_IN_MINUTES) {
                long historicalTimestamp = findHistoricalTimestamp(connection, latestTimestamp, minutesAgo, symbol);
                if (historicalTimestamp != 0) {
                    Map<Double, OiTotals> historicalOiPerStrike = getOiPerStrikeForTimestamp(connection, historicalTimestamp, symbol, relevantStrikes);
                    System.out.printf("Comparing with data for %s from ~%d minutes ago...\n", symbol, minutesAgo);
                    compareAndAlert(latestOiPerStrike, historicalOiPerStrike, minutesAgo, symbol);
                } else {
                    System.out.printf("No data found for %s from ~%d minutes ago. Skipping comparison.\n", symbol, minutesAgo);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during analysis for " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("--- Analysis Complete for " + symbol + " ---");
    }

    private static List<Double> getRelevantStrikePrices(double underlyingValue, List<Double> allStrikes) {
        if (allStrikes == null || allStrikes.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.sort(allStrikes);

        double closestStrike = -1;
        double minDiff = Double.MAX_VALUE;
        for (double strike : allStrikes) {
            double diff = Math.abs(strike - underlyingValue);
            if (diff < minDiff) {
                minDiff = diff;
                closestStrike = strike;
            }
        }

        if (closestStrike == -1) {
            return Collections.emptyList();
        }

        int atmIndex = allStrikes.indexOf(closestStrike);
        int startIndex = Math.max(0, atmIndex - 5);
        int endIndex = Math.min(allStrikes.size() - 1, atmIndex + 5);

        return allStrikes.subList(startIndex, endIndex + 1);
    }

    private static void compareAndAlert(Map<Double, OiTotals> latestData, Map<Double, OiTotals> historicalData, int minutesAgo, String symbol) {
        for (Map.Entry<Double, OiTotals> entry : latestData.entrySet()) {
            double strikePrice = entry.getKey();
            OiTotals latestOIs = entry.getValue();

            if (historicalData.containsKey(strikePrice)) {
                OiTotals historicalOIs = historicalData.get(strikePrice);
                checkAndAlert(strikePrice, "Call", latestOIs.getCallOi(), historicalOIs.getCallOi(), minutesAgo, symbol);
                checkAndAlert(strikePrice, "Put", latestOIs.getPutOi(), historicalOIs.getPutOi(), minutesAgo, symbol);
            }
        }
    }

    private static void checkAndAlert(double strikePrice, String optionType, OiDetail newOi, OiDetail oldOi, int minutesAgo, String symbol) {
        if (oldOi == null || newOi == null) return;

        if (oldOi.getOpenInterest() == 0) {
            if (newOi.getOpenInterest() > 0) {
                System.out.printf("ALERT: [%s] Strike Price %.2f - %s OI is new (was 0) at %s.\n",
                        symbol, strikePrice, optionType, new SimpleDateFormat("HH:mm:ss").format(new Date()));
            }
            return;
        }

        double percentChange = ((newOi.getOpenInterest() - oldOi.getOpenInterest()) / oldOi.getOpenInterest()) * 100;

        if (Math.abs(percentChange) > ALERT_THRESHOLD) {
            String direction = percentChange > 0 ? "increased" : "decreased";
            System.out.printf("!!! ALERT !!! [%s] Strike Price %.2f - %s OI has %s by %.2f%% in the last %d minutes. Price: %.2f, OI went from %.0f to %.0f (at %s).\n",
                    symbol, strikePrice, optionType, direction, Math.abs(percentChange), minutesAgo, newOi.getLastPrice(), oldOi.getOpenInterest(), newOi.getOpenInterest(), new SimpleDateFormat("HH:mm:ss").format(new Date()));
        }
    }

    private static long getLatestTimestamp(Connection conn, String symbol) throws SQLException {
        String sql = "SELECT MAX(timestamp) FROM OPTION_DATA WHERE symbol = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private static long findHistoricalTimestamp(Connection conn, long latestTimestamp, int minutesAgo, String symbol) throws SQLException {
        long targetTimestamp = latestTimestamp - (long) minutesAgo * 60 * 1000;
        String sql = "SELECT MAX(timestamp) FROM OPTION_DATA WHERE symbol = ? AND timestamp <= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            pstmt.setLong(2, targetTimestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private static Map<Double, OiTotals> getOiPerStrikeForTimestamp(Connection conn, long timestamp, String symbol, List<Double> strikes) throws SQLException {
        Map<Double, OiTotals> oiPerStrike = new HashMap<>();
        String placeHolders = String.join(",", Collections.nCopies(strikes.size(), "?"));
        String sql = "SELECT strikePrice, optionType, openInterest, lastPrice FROM OPTION_DATA WHERE timestamp = ? AND symbol = ? AND strikePrice IN (" + placeHolders + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            pstmt.setLong(index++, timestamp);
            pstmt.setString(index++, symbol);
            for (Double strike : strikes) {
                pstmt.setDouble(index++, strike);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double strikePrice = rs.getDouble("strikePrice");
                    String optionType = rs.getString("optionType");
                    double openInterest = rs.getDouble("openInterest");
                    double lastPrice = rs.getDouble("lastPrice");

                    oiPerStrike.putIfAbsent(strikePrice, new OiTotals(null, null));
                    OiTotals totals = oiPerStrike.get(strikePrice);

                    if ("CE".equals(optionType)) {
                        totals.setCallOi(new OiDetail(openInterest, lastPrice));
                    } else if ("PE".equals(optionType)) {
                        totals.setPutOi(new OiDetail(openInterest, lastPrice));
                    }
                }
            }
        }
        return oiPerStrike;
    }

    private static class OiDetail {
        private double openInterest;
        private double lastPrice;
        public OiDetail(double oi, double lp) { this.openInterest = oi; this.lastPrice = lp; }
        public double getOpenInterest() { return openInterest; }
        public double getLastPrice() { return lastPrice; }
    }

    private static class OiTotals {
        private OiDetail callOi;
        private OiDetail putOi;
        public OiTotals(OiDetail co, OiDetail po) { this.callOi = co; this.putOi = po; }
        public OiDetail getCallOi() { return callOi; }
        public void setCallOi(OiDetail callOi) { this.callOi = callOi; }
        public OiDetail getPutOi() { return putOi; }
        public void setPutOi(OiDetail putOi) { this.putOi = putOi; }
    }
}
