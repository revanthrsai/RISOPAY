package com.risopay.paymentapi.model;

import java.math.BigDecimal;

/** RISOPAY's message-neutral representation of one payment transaction. */
public record PaymentTransaction(
        String paymentInfoId,
        String endToEndId,
        BigDecimal amount,
        String currency
) {
}
