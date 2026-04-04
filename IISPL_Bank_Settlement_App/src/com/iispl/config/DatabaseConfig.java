package com.iispl.config;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Database Configuration using HikariCP (Optimized for Neon PostgreSQL)
 */
public final class DatabaseConfig {

	private static final HikariDataSource DATA_SOURCE;

	static {
		try {
			Properties properties = new Properties();

			try (InputStream inputStream = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {

				if (inputStream == null) {
					throw new RuntimeException("db.properties not found in resources folder ");
				}

				properties.load(inputStream);
			}

			String jdbcUrl = require(properties, "CONNECTION_STRING");
			String username = require(properties, "USERNAME");
			String password = require(properties, "PASSWORD");
			String driverClass = require(properties, "DRIVER_CLASS");

			HikariConfig hikariConfig = new HikariConfig();

			hikariConfig.setJdbcUrl(jdbcUrl);
			hikariConfig.setUsername(username);
			hikariConfig.setPassword(password);
			hikariConfig.setDriverClassName(driverClass);

			// NEON OPTIMIZED SETTINGS
			hikariConfig.setMaximumPoolSize(5); // keeping small for Neon
			hikariConfig.setMinimumIdle(1);
			hikariConfig.setConnectionTimeout(30000);
			hikariConfig.setIdleTimeout(300000);
			hikariConfig.setMaxLifetime(1200000);

			// RELIABILITY SETTINGS
			hikariConfig.setKeepaliveTime(30000); // prevents connection reset
			hikariConfig.setValidationTimeout(5000);

			hikariConfig.addDataSourceProperty("sslmode", "require");
			hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
			hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");

			// opptional -- Faster startup
			hikariConfig.setInitializationFailTimeout(0);

			DATA_SOURCE = new HikariDataSource(hikariConfig);

			System.out.println(" Database HikariCP Pool initialized (Neon)");

		} catch (Exception exception) {
			throw new RuntimeException("Failed to initialize DB pool", exception);
		}
	}

//	Get connection from pool
	 
	public static Connection getConnection() throws Exception {
		return DATA_SOURCE.getConnection();
	}


	public static void shutdown() {
		if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
			DATA_SOURCE.close();
			System.out.println("Database Connection pool closed");
		}
	}


	private static String require(Properties properties, String key) {
		String value = properties.getProperty(key);

		if (value == null || value.trim().isEmpty()) {
			throw new RuntimeException("Missing required DB property: " + key);
		}

		return value.trim();
	}
}