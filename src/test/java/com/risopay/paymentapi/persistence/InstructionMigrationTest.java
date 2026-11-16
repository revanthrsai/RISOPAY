package com.risopay.paymentapi.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InstructionMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsInstructionTableThroughFlywayMigration() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'INSTRUCTION'
                """,
                Integer.class
        );

        Integer instructionIdColumnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'INSTRUCTION'
                  AND COLUMN_NAME = 'INSTRUCTION_ID'
                """,
                Integer.class
        );

        assertEquals(1, tableCount);
        assertEquals(1, instructionIdColumnCount);
    }
}
