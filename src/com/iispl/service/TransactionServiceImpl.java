package com.iispl.service;

import java.math.BigDecimal;
import java.util.List;

import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;

public class TransactionServiceImpl implements TransactionService {

	private final TransactionDao transactionDao = new TransactionDaoImpl();
	private final ValidationService validationService = new ValidationService();

	@Override
	public void save(IncomingTransaction incoming) {

		// 1. RECEIVED
		IncomingTransaction txn = incoming.toBuilder().processingStatus(ProcessingStatus.RECEIVED).build();

		// 2. VALIDATION
		txn = validationService.validate(txn);

		if (txn.getProcessingStatus() == ProcessingStatus.FAILED) {
			insert(txn, "FAILED - VALIDATION");
			return;
		}

		// 3. SAME BANK CHECK
		if (isSameBank(txn)) {
			txn = txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
					.errorMessage("Same bank transaction not allowed").build();

			insert(txn, "FAILED - SAME BANK");
			return;
		}

		// 4. SOURCE STATUS CHECK
		if (txn.getTxnStatus().name().equals("FAILED")) {

			txn = txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
					.errorMessage("Source transaction failed at origin").build();

			insert(txn, "FAILED - SOURCE");
			return;
		}

		// 5. SUCCESS → QUEUED
		txn = txn.toBuilder().processingStatus(ProcessingStatus.QUEUED).build();

		insert(txn, "QUEUED");
	}

	// RULES
	private IncomingTransaction applySameBankRule(IncomingTransaction txn) {

		if (isSameBank(txn)) {
			return txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
					.errorMessage("Same bank transaction not allowed for settlement").build();
		}

		return txn;
	}

	private IncomingTransaction applyTxnStatusRule(IncomingTransaction txn) {

		if (txn.getTxnStatus() == TransactionStatus.FAILED) {
			return txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
					.errorMessage("Source transaction failed at origin").build();
		}

		if (txn.getTxnStatus() != TransactionStatus.SUCCESS) {
			return txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
					.errorMessage("Pending / invalid transaction status").build();
		}

		return txn;
	}

	// INSERT
	private void insert(IncomingTransaction txn, String stage) {

		boolean inserted = transactionDao.save(txn);

		if (!inserted) {
			System.out.printf("[DUPLICATE][%-18s][%-7s] REF=%-22s | SKIPPED%n", Thread.currentThread().getName(),
					safe(txn.getChannelCode()), safe(txn.getSourceRef()));
			return;
		}

		log(txn, stage);
	}

	// LOGGING
	private void log(IncomingTransaction txn, String stage) {
		System.out.printf("[SERVICE ][%-18s][%-7s] REF=%-22s | AMT=%12s %-3s | TXN=%-8s | PROC=%-10s | REMARK: %s%n",
				Thread.currentThread().getName(), safe(txn.getChannelCode()), safe(txn.getSourceRef()),
				formatAmount(txn.getAmount()), safe(txn.getCurrency()), safe(txn.getTxnStatus()),
				safe(txn.getProcessingStatus()), buildRemark(stage, txn.getErrorMessage()));
	}


	// REPORT (FULL TABLE)
	@Override
	public void printAllTransactions() {

		List<IncomingTransaction> list = transactionDao.findAll();

		if (list.isEmpty()) {
			System.out.println("\nNo transactions available.");
			return;
		}

		System.out.println(
				"\n========================================================================================================================================================");
		System.out.println("                                      INCOMING TRANSACTIONS - FULL REPORT");
		System.out.println(
				"========================================================================================================================================================");

		System.out.printf("%-4s %-20s %-8s %-8s %-20s %-20s %-12s %-6s %-12s %-14s %-40s%n", "ID", "REF ID", "CHNL",
				"TYPE", "SENDER BANK", "RECEIVER BANK", "AMOUNT", "CUR", "TXN STATUS", "PROC STATUS", "REMARK");

		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------------------------------");

		int queued = 0;
		int failed = 0;
		int flagged = 0;

		for (IncomingTransaction txn : list) {

			String proc = safe(txn.getProcessingStatus());

			if ("QUEUED".equals(proc))
				queued++;
			else if ("FAILED".equals(proc))
				failed++;
			else if ("FLAGGED".equals(proc))
				flagged++;

			System.out.printf("%-4d %-20s %-8s %-8s %-20s %-20s %-12s %-6s %-12s %-14s %-40s%n", txn.getIncomingTxnId(),
					safe(txn.getSourceRef()), safe(txn.getChannelCode()), safe(txn.getTxnType()),
					trim(txn.getSenderBankName()), trim(txn.getReceiverBankName()), formatAmount(txn.getAmount()),
					safe(txn.getCurrency()), safe(txn.getTxnStatus()), proc, formatRemark(txn.getErrorMessage()));
		}

		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------------------------------");

		System.out.println("\nSUMMARY");
		System.out.println("--------------------------------------------------");
		System.out.println("Total Transactions : " + list.size());
		System.out.println("Queued (Ready)     : " + queued);
		System.out.println("Failed             : " + failed);
		System.out.println("Flagged            : " + flagged);

		System.out.println(
				"========================================================================================================================================================\n");
	}


