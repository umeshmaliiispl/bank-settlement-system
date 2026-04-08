package com.iispl.dao;

import com.iispl.entity.SettlementBatch;

public interface SettlementBatchDAO {
	void save(SettlementBatch batch);

	SettlementBatch findByBatchId(String batchId);

	SettlementBatch findTodayBatch();
}