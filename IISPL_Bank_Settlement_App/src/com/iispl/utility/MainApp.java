package com.iispl.utility;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import com.iispl.config.DatabaseConfig;

public class MainApp {

    public static void main(String[] args) {

        System.out.println("🔄 Checking Database Connection...");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customer")) {

            System.out.println("✅ SUCCESS: Database Connected!\n");

            System.out.println("📊 Customer Data:\n");

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Name: " + rs.getString("first_name") + " " + rs.getString("last_name"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("KYC: " + rs.getString("kyc_status"));
                System.out.println("Tier: " + rs.getString("customer_tier"));
                System.out.println("-----------------------------");
            }

        } catch (Exception e) {
            System.out.println("❌ ERROR: Unable to fetch data");
            e.printStackTrace();
        }
    }
}