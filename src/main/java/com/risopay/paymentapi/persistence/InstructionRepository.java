package com.risopay.paymentapi.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstructionRepository
        extends JpaRepository<InstructionEntity, UUID> {

    Optional<InstructionEntity> findByInstructionId(String instructionId);

    boolean existsByInstructionId(String instructionId);
}
