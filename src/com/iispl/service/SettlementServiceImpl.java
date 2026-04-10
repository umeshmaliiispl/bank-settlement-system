
package com.iispl.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iispl.dao.NettingPositionDAO;
import com.iispl.dao.NettingPositionDAOImpl;
import com.iispl.dao.SettlementBatchDAO;
import com.iispl.dao.SettlementBatchDAOImpl;
import com.iispl.dao.SettlementInstructionDAO;
import com.iispl.dao.SettlementInstructionDAOImpl;
import com.iispl.dao.SettlementRecordDAO;
import com.iispl.dao.SettlementRecordDAOImpl;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.SettlementInstruction;
import com.iispl.entity.SettlementRecord;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;
import com.iispl.enums.InstructionStatus;

public class SettlementServiceImpl implements SettlementService { 
    private final SettlementInstructionDAO instructionDAO = new SettlementInstructionDAOImpl();
    private final NettingPositionDAO nettingDAO = new NettingPositionDAOImpl();

    
    public void settle(NettingEngine engine) {

        String batchId = generateBatchId();

        int totalTxn = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        System.out.println("\n==============================================================");
        System.out.println("                 SETTLEMENT INSTRUCTIONS REPORT");
        System.out.println("==============================================================");

        System.out.printf(
                "%-40s %-30s %-30s %-12s %-10s %-12s\n",
                "Instruction ID",
                "From Bank",
                "To Bank",
                "Amount",
                "Channel",
                "Value Date"
        );

        System.out.println("--------------------------------------------------------------");

        //  Convert map values → List
        List<NettingPosition> positions =
                new java.util.ArrayList<>(engine.getPositions().values());

        //  SAVE ALL NETTING AT ONCE
        nettingDAO.saveAll(positions);

        for (NettingPosition pos : positions) {

            SettlementInstruction ins = new SettlementInstruction();

            ins.setInstructionId("INS-" + UUID.randomUUID());

            // ✅ FIX: correct method
            String bankName = pos.getSenderBank();

            String fromBranch = generateBranch(bankName);
            String toBranch = generateBranch("IISPL Bank");

            ins.setFromBank(bankName + " (" + fromBranch + ")");
            ins.setToBank("IISPL Bank (" + toBranch + ")");

            ins.setAmount(Math.abs(pos.getNetAmount().doubleValue()));

            ins.setChannel(ChannelType.RTGS);
            ins.setInstructionStatus(InstructionStatus.INITIATED);
            ins.setValueDate(LocalDate.now());

            instructionDAO.save(ins);

            System.out.printf(
                    "%-40s %-30s %-30s %-12.2f %-10s %-12s\n",
                    ins.getInstructionId(),
                    ins.getFromBank(),
                    ins.getToBank(),
                    ins.getAmount(),
                    ins.getChannel(),
                    ins.getValueDate()
            );

            totalTxn++;
            totalAmount = totalAmount.add(pos.getNetAmount().abs());
        }

        System.out.println("==============================================================");
        System.out.println("Batch ID   : " + batchId);
        System.out.println("Total Txn  : " + totalTxn);
        System.out.println("Total Amt  : " + totalAmount);
        System.out.println("==============================================================");

        System.out.println("✅ Settlement Completed: " + batchId);
    }


    // ─────────────────────────────────────────────
    // BATCH ID GENERATOR
    // ─────────────────────────────────────────────
    private String generateBatchId() {
        return "BATCH-" +
                LocalDate.now().toString().replace("-", "") +
                "-" +
                UUID.randomUUID().toString().substring(0, 6);
    }

    // ─────────────────────────────────────────────
    // BRANCH GENERATOR
    // ─────────────────────────────────────────────
    private String generateBranch(String bankName) {

        String code = bankName.replaceAll("[^A-Za-z]", "")
                .substring(0, Math.min(4, bankName.length()))
                .toUpperCase();

        int branchNumber = (int) (Math.random() * 900) + 100;

        return code + "-BR-" + branchNumber;
    }
    
     
    public static void createSettlementBatch()
    {
    	 TransactionDaoImpl transactionDaoImpl=new TransactionDaoImpl();
    	 List<IncomingTransaction> unSettledTransactions=transactionDaoImpl.getUnsettledTranasactions();
    	 SettlementBatchDAO settlementBatchDao=new SettlementBatchDAOImpl();
    	 settlementBatchDao.createSettlementBatch(unSettledTransactions);
    }
    
    public static void getAllSettlementBatches()
    {
    	SettlementBatchDAO settlementBatchDao=new SettlementBatchDAOImpl();
    	settlementBatchDao.getSettlementBatch();
    }
    
