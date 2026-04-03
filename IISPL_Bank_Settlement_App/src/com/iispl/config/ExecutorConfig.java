package com.iispl.config;

import java.util.concurrent.*;

public class ExecutorConfig {

	public static final ExecutorService PRODUCER_POOL = Executors.newFixedThreadPool(5);

	public static final ExecutorService CONSUMER_POOL = new ThreadPoolExecutor(
			5, // core threads
			20, // max threads
			60, // idle timeout
			TimeUnit.SECONDS, new LinkedBlockingQueue<>());
}