package com.iispl.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.IncomingTransaction;

public class TransactionServiceImpl implements TransactionService {

	private final TransactionDao transactionDao = new TransactionDaoImpl();
	private final ValidationService validationService = new ValidationService();

	@Override
	public void save(IncomingTransaction txn) {

		// 1. Validate
		validationService.validate(txn);

		// 2. F save
		transactionDao.save(txn);

		// 3. Logging
		System.out.printf("[DB      ][%-18s][%-7s] REF=%-22s | STATUS=%-8s/%-10s%n", Thread.currentThread().getName(),
				safe(txn.getChannelCode()), safe(txn.getSourceRef()), safe(txn.getTxnStatus()),
				safe(txn.getProcessingStatus()));
	}

	@Override
	public void printAllTransactions() {

		List<IncomingTransaction> list = transactionDao.findAll();

		if (list.isEmpty()) {
			System.out.println("\n No transactions available.");
			return;
		}

		int total = list.size();
		int successQueued = 0;
		int failed = 0;
		int flagged = 0;

		List<String> settlementReady = new ArrayList<>();

		for (IncomingTransaction txn : list) {

			String src = safe(txn.getTxnStatus());
			String proc = safe(txn.getProcessingStatus());

			if ("SUCCESS".equals(src) && "QUEUED".equals(proc)) {
				successQueued++;
				settlementReady.add(txn.getSourceRef());
			} else if ("FAILED".equals(proc)) {
				failed++;
			} else if ("FLAGGED".equals(proc)) {
				flagged++;
			}
		}

		System.out.println(
				"\n====================================================================================================================");
		System.out.println("                           BANK SETTLEMENT - INCOMING TRANSACTIONS REPORT");
		System.out.println(
				"====================================================================================================================");

		System.out.println("\nPIPELINE STATUS");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------");
		System.out.println("Processing Mode      : Multi-Threaded (Producer-Consumer)");
		System.out.println("Queue Enabled        : YES");
		System.out.println("Database             : PostgreSQL (HikariCP)");

		// =====================================================================================
		// SUMMARY
		// =====================================================================================
		System.out.println(
				"\n--------------------------------------------------------------------------------------------------------------------");
		System.out.println("TRANSACTION SUMMARY");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------");
		System.out.printf("Total Transactions     : %d\n", total);
		System.out.printf("Successful (Queued)  : %d\n", successQueued);
		System.out.printf("Flagged             : %d\n", flagged);
		System.out.printf("Failed              : %d\n", failed);

		System.out.println(
				"\n====================================================================================================================================================================");
		System.out.println("                                              DETAILED TRANSACTION VIEW");
		System.out.println(
				"====================================================================================================================================================================");

		System.out.printf("%-4s %-22s %-8s %-8s %-22s %-22s %-14s %-6s %-14s %-14s %-35s%n", "ID", "REF ID", "CHANNEL",
				"TYPE", "SENDER BANK", "RECEIVER BANK", "AMOUNT", "CUR", "SRC STATUS", "PROC STATUS", "REMARK");

		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------------------------------------------");
		for (IncomingTransaction txn : list) {

			System.out.printf("%-4d %-22s %-8s %-8s %-22s %-22s %-14s %-6s %-14s %-14s %-35s%n", txn.getIncomingTxnId(),
					safe(txn.getSourceRef()), safe(txn.getChannelCode()), safe(txn.getTxnType()),
					trimBankName(txn.getSenderBankName()), trimBankName(txn.getReceiverBankName()),
					formatAmount(txn.getAmount()), safe(txn.getCurrency()), safe(txn.getTxnStatus()),
					safe(txn.getProcessingStatus()), formatRemark(txn.getErrorMessage()));

		}

		System.out.println(
				"\n====================================================================================================================");
		System.out.println("SETTLEMENT ELIGIBILITY");
		System.out.println(
				"====================================================================================================================");

		System.out.println("\n READY FOR SETTLEMENT (SUCCESS + QUEUED):");

		if (settlementReady.isEmpty()) {
			System.out.println("   None");
		} else {
			for (String ref : settlementReady) {
				System.out.println("   - " + ref);
			}
		}

		System.out.println("\n NOT ELIGIBLE:");
		System.out.println("   - All FAILED / FLAGGED / INVALID transactions");

		System.out.println(
				"\n====================================================================================================================\n");
	}

	private String trimBankName(String name) {
		if (name == null)
			return "N/A";

		if (name.length() > 20) {
			return name.substring(0, 19) + ".";
		}
		return name;
	}

	private String formatAmount(BigDecimal amt) {
		if (amt == null)
			return "0.00";
		return String.format("%,.2f", amt);
	}

	private String formatRemark(String error) {
		if (error == null || error.isEmpty())
			return "-";

		String clean = error.replaceAll("\\[.*?\\]", "").trim();

		if (clean.contains(":")) {
			clean = clean.substring(0, clean.indexOf(":"));
		}

		return clean;
	}

	private String safe(Object val) {
		return val == null ? "N/A" : val.toString();
	}
}