    public void printBatchSummary(List<SettlementBatch> batches) {

        if (batches == null || batches.isEmpty()) {
            System.out.println("No settlement batches available.");
            return;
        }

        System.out.println("\n===========================================================================================");
        System.out.println("                                BATCH SUMMARY REPORT                              ");
        System.out.println("===========================================================================================");

        
        System.out.printf(
        	    "%-35s %-20s %-20s %-15s%n",
        	    "Batch ID", "Total Transactions", "Total Amount", "Batch Status"
        	);

        System.out.println("-------------------------------------------------------------------------------------------");

        for (SettlementBatch batch : batches) {

            System.out.printf(
                "%-35s %-20d %-20.2f %-15s%n",
                batch.getBatchId(),
                batch.getTotalTransactions(),
                batch.getTotalAmount(),
                batch.getBatchStatus()
            );
        }

        System.out.println("=============================================================================================");
    }
    
    public void printSingleBatchSummary(SettlementBatch batch) {

        if (batch == null) {
            System.out.println("No batch found.");
            return;
        }

        // ================= BATCH SUMMARY =================
        System.out.println("\n=================================================================================================");
        System.out.println("                    SINGLE BATCH SUMMARY REPORT                           ");
        System.out.println("===================================================================================================");

        System.out.printf(
        	    "%-35s %-20s %-20s %-15s%n",
        	    "Batch ID", "Total Transactions", "Total Amount", "Batch Status"
        	);

        System.out.println("----------------------------------------------------------------------------------------------------");

        System.out.printf(
                "%-35s %-20d %-20.2f %-15s%n",
                batch.getBatchId(),
                batch.getTotalTransactions(),
                batch.getTotalAmount(),
                batch.getBatchStatus()
            );

        System.out.println("========================================================================================================");


        // ================= TRANSACTIONS =================
        System.out.println("\n======================================================================================================================================================================================================================================================================================");
        System.out.println("                                      TRANSACTIONS IN BATCH");
        System.out.println("=======================================================================================================================================================================================================================================================================================");

        System.out.printf(
            "%-4s %-22s %-8s %-8s %-10s %-10s %-12s %-12s %-10s %-5s %-12s %-20s %-12s %-20s %-19s %-5s %-10s %-10s %-20s%n",
            "ID","REF ID","CHNL","TYPE","TXN_ST","PROC_ST",
            "AMOUNT","GROSS","FEE","CUR",
            "SND_IFSC","SND_BANK",
            "RCV_IFSC","RCV_BANK",
            "VALUE_DATE","PRIO",
            "PARTNER","MERCH","ERROR"
        );

        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        
        
        List<SettlementRecord> records = batch.getRecords();

        if (records == null || records.isEmpty()) {
            System.out.println("No records in this batch.");
            return;
        }

        int count = 1;

        for (SettlementRecord record : records) {

            List<IncomingTransaction> txns = record.getSettlementBatchRecordTransactions();

            if (txns == null || txns.isEmpty()) continue;

            for (IncomingTransaction txn : txns) {

                System.out.printf(
                    "%-4d %-22s %-8s %-8s %-10s %-10s %-12.2f %-12.2f %-10.2f %-5s %-12s %-20s %-12s %-20s %-19s %-5d %-10s %-10s %-20s%n",

                    txn.getIncomingTxnId(),
                    safe(txn.getSourceRef()),
                    safe(txn.getChannelCode()),
                    safe(txn.getTxnType()),
                    safe(txn.getTxnStatus()),
                    safe(txn.getProcessingStatus()),

                    val(txn.getAmount()),
                    val(txn.getGrossAmount()),
                    val(txn.getFeeAmount()),
                    safe(txn.getCurrency()),

                    safe(txn.getSenderIfsc()),
                    trim(safe(txn.getSenderBankName()), 20),

                    safe(txn.getReceiverIfsc()),
                    trim(safe(txn.getReceiverBankName()), 20),

                    safe(txn.getValueDate()),
                    txn.getPriority(),

                    safe(txn.getPartnerName()),
                    safe(txn.getMerchantId()),
                    trim(safe(txn.getErrorMessage()), 20)
                );
            }
        }

        System.out.println("===================================================================================================================================================================================================================================================================================================\n");
    }
    public List<SettlementBatch> getAllBatchesWithRecords() {

        SettlementBatchDAO batchDAO = new SettlementBatchDAOImpl();
        SettlementRecordDAO recordDAO = new SettlementRecordDAOImpl();

        List<SettlementBatch> batches = batchDAO.getAllBatches();

        for (SettlementBatch batch : batches) {

            List<SettlementRecord> records =
                    recordDAO.findByBatchId(batch.getBatchId());

            for (SettlementRecord record : records) {
                batch.addRecord(record);
            }
        }

        return batches;
    }
    
    
    public SettlementBatch getBatchWithRecords(String batchId) {

        SettlementBatchDAO batchDAO = new SettlementBatchDAOImpl();
        SettlementRecordDAO recordDAO = new SettlementRecordDAOImpl();

        SettlementBatch batch = batchDAO.findByBatchId(batchId);

        if (batch == null) {
            System.out.println("Batch not found: " + batchId);
            return null;
        }

        List<SettlementRecord> records = recordDAO.findByBatchId(batchId);

        for (SettlementRecord record : records) {
            batch.addRecord(record); 
        }

        return batch;
    }

