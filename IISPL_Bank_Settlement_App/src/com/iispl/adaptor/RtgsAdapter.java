package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.*;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

public class RtgsAdapter implements TransactionAdapter {

    private static final SourceSystem RTGS_SOURCE = SourceSystem.RTGS();
    private static final BigDecimal MIN = new BigDecimal("200000");

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.RTGS,
                    rawPayload,
                    "rawPayload is null");

        IncomingTransaction txn = new IncomingTransaction();

        txn.setSourceSystem(RTGS_SOURCE);
        txn.setChannelCode("RTGS");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("RTGS-ADAPTER");
        txn.setPriority(1);

        txn.setSourceRef(extract(rawPayload, "utr"));
        txn.setSenderIfsc(extract(rawPayload, "senderIFSC"));
        txn.setReceiverIfsc(extract(rawPayload, "receiverIFSC"));
        txn.setCurrency(extract(rawPayload, "currency"));

        txn.setAmount(new BigDecimal(extract(rawPayload, "amount")));
        txn.setGrossAmount(txn.getAmount());
        txn.setFeeAmount(BigDecimal.ZERO);

        txn.setValueDate(LocalDate.parse(extract(rawPayload, "valueDate")));

        String msgType = extract(rawPayload, "msgType");
        txn.setTxnType(msgType.contains("CREDIT")
                ? TransactionType.CREDIT
                : TransactionType.DEBIT);

        // 🔥 STATUS SUPPORT
        String status = safeExtract(rawPayload, "status");

        txn.setTxnStatus(
                status.isEmpty()
                        ? TransactionStatus.SUCCESS
                        : TransactionStatus.valueOf(status.toUpperCase())
        );

        txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));
        txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // Business rule
        if (txn.getAmount().compareTo(MIN) < 0)
            throw new IngestionException(
                    IngestionException.ERR_BUSINESS_RULE,
                    SourceType.RTGS,
                    rawPayload,
                    "Below RTGS limit");

        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.RTGS);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
        } else {
            txn.setProcessingStatus(
                    txn.getTxnStatus() == TransactionStatus.SUCCESS
                            ? ProcessingStatus.QUEUED
                            : ProcessingStatus.VALIDATED
            );
        }

        System.out.println("  [RTGS-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    private String extract(String json, String key) {
        String s = "\"" + key + "\"";
        int i = json.indexOf(s);
        int start = json.indexOf("\"", i + s.length() + 1);
        int end = json.indexOf("\"", start + 1);
        return json.substring(start + 1, end);
    }

    private String safeExtract(String json, String key) {
        try { return extract(json, key); }
        catch (Exception e) { return ""; }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "CHECKSUM-ERROR";
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.RTGS;
    }
}