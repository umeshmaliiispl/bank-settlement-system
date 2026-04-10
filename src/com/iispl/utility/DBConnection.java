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

            Properties properties = new Properties();

            //  Load from src folder directly
            FileInputStream fileInputStream = new FileInputStream("src/db.properties");
            properties.load(fileInputStream);

            String driver = properties.getProperty("DRIVER_CLASS");
            String url = properties.getProperty("CONNECTION_STRING");
            String username = properties.getProperty("USERNAME");
            String password = properties.getProperty("PASSWORD");

            Class.forName(driver);

            connection = DriverManager.getConnection(url, username, password);

        } catch (Exception e) {
            throw new RuntimeException("Database connection failed", e);
        }

        return connection;
    }
}