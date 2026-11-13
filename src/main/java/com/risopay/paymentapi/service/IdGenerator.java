
package com.risopay.paymentapi.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class IdGenerator {

    private static final char[] DIGITS =
            "0123456789".toCharArray();

    private static final int SUFFIX_LENGTH = 8;

    private final SecureRandom secureRandom = new SecureRandom();

    public String newInstructionId() {
        return generate("RISOINS");
    }

    public String newBatchId() {
        return generate("RISOBAT");
    }

    public String newTransactionId() {
        return generate("RISOTXN");
    }

    private String generate(String prefix) {

        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);

        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(DIGITS[secureRandom.nextInt(DIGITS.length)]);
        }

        return prefix + suffix;
    }
}
