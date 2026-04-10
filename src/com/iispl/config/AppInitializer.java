
package com.iispl.config;

import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;

public class AppInitializer {

	public static void init() {
		System.out.println("========== INITIALIZING DATABASE ==========");

		boolean success = false;

		try {
			TransactionDao transactionDao = new TransactionDaoImpl();
//			transactionDao.checkConnection(); // ✅ lightweight check  //But its taking Time
			transactionDao.findAll();
			success = true;

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		if (success) {
			System.out.println("Database initialized successfully");
		} else {
			System.out.println("Database initialization failed");
		}

	}
}