	// UTILS
	private boolean isSameBank(IncomingTransaction txn) {
		String s = txn.getSenderBankName();
		String r = txn.getReceiverBankName();
		return s != null && s.equalsIgnoreCase(r);
	}

	private String buildRemark(String stage, String error) {
		if (error == null || error.isEmpty())
			return stage;
		return stage + " | " + error;
	}

	private String trim(String val) {
		if (val == null)
			return "N/A";
		return val.length() > 18 ? val.substring(0, 17) + "." : val;
	}

	private String formatRemark(String error) {
		if (error == null || error.isEmpty())
			return "-";
		String clean = error.replaceAll("\\[.*?]", "").trim();
		if (clean.contains(":"))
			clean = clean.substring(0, clean.indexOf(":"));
		return clean;
	}

	private static String formatAmount(BigDecimal amount) {
		return amount == null ? "0.00" : String.format("%,.2f", amount);
	}

	 /* @param value any object whose string representation is needed; may be {@code null}
	 * @return {@code value.toString()} if non-null, otherwise the placeholder {@code "N/A"}
	 */
	private static String safe(Object value) {
	    return value == null ? "N/A" : value.toString();
	}
}


// package com.iispl.service;

// import java.math.BigDecimal;
// import java.util.List;

// import com.iispl.dao.TransactionDao;
// import com.iispl.dao.TransactionDaoImpl;
// import com.iispl.entity.IncomingTransaction;
// import com.iispl.enums.ProcessingStatus;
// import com.iispl.enums.TransactionStatus;

// public class TransactionServiceImpl implements TransactionService {

// 	private final TransactionDao transactionDao = new TransactionDaoImpl();
// 	private final ValidationService validationService = new ValidationService();

// 	@Override
// 	public void save(IncomingTransaction incoming) {

// 		// 1. RECEIVED
// 		IncomingTransaction txn = incoming.toBuilder().processingStatus(ProcessingStatus.RECEIVED).build();

// 		// 2. VALIDATION
// 		txn = validationService.validate(txn);

// 		if (txn.getProcessingStatus() == ProcessingStatus.FAILED) {
// 			insert(txn, "FAILED - VALIDATION");
// 			return;
// 		}

// 		// 3. SAME BANK CHECK
// 		if (isSameBank(txn)) {
// 			txn = txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
// 					.errorMessage("Same bank transaction not allowed").build();

// 			insert(txn, "FAILED - SAME BANK");
// 			return;
// 		}

// 		// 4. SOURCE STATUS CHECK
// 		if (txn.getTxnStatus().name().equals("FAILED")) {

// 			txn = txn.toBuilder().processingStatus(ProcessingStatus.FAILED)
// 					.errorMessage("Source transaction failed at origin").build();

// 			insert(txn, "FAILED - SOURCE");
// 			return;
// 		}

// 		// 5. SUCCESS → QUEUED
// 		txn = txn.toBuilder().processingStatus(ProcessingStatus.QUEUED).build();

// 		insert(txn, "QUEUED");
// 	}
	
	
// 	@Override
// 	public void printAllTransactions() {

// 	    List<IncomingTransaction> list = transactionDao.findAll();

// 	    if (list.isEmpty()) {
// 	        System.out.println("\n No transactions available.");
// 	        return;
// 	    }

// 	    System.out.println("\n==============================================================================================================================================================================================");
// 	    System.out.println("                                                        FULL INCOMING TRANSACTION TABLE");
// 	    System.out.println("==============================================================================================================================================================================================");

