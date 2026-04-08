package com.iispl.dao;

import com.iispl.entity.SettlementRecord;
import java.util.List;

public interface SettlementRecordDAO {
	void save(SettlementRecord record);

	List<SettlementRecord> findByBatchId(String batchId);
}