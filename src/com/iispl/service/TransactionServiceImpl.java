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