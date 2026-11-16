package com.risopay.paymentapi.transaction;

public enum PaymentTransactionStatus {
    PROCESSING,
    READY_TO_BULK,
    REJECTED,
    REPAIR_REQUIRED,
    ACCEPTED
}
