package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

public class FintechAdapter implements TransactionAdapter {

    private static final SourceSystem FINTECH_SOURCE = SourceSystem.FINTECH();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.FINTECH,
                    rawPayload,
                    "rawPayload is null or empty");

        // 🔥 Extract status FIRST
        String status = safeExtract(rawPayload, "status");

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(FINTECH_SOURCE);
        txn.setChannelCode("FINTECH");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("FINTECH-ADAPTER");
        txn.setPriority(7);

        // 🔥 Set TransactionStatus (IMPORTANT)
        try {
            txn.setTxnStatus(TransactionStatus.valueOf(status.toUpperCase()));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.FINTECH,
                    rawPayload,
                    "Invalid status: " + status
            );
        }

        // ── Mapping ─────────────────────
        txn.setSourceRef(extract(rawPayload, "partner_ref"));
        txn.setPartnerName(extract(rawPayload, "partner_name"));
        txn.setMerchantId(safeExtract(rawPayload, "merchant_id"));

        BigDecimal gross = new BigDecimal(extract(rawPayload, "gross_amount"));
        BigDecimal fee   = new BigDecimal(extract(rawPayload, "fee_amount"));
        BigDecimal net   = new BigDecimal(extract(rawPayload, "net_amount"));

        txn.setGrossAmount(gross);
        txn.setFeeAmount(fee);
        txn.setAmount(net);

        txn.setCurrency(extract(rawPayload, "currency").toUpperCase());
        txn.setValueDate(LocalDate.parse(extract(rawPayload, "settlement_date")));

        String type = extract(rawPayload, "payment_type").toUpperCase();
        txn.setTxnType(TransactionType.valueOf(type));

        txn.setSenderBankName(txn.getPartnerName());
        txn.setReceiverBankName("IISPL Bank");

        txn.setNormalizedPayload("{\"source\":\"FINTECH\",\"status\":\"" + txn.getTxnStatus() + "\"}");

        // ── Validation ─────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.FINTECH);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
        } else {
            txn.setProcessingStatus(
                    txn.getTxnStatus() == TransactionStatus.SUCCESS
                            ? ProcessingStatus.QUEUED
                            : ProcessingStatus.VALIDATED
            );
        }

        System.out.println("  [FINTECH-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.FINTECH;
    }

    private String extract(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        int s = json.indexOf("\"", i + search.length() + 2);
        int e = json.indexOf("\"", s + 1);
        return json.substring(s + 1, e);
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
}