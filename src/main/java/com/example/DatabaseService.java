package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private static final String DB_URL = "jdbc:h2:mem:optionchain;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private Connection connection;

    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        createTable();
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS OPTION_DATA (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "symbol VARCHAR(255)," +
                         "timestamp BIGINT," +
                         "expiryDate VARCHAR(255)," +
                         "strikePrice DOUBLE," +
                         "optionType VARCHAR(2)," + // "CE" or "PE"
                         "openInterest DOUBLE," +
                         "lastPrice DOUBLE)";
            stmt.executeUpdate(sql);
        }
    }

    public void insertData(NseResponse response, String symbol) throws SQLException {
        if (response == null || response.getFiltered() == null || response.getFiltered().getData() == null) {
            return;
        }

        String sql = "INSERT INTO OPTION_DATA (symbol, timestamp, expiryDate, strikePrice, optionType, openInterest, lastPrice) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Data data : response.getFiltered().getData()) {
                if (data.getCallOption() != null) {
                    pstmt.setString(1, symbol);
                    pstmt.setLong(2, response.getTimestamp());
                    pstmt.setString(3, data.getExpiryDate());
                    pstmt.setDouble(4, data.getStrikePrice());
                    pstmt.setString(5, "CE");
                    pstmt.setDouble(6, data.getCallOption().getOpenInterest());
                    pstmt.setDouble(7, data.getCallOption().getLastPrice());
                    pstmt.addBatch();
                }
                if (data.getPutOption() != null) {
                    pstmt.setString(1, symbol);
                    pstmt.setLong(2, response.getTimestamp());
                    pstmt.setString(3, data.getExpiryDate());
                    pstmt.setDouble(4, data.getStrikePrice());
                    pstmt.setString(5, "PE");
                    pstmt.setDouble(6, data.getPutOption().getOpenInterest());
                    pstmt.setDouble(7, data.getPutOption().getLastPrice());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
