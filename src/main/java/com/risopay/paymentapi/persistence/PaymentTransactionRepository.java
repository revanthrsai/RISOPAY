package com.risopay.paymentapi.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransactionEntity, UUID> {
}
