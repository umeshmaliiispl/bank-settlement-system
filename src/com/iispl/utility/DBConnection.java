package com.iispl.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.FileInputStream;

public class DBConnection {

    private static Connection connection;

    public static Connection getConnection() {

        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            Properties props = new Properties();

            // ✅ Load from src folder directly
            FileInputStream fis = new FileInputStream("src/db.properties");
            props.load(fis);

            String driver = props.getProperty("DRIVER_CLASS");
            String url = props.getProperty("CONNECTION_STRING");
            String username = props.getProperty("USERNAME");
            String password = props.getProperty("PASSWORD");

            Class.forName(driver);

            connection = DriverManager.getConnection(url, username, password);

        } catch (Exception e) {
            throw new RuntimeException("Database connection failed", e);
        }

        return connection;
    }
}