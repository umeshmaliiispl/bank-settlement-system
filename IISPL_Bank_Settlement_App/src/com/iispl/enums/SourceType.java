package com.iispl.enums;


/**
 * SourceType — identifies which external system sent the transaction.
 *
 *  CBS      — Core Banking System (internal direct-DB feed)
 *  RTGS     — Real Time Gross Settlement  (RBI high-value)
 *  SWIFT    — Cross-border MT103 payments (international)
 *  NEFT     — National Electronic Funds Transfer  (NPCI batch)
 *  UPI      — Unified Payments Interface  (NPCI real-time)
 *  FINTECH  — Third-party API partners (Razorpay, Paytm, PhonePe…)
 *  INTERNAL — Internal book-transfers (no external source)
 */


public enum SourceType {
    CBS,
    RTGS,
    SWIFT,
    NEFT,
    UPI,
    FINTECH,
    INTERNAL
}