// 	    System.out.printf(
// 	    "%-5s %-25s %-8s %-10s %-10s %-14s %-14s %-10s %-8s %-22s %-12s %-16s %-16s %-16s %-22s %-22s %-14s %-14s %-14s %-14s %-8s %-8s %-20s %-30s %-30s %-22s %-22s %-15s %-8s%n",
// 	    "ID",
// 	    "SOURCE_REF",
// 	    "SRC_ID",
// 	    "CHANNEL",
// 	    "TXN_TYPE",
// 	    "AMOUNT",
// 	    "GROSS_AMOUNT",
// 	    "FEE_AMOUNT",
// 	    "CURRENCY",
// 	    "VALUE_DATE",
// 	    "TXN_STATUS",
// 	    "PROCESS_STATUS",
// 	    "SENDER_IFSC",
// 	    "RECEIVER_IFSC",
// 	    "SENDER_BANK",
// 	    "RECEIVER_BANK",
// 	    "SENDER_BIC",
// 	    "RECEIVER_BIC",
// 	    "PARTNER_NAME",
// 	    "MERCHANT_ID",
// 	    "PRIORITY",
// 	    "RETRY",
// 	    "CHECKSUM",
// 	    "RAW_PAYLOAD",
// 	    "NORMALIZED_PAYLOAD",
// 	    "CREATED_AT",
// 	    "UPDATED_AT",
// 	    "CREATED_BY",
// 	    "VERSION"
// 	    );

// 	    System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

// 	    for (IncomingTransaction txn : list) {

// 	        System.out.printf(
// 	        "%-5d %-25s %-8d %-10s %-10s %-14.2f %-14.2f %-10.2f %-8s %-22s %-12s %-16s %-16s %-16s %-22s %-22s %-14s %-14s %-14s %-14s %-8d %-8d %-20s %-30s %-30s %-22s %-22s %-15s %-8d%n",

// 	        txn.getIncomingTxnId(),
// 	        fit(txn.getSourceRef(),25),
// 	        txn.getSourceSystemId(),
// 	        fit(txn.getChannelCode(),10),
// 	        fit(txn.getTxnType(),10),

// 	        val(txn.getAmount()),
// 	        val(txn.getGrossAmount()),
// 	        val(txn.getFeeAmount()),

// 	        fit(txn.getCurrency(),8),
// 	        fit(str(txn.getValueDate()),22),

// 	        fit(txn.getTxnStatus(),12),
// 	        fit(txn.getProcessingStatus(),16),

// 	        fit(txn.getSenderIfsc(),16),
// 	        fit(txn.getReceiverIfsc(),16),

// 	        fit(txn.getSenderBankName(),22),
// 	        fit(txn.getReceiverBankName(),22),

// 	        fit(txn.getSenderBic(),14),
// 	        fit(txn.getReceiverBic(),14),

// 	        fit(txn.getPartnerName(),14),
// 	        fit(txn.getMerchantId(),14),

// 	        txn.getPriority(),
// 	        txn.getRetryCount(),

// 	        fit(txn.getChecksum(),20),
// 	        fit(txn.getRawPayload(),30),
// 	        fit(txn.getNormalizedPayload(),30),

// 	        fit(str(txn.getCreatedAt()),22),
// 	        fit(str(txn.getUpdatedAt()),22),

// 	        fit(txn.getCreatedBy(),15),
// 	        txn.getVersion()
// 	        );
// 	    }

// 	    System.out.println("\n==============================================================================================================================================================================================\n");
// 	}
	
// 	private String fit(Object val, int max) {
// 	    if (val == null) return "-";
// 	    String s = val.toString();
// 	    return s.length() > max ? s.substring(0, max - 3) + "..." : s;
// 	}

// 	private double val(java.math.BigDecimal num) {
// 	    return num != null ? num.doubleValue() : 0.0;
// 	}

// 	private String str(Object val) {
// 	    return val == null ? "-" : val.toString();
// 	}
	
