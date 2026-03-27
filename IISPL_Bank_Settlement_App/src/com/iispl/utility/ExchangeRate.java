package com.iispl.utility;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * ExchangeRate — FX rate snapshot for a currency pair on a given date.
 *
 * Used by the settlement engine to convert foreign currency transactions
 * (mainly SWIFT cross-border) into INR for netting and settlement.
 *
 * FIELDS:
 *   baseCurrency  — the currency being priced      e.g. USD
 *   quoteCurrency — the pricing currency           e.g. INR
 *   rate          — mid-market rate                e.g. 83.45
 *   bidRate       — bank buys base at this rate    e.g. 83.40
 *   askRate       — bank sells base at this rate   e.g. 83.50
 *
 * EXAMPLE:
 *   base=USD, quote=INR, rate=83.45
 *   → 1 USD = 83.45 INR
 *   → convert(1000 USD) = 83,450 INR
 *
 * OWNED BY: Umesh (Team Lead)
 * USED BY:  Nayeem (NettingEngine calls convert() for cross-currency netting)
 *           Varalaxmi (InterBankTransaction may carry FX rate)
 *
 * DB TABLE: exchange_rate
 */
public class ExchangeRate {

    private Long rateId;
    private String baseCurrency;     // ISO 4217 — e.g. USD, EUR, GBP
    private String quoteCurrency;    // ISO 4217 — e.g. INR
    private BigDecimal rate;         // mid-market rate
    private BigDecimal bidRate;      // bank buys base at this
    private BigDecimal askRate;      // bank sells base at this
    private LocalDate rateDate;      // date this rate is valid for
    private String source;           // RBI / Bloomberg / Reuters
    private boolean isActive;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ExchangeRate() {
        this.rateDate = LocalDate.now();
        this.isActive = true;
    }

    public ExchangeRate(String baseCurrency, String quoteCurrency,
                        BigDecimal rate, BigDecimal bidRate, BigDecimal askRate,
                        String source) {
        this();
        this.baseCurrency  = baseCurrency.toUpperCase();
        this.quoteCurrency = quoteCurrency.toUpperCase();
        this.rate          = rate;
        this.bidRate       = bidRate;
        this.askRate       = askRate;
        this.source        = source;
    }

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Convert an amount in baseCurrency → quoteCurrency using mid rate.
     *
     * Example: convert(1000) where base=USD, quote=INR, rate=83.45
     *          → returns 83,450.00 INR
     *
     * @param amountInBase  amount in the base currency (e.g. USD amount)
     * @return              equivalent amount in quote currency (e.g. INR)
     */
    public BigDecimal convert(BigDecimal amountInBase) {
        if (amountInBase == null || rate == null) {
            throw new IllegalStateException(
                "[ExchangeRate] Cannot convert — amount or rate is null."
            );
        }
        return amountInBase.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Convert using ASK rate (used when bank sells base currency to customer).
     */
    public BigDecimal convertAtAsk(BigDecimal amountInBase) {
        if (askRate == null) return convert(amountInBase);
        return amountInBase.multiply(askRate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Convert using BID rate (used when bank buys base currency from customer).
     */
    public BigDecimal convertAtBid(BigDecimal amountInBase) {
        if (bidRate == null) return convert(amountInBase);
        return amountInBase.multiply(bidRate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Returns the spread = askRate - bidRate.
     * Wider spread = more bank profit on the conversion.
     */
    public BigDecimal getSpread() {
        if (bidRate == null || askRate == null) return BigDecimal.ZERO;
        return askRate.subtract(bidRate);
    }

    /**
     * Returns a human-readable pair string, e.g. "USD/INR"
     */
    public String getPair() {
        return baseCurrency + "/" + quoteCurrency;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getRateId() { return rateId; }
    public void setRateId(Long rateId) { this.rateId = rateId; }

    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency.toUpperCase(); }

    public String getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency.toUpperCase(); }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getBidRate() { return bidRate; }
    public void setBidRate(BigDecimal bidRate) { this.bidRate = bidRate; }

    public BigDecimal getAskRate() { return askRate; }
    public void setAskRate(BigDecimal askRate) { this.askRate = askRate; }

    public LocalDate getRateDate() { return rateDate; }
    public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ExchangeRate{"
             + getPair()
             + ", rate=" + rate
             + ", bid=" + bidRate
             + ", ask=" + askRate
             + ", date=" + rateDate
             + ", source=" + source
             + "}";
    }
}
