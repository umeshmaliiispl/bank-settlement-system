package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

public class TransactionDaoImpl implements TransactionDao{

    public void save(IncomingTransaction txn) {

        String sql = "INSERT INTO incoming_transaction (" +
                "source_system_id, source_ref, raw_payload, normalized_payload, " +
                "txn_type, amount, gross_amount, fee_amount, currency, value_date, " +
                "txn_status, processing_status, sender_ifsc, receiver_ifsc, " +
                "sender_bank_name, receiver_bank_name, sender_bic, receiver_bic, " +
                "partner_name, merchant_id, channel_code, checksum, error_message" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            
            ps.setLong(1, getSourceSystemId(txn.getChannelCode()));
            
//            ps.setLong(1, 1); // ✅ assume CBS = 1, NEFT = 2 etc.
////            ps.setLong(1, txn.getSourceSystem().getId()); // ⚠️ ensure ID exists
            ps.setString(2, txn.getSourceRef());
            ps.setString(3, txn.getRawPayload());
            ps.setString(4, txn.getNormalizedPayload());

            ps.setString(5, txn.getTxnType().name());
            ps.setBigDecimal(6, txn.getAmount());
            ps.setBigDecimal(7, txn.getGrossAmount());
            ps.setBigDecimal(8, txn.getFeeAmount());
            ps.setString(9, txn.getCurrency());
            ps.setObject(10, txn.getValueDate());

            ps.setString(11, txn.getTxnStatus().name());
            ps.setString(12, txn.getProcessingStatus().name());

            ps.setString(13, txn.getSenderIfsc());
            ps.setString(14, txn.getReceiverIfsc());

            ps.setString(15, txn.getSenderBankName());
            ps.setString(16, txn.getReceiverBankName());

            ps.setString(17, txn.getSenderBic());
            ps.setString(18, txn.getReceiverBic());

            ps.setString(19, txn.getPartnerName());
            ps.setString(20, txn.getMerchantId());

            ps.setString(21, txn.getChannelCode());
            ps.setString(22, txn.getChecksum());
            ps.setString(23, txn.getErrorMessage());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("DB Insert Failed", e);
        }
    }
    
    public static List<IncomingTransaction> getAllTransactions() {

        String sql = "SELECT * FROM incoming_transaction";
        List<IncomingTransaction> transactions = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                IncomingTransaction txn = new IncomingTransaction();

                txn.setSourceRef(rs.getString("source_ref"));
                txn.setRawPayload(rs.getString("raw_payload"));
                txn.setNormalizedPayload(rs.getString("normalized_payload"));

                txn.setTxnType(TransactionType.valueOf(rs.getString("txn_type")));
                txn.setAmount(rs.getBigDecimal("amount"));
                txn.setGrossAmount(rs.getBigDecimal("gross_amount"));
                txn.setFeeAmount(rs.getBigDecimal("fee_amount"));
                txn.setCurrency(rs.getString("currency"));
                txn.setIngestTimestamp(rs.getTimestamp("ingest_timestamp").toLocalDateTime());
                
                txn.setTxnStatus(TransactionStatus.valueOf(rs.getString("txn_status")));
                txn.setProcessingStatus(ProcessingStatus.valueOf(rs.getString("processing_status")));

                txn.setSenderIfsc(rs.getString("sender_ifsc"));
                txn.setReceiverIfsc(rs.getString("receiver_ifsc"));

                txn.setSenderBankName(rs.getString("sender_bank_name"));
                txn.setReceiverBankName(rs.getString("receiver_bank_name"));

                txn.setSenderBic(rs.getString("sender_bic"));
                txn.setReceiverBic(rs.getString("receiver_bic"));

                txn.setPartnerName(rs.getString("partner_name"));
                txn.setMerchantId(rs.getString("merchant_id"));

                txn.setChannelCode(rs.getString("channel_code"));
                txn.setChecksum(rs.getString("checksum"));
                txn.setErrorMessage(rs.getString("error_message"));

                transactions.add(txn);
            }

        } catch (Exception e) {
            throw new RuntimeException("DB Fetch Failed", e);
        }

        return transactions;
    }
    
    private Long getSourceSystemId(String channel) {
        switch (channel) {
            case "CBS": return 1L;
            case "NEFT": return 2L;
            case "UPI": return 3L;
            case "RTGS": return 4L;
            case "SWIFT": return 5L;
            case "FINTECH": return 6L;
            default: return 1L;
        }
    }   
}