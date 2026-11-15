package com.risopay.paymentapi.workflow;

import java.time.Instant;
import java.util.UUID;

public record WorkflowEvent(
        UUID id,
        UUID workflowInstanceId,
        long sequenceNumber,
        BusinessFunctionCode fromBusinessFunction,
        BusinessFunctionCode toBusinessFunction,
        BusinessFunctionResult result,
        WorkflowState state,
        String detail,
        String actorId,
        Instant occurredAt
) {
}
