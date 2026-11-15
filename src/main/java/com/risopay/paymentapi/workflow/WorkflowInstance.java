package com.risopay.paymentapi.workflow;

import java.time.Instant;
import java.util.UUID;

public record WorkflowInstance(
        UUID id,
        WorkflowCode workflowCode,
        WorkflowSubjectType subjectType,
        UUID subjectId,
        UUID correlationId,
        BusinessFunctionCode currentBusinessFunction,
        WorkflowState state,
        Instant startedAt,
        Instant completedAt
) {
    public static WorkflowInstance start(
            WorkflowDefinition definition,
            WorkflowSubjectType subjectType,
            UUID subjectId,
            UUID correlationId
    ) {
        return new WorkflowInstance(
                UUID.randomUUID(),
                definition.code(),
                subjectType,
                subjectId,
                correlationId,
                definition.businessFunctions().getFirst(),
                WorkflowState.RUNNING,
                Instant.now(),
                null
        );
    }
}
