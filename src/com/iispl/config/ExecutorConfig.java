package com.iispl.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorConfig {

    // Remove static final pools — they can't be reused after shutdown

    // Call this to get a fresh producer pool every time
    public static ExecutorService newProducerPool() {
        return Executors.newFixedThreadPool(10);
    }

    // Call this to get a fresh consumer pool every time
    public static ExecutorService newConsumerPool() {
        return Executors.newFixedThreadPool(5);
    }
}