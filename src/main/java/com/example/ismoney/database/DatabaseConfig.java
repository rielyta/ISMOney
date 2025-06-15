package com.example.ismoney.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String DB_URL = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres";
    private static final String DB_USERNAME = "postgres.toswlsbzcznzwlsxumpo";
    private static final String DB_PASSWORD = "ismoneypbolab";

    private static DatabaseConfig instance;

    private DatabaseConfig() {}

    public static DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");

            Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Database connection successful!");
            return connection;

        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found", e);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw e;
        }
    }

    public void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(" Database connection test successful!");
            }
        } catch (SQLException e) {
            System.err.println(" Database connection test failed: " + e.getMessage());
        }
    }
}