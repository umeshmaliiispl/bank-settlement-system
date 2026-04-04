package com.iispl.utility;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.iispl.entity.IncomingTransaction;

public class QueueManager {

	public static final BlockingQueue<IncomingTransaction> QUEUE = new LinkedBlockingQueue<>(1000);
}