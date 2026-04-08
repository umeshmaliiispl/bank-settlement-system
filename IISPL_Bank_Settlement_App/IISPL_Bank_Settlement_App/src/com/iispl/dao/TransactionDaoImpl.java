package com.iispl.dao;

import java.sql.*;
import java.sql.Date;
import java.util.*;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.*;

public class TransactionDaoImpl implements TransactionDao {

    // =========================================================
    // ✅ SAVE (ALREADY GOOD - SMALL SAFE FIX)
    // =========================================================
    @Override
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
            ps.setString(2, txn.getSourceRef());
            ps.setString(3, txn.getRawPayload());
            ps.setString(4, txn.getNormalizedPayload());

            ps.setString(5, txn.getTxnType().name());
            ps.setBigDecimal(6, txn.getAmount());
            ps.setBigDecimal(7, txn.getGrossAmount());
            ps.setBigDecimal(8, txn.getFeeAmount());
            ps.setString(9, txn.getCurrency());

            // ✅ FIX (IMPORTANT)
            ps.setDate(10, txn.getValueDate() != null ? Date.valueOf(txn.getValueDate()) : null);

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

    // =========================================================
    // 🔥 REQUIRED FOR SETTLEMENT
    // =========================================================
    @Override
    public List<IncomingTransaction> findAll() {

        List<IncomingTransaction> list = new ArrayList<>();

        String sql = "SELECT * FROM incoming_transaction";

        try (Connection con = DatabaseConfig.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                IncomingTransaction txn = mapRow(rs);
                list.add(txn);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // 🔥 FIND BY ID (MENU OPTION 5)
    // =========================================================
    @Override
    public IncomingTransaction findById(long id) {

        String sql = "SELECT * FROM incoming_transaction WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // 🔥 COMMON MAPPER (VERY IMPORTANT CLEAN CODE)
    // =========================================================
    private IncomingTransaction mapRow(ResultSet rs) throws Exception {

        IncomingTransaction txn = new IncomingTransaction();

        txn.setId(rs.getLong("id"));
        txn.setSourceRef(rs.getString("source_ref"));

        txn.setAmount(rs.getBigDecimal("amount"));
        txn.setGrossAmount(rs.getBigDecimal("gross_amount"));
        txn.setFeeAmount(rs.getBigDecimal("fee_amount"));

        txn.setCurrency(rs.getString("currency"));

        Date valueDate = rs.getDate("value_date");
        if (valueDate != null) {
            txn.setValueDate(valueDate.toLocalDate());
        }

        txn.setSenderBankName(rs.getString("sender_bank_name"));
        txn.setReceiverBankName(rs.getString("receiver_bank_name"));

        txn.setSenderIfsc(rs.getString("sender_ifsc"));
        txn.setReceiverIfsc(rs.getString("receiver_ifsc"));

        txn.setChannelCode(rs.getString("channel_code"));

        // ✅ ENUM MAPPING (VERY IMPORTANT)
        txn.setTxnType(TransactionType.valueOf(rs.getString("txn_type")));
        txn.setTxnStatus(TransactionStatus.valueOf(rs.getString("txn_status")));
        txn.setProcessingStatus(
                ProcessingStatus.valueOf(rs.getString("processing_status"))
        );

        return txn;
    }

    // =========================================================
    // 🔥 SOURCE SYSTEM MAPPING
    // =========================================================
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

    @Override
    public List<IncomingTransaction> getAllTransactions() {
        return findAll(); // ✅ FIX
    }
}