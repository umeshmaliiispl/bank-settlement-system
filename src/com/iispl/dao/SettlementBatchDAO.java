package com.iispl.dao;

import java.util.List;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementBatch;

public interface SettlementBatchDAO {

<<<<<<< HEAD
	SettlementBatch findByBatchId(String batchId);

	SettlementBatch findTodayBatch();
}
=======
    void save(SettlementBatch batch);

    SettlementBatch findByBatchId(String batchId);

    // ✅ Must return String (batchId) — NOT void
    String createSettlementBatch(List<IncomingTransaction> txns);
    
    public List<SettlementBatch> getSettlementBatch();

	List<SettlementBatch> getAllBatches();
}



//package com.iispl.dao;
//
//import java.util.List;
//
//import com.iispl.entity.IncomingTransaction;
//import com.iispl.entity.SettlementBatch;
//
//public interface SettlementBatchDAO {
//	void save(SettlementBatch batch);
//
//	SettlementBatch findByBatchId(String batchId);
//	public void createSettlementBatchByTime(List<IncomingTransaction>unSettedTransactions);
//}
>>>>>>> ca3d12e (My local changes before pull of Settlement handling)
