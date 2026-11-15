package com.risopay.paymentapi.processing;

import com.risopay.paymentapi.model.PaymentTransaction;

import java.util.ArrayList;
import java.util.List;

/** Current-compatible sink used when callers need all transactions in memory. */
public final class InMemoryPaymentTransactionSink
        implements PaymentTransactionSink {

    private final List<PaymentTransaction> transactions = new ArrayList<>();

    @Override
    public void accept(PaymentTransaction transaction) {
        transactions.add(transaction);
    }

    public List<PaymentTransaction> transactions() {
        return List.copyOf(transactions);
    }
}
