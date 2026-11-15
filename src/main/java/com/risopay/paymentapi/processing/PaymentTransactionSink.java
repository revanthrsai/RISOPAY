package com.risopay.paymentapi.processing;

import com.risopay.paymentapi.model.PaymentTransaction;

/** Receives each transaction as it is extracted from an XML instruction. */
public interface PaymentTransactionSink {

    void accept(PaymentTransaction transaction);
}
