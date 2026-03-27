package com.iispl.config;


import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static String DRIVER;
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    // Static block to load properties once
    static {
        try {
            Properties props = new Properties();

            InputStream input = DatabaseConfig.class
                    .getClassLoader()
                    .getResourceAsStream("db.properties");

            if (input == null) {
                throw new RuntimeException("❌ db.properties file not found in resources folder!");
            }

            props.load(input);

            DRIVER = props.getProperty("DRIVER_CLASS");
            URL = props.getProperty("CONNECTION_STRING");
            USER = props.getProperty("USERNAME");
            PASSWORD = props.getProperty("PASSWORD");

            if (URL == null || USER == null || PASSWORD == null) {
                throw new RuntimeException("❌ Missing DB configuration values!");
            }

            Class.forName(DRIVER);

            System.out.println("✅ [DB] Config loaded successfully.");

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load DB config", e);
        }
    }

    // Create new connection (thread-safe)
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

        System.out.println("🔗 [DB] Connected by " + Thread.currentThread().getName());

        return conn;
    }

    // Test method
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("🎉 SUCCESS: Connected to -> " + conn.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("❌ FAILED: " + e.getMessage());
        }
    }
}