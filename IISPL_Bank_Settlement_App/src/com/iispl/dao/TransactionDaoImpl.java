package com.iispl.dao;

import java.sql.*;
import java.sql.Date;
import java.util.*;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.Transaction;
import com.iispl.enums.*;

public class TransactionDaoImpl implements TransactionDao {

    // =========================================================
    // SAVE
    // =========================================================
    @Override
    public void save(IncomingTransaction txn) {

        String sql = "INSERT INTO incoming_transaction (" +
                "source_system_id, source_ref, raw_payload, normalized_payload, " +
                "txn_type, amount, gross_amount, fee_amount, currency, value_date, " +
                "txn_status, processing_status, sender_ifsc, receiver_ifsc, " +
                "sender_bank_name, receiver_bank_name, sender_bic, receiver_bic, " +
                "partner_name, merchant_id, channel_code, checksum, error_message" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                "ON CONFLICT (source_system_id, source_ref) DO NOTHING";

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
            System.out.println("⚠ Skipped duplicate or error: " + txn.getSourceRef());
        }
    }

    // =========================================================
    // FETCH ALL
    // =========================================================
    @Override
    public List<IncomingTransaction> findAll() {

        List<IncomingTransaction> list = new ArrayList<>();

        String sql = "SELECT * FROM incoming_transaction ORDER BY id";

        try (Connection con = DatabaseConfig.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // ROW MAPPER (FULL DB MAPPING)
    // =========================================================
    private IncomingTransaction mapRow(ResultSet rs) throws Exception {

        IncomingTransaction txn = new IncomingTransaction();

        // BASIC
        txn.setId(rs.getLong("id"));
        txn.setSourceRef(rs.getString("source_ref"));

        txn.setRawPayload(rs.getString("raw_payload"));
        txn.setNormalizedPayload(rs.getString("normalized_payload"));

        // CORE
        txn.setTxnType(TransactionType.valueOf(rs.getString("txn_type")));
        txn.setAmount(rs.getBigDecimal("amount"));
        txn.setGrossAmount(rs.getBigDecimal("gross_amount"));
        txn.setFeeAmount(rs.getBigDecimal("fee_amount"));
        txn.setCurrency(rs.getString("currency"));

        Timestamp valueTs = rs.getTimestamp("value_date");
        if (valueTs != null) {
            txn.setValueDate(valueTs.toLocalDateTime().toLocalDate());
        }

        // STATUS
        txn.setTxnStatus(TransactionStatus.valueOf(rs.getString("txn_status")));
        txn.setProcessingStatus(ProcessingStatus.valueOf(rs.getString("processing_status")));

        // BANK DETAILS
        txn.setSenderIfsc(rs.getString("sender_ifsc"));
        txn.setReceiverIfsc(rs.getString("receiver_ifsc"));
        txn.setSenderBankName(rs.getString("sender_bank_name"));
        txn.setReceiverBankName(rs.getString("receiver_bank_name"));

        txn.setSenderBic(rs.getString("sender_bic"));
        txn.setReceiverBic(rs.getString("receiver_bic"));

        // FINTECH
        txn.setPartnerName(rs.getString("partner_name"));
        txn.setMerchantId(rs.getString("merchant_id"));

        // EXTRA
        txn.setChannelCode(rs.getString("channel_code"));
        txn.setChecksum(rs.getString("checksum"));
        txn.setErrorMessage(rs.getString("error_message"));

        txn.setPriority(rs.getInt("priority"));
       // txn.setRetryCount(rs.getInt("retry_count")); // if exists

        // TIMESTAMPS
        Timestamp ingest = rs.getTimestamp("ingest_timestamp");
        if (ingest != null) txn.setIngestTimestamp(ingest.toLocalDateTime());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) txn.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) txn.setUpdatedAt(updated.toLocalDateTime());

        txn.setVersion(rs.getInt("version"));

        return txn;
    }

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
        return findAll();
    }

	@Override
	public void save(Transaction transaction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IncomingTransaction findById(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Long id) {
		// TODO Auto-generated method stub
		
	}
}