    public void printBatchListWithIndex(List<SettlementBatch> batches) {

        if (batches == null || batches.isEmpty()) {
            System.out.println("No settlement batches available.");
            return;
        }

        System.out.println("\n==========================================================================");
        System.out.println("                        AVAILABLE BATCHES                                 ");
        System.out.println("==========================================================================");

        System.out.printf(
            "%-5s %-35s %-20s %-20s%n",
            "No", "Batch ID", "Total Txns", "Total Amount"
        );

        System.out.println("--------------------------------------------------------------------------");

        int index = 1;

        for (SettlementBatch batch : batches) {
            System.out.printf(
                "%-5d %-35s %-20d %-20s%n",
                index++,
                batch.getBatchId(),
                batch.getTotalTransactions(),
                String.format("%,.2f", batch.getTotalAmount())
            );
        }

        System.out.println("==========================================================================");
    }
    
    private static String safe(Object o) {
        return o == null ? "N/A" : o.toString();
    }

    private static double val(BigDecimal b) {
        return b == null ? 0.00 : b.doubleValue();
    }

    private static String trim(String s, int max) {
        if (s == null) return "N/A";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
    
    public static void sendBatchToNpc(List<SettlementBatch> batches) {

        List<SettlementBatch> scheduledBatches = batches.stream()
                .filter(batch -> batch.getBatchStatus() == BatchStatus.SCHEDULED)
                .collect(Collectors.toList());

        if (scheduledBatches.isEmpty()) {
            System.out.println("No batches available for NPCI settlement");
            return;
        }

        for (SettlementBatch batch : scheduledBatches) {

            try {
                System.out.println("Processing Batch: " + batch.getBatchId());

                // Step 1: Create folder
                File folder = new File("output");
                if (!folder.exists()) folder.mkdir();

                // Step 2: Generate XML
                String xml = generateNpcXml(batch);

                // Step 3: Save XML
                saveXmlToFile(batch.getBatchId(), xml);

                // Step 4: Update status
                updateBatchStatus(batch.getBatchId(), "COMPLETED");

                System.out.println("✅ Batch sent to NPCI: " + batch.getBatchId());

            } catch (Exception e) {
                System.out.println("❌ Failed batch: " + batch.getBatchId());
                e.printStackTrace();
            }
        }
    }

	private static void updateBatchStatus(String batchId, String status) {
		// TODO Auto-generated method stub
		SettlementBatchDAOImpl.updateBatchStatus(batchId, status);
		
	}

	public static void saveXmlToFile(String batchId, String xml) {

	    try {
	        File file = new File("output/" + batchId + ".xml");

	        try (FileWriter writer = new FileWriter(file)) {
	            writer.write(xml);
	        }

	        System.out.println("XML generated: " + file.getAbsolutePath());

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public static String generateNpcXml(SettlementBatch batch) {

	    StringBuilder xml = new StringBuilder();

	    xml.append("<SettlementBatch>\n");
	    xml.append("  <BatchId>").append(batch.getBatchId()).append("</BatchId>\n");
	    xml.append("  <BatchDate>").append(batch.getBatchDate()).append("</BatchDate>\n");

	    xml.append("  <Transactions>\n");

	    for (SettlementRecord record : batch.getRecords()) {

	        for (IncomingTransaction txn : record.getSettlementBatchRecordTransactions()) {

	            xml.append("    <Transaction>\n");
	            xml.append("      <Ref>").append(txn.getSourceRef()).append("</Ref>\n");
	            xml.append("      <Amount>").append(txn.getAmount()).append("</Amount>\n");
	            xml.append("      <Channel>").append(txn.getChannelCode()).append("</Channel>\n");
	            xml.append("      <Status>").append(txn.getTxnStatus()).append("</Status>\n");
	            xml.append("    </Transaction>\n");
	        }
	    }

	    xml.append("  </Transactions>\n");
	    xml.append("</SettlementBatch>");

	    return xml.toString();
	}
	
	
	public static void viewXmlByIndex() {

		SettlementServiceImpl settlementService = new SettlementServiceImpl();
    	List<SettlementBatch> batches = settlementService.getAllBatchesWithRecords();
		Scanner scanner=new Scanner(System.in);
	    if (batches == null || batches.isEmpty()) {
	        System.out.println("⚠️ No batches available.");
	        return;
	    }

	    System.out.println("\n=========== NPCI SENT BATCHES ===========");

	    int index = 1;
	    for (SettlementBatch batch : batches) {
	        if (batch.getBatchStatus() == BatchStatus.COMPLETED) {
	            System.out.println(index + ". " + batch.getBatchId());
	            index++;
	        }
	    }

	    if (index == 1) {
	        System.out.println("⚠️ No SENT batches available.");
	        return;
	    }

	    System.out.println("\nSelect batch S.No to view XML:");
	    int choice = scanner.nextInt();
	    scanner.nextLine(); // consume newline

	    // 🔥 Step 2: Get selected batch
	    SettlementBatch selectedBatch = null;
	    int current = 1;

	    for (SettlementBatch batch : batches) {
	        if (batch.getBatchStatus() == BatchStatus.COMPLETED) {
	            if (current == choice) {
	                selectedBatch = batch;
	                break;
	            }
	            current++;
	        }
	    }

	    if (selectedBatch == null) {
	        System.out.println("❌ Invalid selection.");
	        return;
	    }

	    // Step 3: Show XML
	    viewXmlFile(selectedBatch.getBatchId());
	}

	public static void viewXmlFile(String batchId) {

	    File file = new File("output/" + batchId + ".xml");

	    if (!file.exists()) {
	        System.out.println("❌ XML file not found for batch: " + batchId);
	        return;
	    }

	    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

	        System.out.println("\n=========== XML CONTENT ===========");

	        String line;
	        while ((line = reader.readLine()) != null) {
	            System.out.println(line);
	        }

	        System.out.println("===================================\n");

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
    
}


//package com.iispl.service;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import com.iispl.dao.*;
//import com.iispl.entity.*;
//import com.iispl.enums.*;
//
//public class SettlementServiceImpl implements SettlementService {
//
//    private final SettlementBatchDAO batchDAO = new SettlementBatchDAOImpl();
//    private final SettlementRecordDAO recordDAO = new SettlementRecordDAOImpl();
//    private final TransactionDao txnDAO = new TransactionDaoImpl();
//    private final NettingPositionService nettingService = new NettingPositionServiceImpl();
//    private final SettlementInstructionDAO instructionDAO = new SettlementInstructionDAOImpl();
//
//    @Override
//    public void processSettlement() {
//
//        List<IncomingTransaction> txns = txnDAO.getAllTransactions();
//
//        if (txns.isEmpty()) {
//            System.out.println("❌ No Transactions → No Batch Created");
//            return;
//        }
//
//        // ✅ CHECK TODAY BATCH
//        SettlementBatch batch = batchDAO.findTodayBatch();
//
//        if (batch == null) {
//            batch = new SettlementBatch("BCH" + System.currentTimeMillis(), "SYSTEM");
//            batchDAO.save(batch);
//        }
//
//        // ✅ PROCESS EACH TXN
//        for (IncomingTransaction txn : txns) {
//
//            SettlementRecord record = new SettlementRecord(
//                    batch.getBatchId(),
//                    txn.getId(),
//                    txn.getAmount(),
//                    SettlementStatus.SETTLED
//            );
//
//            recordDAO.save(record);
//            batch.addRecord(record);
//
//            // ✅ CREATE INSTRUCTION
//            SettlementInstruction ins = new SettlementInstruction(
//                    "INS" + System.nanoTime(),
//                    txn.getId(),
//                    ChannelType.valueOf(txn.getChannelCode()),
//                    5,
//                    txn.getValueDate(),
//                    txn.getSenderBankName(),
//                    txn.getReceiverBankName(),
//                    InstructionStatus.SENT
//            );
//
//            instructionDAO.save(ins);
//        }
//
//        // ✅ NETTING
//        nettingService.calculateAndStore(txns);
//
//        System.out.println("✅ Settlement Completed for Batch: " + batch.getBatchId());
//    }
//}