// //	@Override
// //	public void printAllTransactions() {
// //
// //	    List<IncomingTransaction> list = transactionDao.findAll();
// //
// //	    if (list.isEmpty()) {
// //	        System.out.println("\n No transactions available.");
// //	        return;
// //	    }
// //
// //	    System.out.println("\n================= FULL INCOMING_TRANSACTION TABLE =================");
// //
// //	    for (IncomingTransaction txn : list) {
// //
// //	        System.out.println("--------------------------------------------------------------------------------------");
// //
// //	        System.out.println("ID                : " + txn.getIncomingTxnId());
// //	        System.out.println("Source System ID  : " + txn.getSourceSystemId());
// //	        System.out.println("Source Ref        : " + txn.getSourceRef());
// //
// //	        System.out.println("Raw Payload       : " + txn.getRawPayload());
// //	        System.out.println("Normalized Payload: " + txn.getNormalizedPayload());
// //
// //	        System.out.println("Channel Code      : " + txn.getChannelCode());
// //
// //	        System.out.println("Txn Type          : " + txn.getTxnType());
// //	        System.out.println("Amount            : " + formatAmount(txn.getAmount()));
// //	        System.out.println("Gross Amount      : " + formatAmount(txn.getGrossAmount()));
// //	        System.out.println("Fee Amount        : " + formatAmount(txn.getFeeAmount()));
// //	        System.out.println("Currency          : " + txn.getCurrency());
// //
// //	        System.out.println("Value Date        : " + txn.getValueDate());
// //
// //	        System.out.println("Txn Status        : " + txn.getTxnStatus());
// //	        System.out.println("Processing Status : " + txn.getProcessingStatus());
// //
// //	        System.out.println("Sender IFSC       : " + txn.getSenderIfsc());
// //	        System.out.println("Receiver IFSC     : " + txn.getReceiverIfsc());
// //
// //	        System.out.println("Sender Bank       : " + txn.getSenderBankName());
// //	        System.out.println("Receiver Bank     : " + txn.getReceiverBankName());
// //
// //	        System.out.println("Sender BIC        : " + txn.getSenderBic());
// //	        System.out.println("Receiver BIC      : " + txn.getReceiverBic());
// //
// //	        System.out.println("Partner Name      : " + txn.getPartnerName());
// //	        System.out.println("Merchant ID       : " + txn.getMerchantId());
// //
// //	        System.out.println("Checksum          : " + txn.getChecksum());
// //
// //	        System.out.println("Priority          : " + txn.getPriority());
// //	        System.out.println("Retry Count       : " + txn.getRetryCount());
// //
// //	        System.out.println("Error Message     : " + txn.getErrorMessage());
// //
// //	        System.out.println("Created At        : " + txn.getCreatedAt());
// //	        System.out.println("Updated At        : " + txn.getUpdatedAt());
// //	        System.out.println("Created By        : " + txn.getCreatedBy());
// //	        System.out.println("Version           : " + txn.getVersion());
// //
// //	        System.out.println("--------------------------------------------------------------------------------------");
// //	    }
// //
// //	    System.out.println("\n================= END OF TABLE =================\n");
// //	}

