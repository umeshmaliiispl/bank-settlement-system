package com.iispl.utility;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.iispl.entity.IncomingTransaction;

public class QueueManager {

    public static final BlockingQueue<IncomingTransaction> QUEUE = new LinkedBlockingQueue<>(1000);

    // Set to true when all producers are done — signals consumers to stop
    public static final AtomicBoolean PRODUCERS_DONE = new AtomicBoolean(false);
}