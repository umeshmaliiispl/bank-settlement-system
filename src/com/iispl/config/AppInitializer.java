package com.iispl.config;

import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;

public class AppInitializer {

    public static void init() {
        System.out.println("========== INITIALIZING DATABASE ==========");

        try {
            // Trigger DB connection 
            TransactionDao transactionDao = new TransactionDaoImpl();
            transactionDao.findAll(); // lightweight warm-up

            System.out.println("Database initialized successfully");

        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
}