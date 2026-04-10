package com.iispl.runner;

import java.util.concurrent.TimeUnit;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.utility.QueueManager;

public class SettlementProcessor implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                // poll() with timeout — does NOT block forever like take()
                IncomingTransaction txn = QueueManager.QUEUE.poll(500, TimeUnit.MILLISECONDS);

                if (txn == null) {
                    // No item received in 500ms — check if producers are done
                    if (QueueManager.PRODUCERS_DONE.get() && QueueManager.QUEUE.isEmpty()) {
                        // All producers finished and queue is empty — safe to exit
                        break;
                    }
                    // Producers still running — keep waiting
                    continue;
                }

                // ONLY VALID TRANSACTIONS GO TO SETTLEMENT
                if (txn.getTxnStatus() == TransactionStatus.SUCCESS &&
                    txn.getProcessingStatus() == ProcessingStatus.QUEUED) {
                    processSettlement(txn);
                } else {
                    // SKIPPED TRANSACTIONS (IMPORTANT FOR DEBUG)
                    System.out.printf(
                        "[SKIPPED ][%s] REF=%s | TXN=%s | PROC=%s | REASON=Not eligible for settlement%n",
                        txn.getChannelCode(),
                        txn.getSourceRef(),
                        txn.getTxnStatus(),
                        txn.getProcessingStatus()
                    );
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[SETTLEMENT ERROR] " + e.getMessage());
            }
        }
    }

    private void processSettlement(IncomingTransaction txn) {
        // FINAL SAFETY CHECK (DEFENSIVE PROGRAMMING)
        if (txn.getProcessingStatus() != ProcessingStatus.QUEUED) {
            return;
        }
        System.out.printf(
            "[SETTLED ][%s] REF=%s | AMOUNT=%s %s%n",
            txn.getChannelCode(),
            txn.getSourceRef(),
            txn.getAmount(),
            txn.getCurrency()
        );
    }
}