// //	@Override
// //	public void printAllTransactions() {
// //
// //		List<IncomingTransaction> list = transactionDao.findAll();
// //
// //		if (list.isEmpty()) {
// //			System.out.println("\n No transactions available.");
// //			return;
// //		}
// //
// //		int total = list.size();
// //		int successQueued = 0;
// //		int failed = 0;
// //		int flagged = 0;
// //
// //		List<String> settlementReady = new ArrayList<>();
// //
// //		for (IncomingTransaction txn : list) {
// //
// //			String src = safe(txn.getTxnStatus());
// //			String proc = safe(txn.getProcessingStatus());
// //
// //			if ("SUCCESS".equals(src) && "QUEUED".equals(proc)) {
// //				successQueued++;
// //				settlementReady.add(txn.getSourceRef());
// //			} else if ("FAILED".equals(proc)) {
// //				failed++;
// //			} else if ("FLAGGED".equals(proc)) {
// //				flagged++;
// //			}
// //		}
// //
// //		System.out.println(
// //				"\n====================================================================================================================");
// //		System.out.println("                           BANK SETTLEMENT - INCOMING TRANSACTIONS REPORT");
// //		System.out.println(
// //				"====================================================================================================================");
// //
// //		System.out.println("\nPIPELINE STATUS");
// //		System.out.println(
// //				"--------------------------------------------------------------------------------------------------------------------");
// //		System.out.println("Processing Mode      : Multi-Threaded (Producer-Consumer)");
// //		System.out.println("Queue Enabled        : YES");
// //		System.out.println("Database             : PostgreSQL (HikariCP)");
// //
// //		// =====================================================================================
// //		// SUMMARY
// //		// =====================================================================================
// //		System.out.println(
// //				"\n--------------------------------------------------------------------------------------------------------------------");
// //		System.out.println("TRANSACTION SUMMARY");
// //		System.out.println(
// //				"--------------------------------------------------------------------------------------------------------------------");
// //		System.out.printf("Total Transactions     : %d\n", total);
// //		System.out.printf("Successful (Queued)  : %d\n", successQueued);
// //		System.out.printf("Flagged             : %d\n", flagged);
// //		System.out.printf("Failed              : %d\n", failed);
// //
// //		System.out.println(
// //				"\n====================================================================================================================================================================");
// //		System.out.println("                                              DETAILED TRANSACTION VIEW");
// //		System.out.println(
// //				"====================================================================================================================================================================");
// //
// //		System.out.printf("%-4s %-22s %-8s %-8s %-22s %-22s %-14s %-6s %-14s %-14s %-35s%n", "ID", "REF ID", "CHANNEL",
// //				"TYPE", "SENDER BANK", "RECEIVER BANK", "AMOUNT", "CUR", "SRC STATUS", "PROC STATUS", "REMARK");
// //
// //		System.out.println(
// //				"--------------------------------------------------------------------------------------------------------------------------------------------------------------------");
// //		for (IncomingTransaction txn : list) {
// //
// //			System.out.printf("%-4d %-22s %-8s %-8s %-22s %-22s %-14s %-6s %-14s %-14s %-35s%n", txn.getIncomingTxnId(),
// //					safe(txn.getSourceRef()), safe(txn.getChannelCode()), safe(txn.getTxnType()),
// //					trimBankName(txn.getSenderBankName()), trimBankName(txn.getReceiverBankName()),
// //					formatAmount(txn.getAmount()), safe(txn.getCurrency()), safe(txn.getTxnStatus()),
// //					safe(txn.getProcessingStatus()), formatRemark(txn.getErrorMessage()));
// //
// //		}
// //
// //		System.out.println(
// //				"\n====================================================================================================================");
// //		System.out.println("SETTLEMENT ELIGIBILITY");
// //		System.out.println(
// //				"====================================================================================================================");
// //
// //		System.out.println("\n READY FOR SETTLEMENT (SUCCESS + QUEUED):");
// //
// //		if (settlementReady.isEmpty()) {
// //			System.out.println("   None");
// //		} else {
// //			for (String ref : settlementReady) {
// //				System.out.println("   - " + ref);
// //			}
// //		}
// //
// //		System.out.println("\n NOT ELIGIBLE:");
// //		System.out.println("   - All FAILED / FLAGGED / INVALID transactions");
// //
// //		System.out.println(
// //				"\n====================================================================================================================\n");
// //	}


// 	// UTILS
// 	private boolean isSameBank(IncomingTransaction txn) {
// 		String s = txn.getSenderBankName();
// 		String r = txn.getReceiverBankName();
// 		return s != null && s.equalsIgnoreCase(r);
// 	}

// 	private String buildRemark(String stage, String error) {
// 		if (error == null || error.isEmpty())
// 			return stage;
// 		return stage + " | " + error;
// 	}

// 	private String trim(String val) {
// 		if (val == null)
// 			return "N/A";
// 		return val.length() > 18 ? val.substring(0, 17) + "." : val;
// 	}

// 	private String formatRemark(String error) {
// 		if (error == null || error.isEmpty())
// 			return "-";
// 		String clean = error.replaceAll("\\[.*?]", "").trim();
// 		if (clean.contains(":"))
// 			clean = clean.substring(0, clean.indexOf(":"));
// 		return clean;
// 	}

// 	private static String formatAmount(BigDecimal amount) {
// 		return amount == null ? "0.00" : String.format("%,.2f", amount);
// 	}

// 	 /* @param value any object whose string representation is needed; may be {@code null}
// 	 * @return {@code value.toString()} if non-null, otherwise the placeholder {@code "N/A"}
// 	 */
// 	private static String safe(Object value) {
// 	    return value == null ? "N/A" : value.toString();
// 	}
// }
