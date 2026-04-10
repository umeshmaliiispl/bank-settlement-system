package com.iispl.service;

import java.util.List;
import com.iispl.entity.SettlementBatch;

public interface SettlementService {

    // ─── Instance methods ─────────────────────────────────────────────────────
    void settle(NettingEngine engine);

    void printBatchSummary(List<SettlementBatch> batches);

    void printSingleBatchSummary(SettlementBatch batch);

    void printBatchListWithIndex(List<SettlementBatch> batches);

    List<SettlementBatch> getAllBatchesWithRecords();

    SettlementBatch getBatchWithRecords(String batchId);

    // ─── Static methods (delegates to SettlementServiceImpl) ─────────────────
    static void createSettlementBatch() {
        SettlementServiceImpl.createSettlementBatch();
    }

    static void getAllSettlementBatches() {
        SettlementServiceImpl.getAllSettlementBatches();
    }

    static void sendBatchToNpc(List<SettlementBatch> batches) {
        SettlementServiceImpl.sendBatchToNpc(batches);
    }

    static void saveXmlToFile(String batchId, String xml) {
        SettlementServiceImpl.saveXmlToFile(batchId, xml);
    }

    static String generateNpcXml(SettlementBatch batch) {
        return SettlementServiceImpl.generateNpcXml(batch);
    }

    static void viewXmlByIndex() {
        SettlementServiceImpl.viewXmlByIndex();
    }

    static void viewXmlFile(String batchId) {
        SettlementServiceImpl.viewXmlFile(batchId);
    }
}

