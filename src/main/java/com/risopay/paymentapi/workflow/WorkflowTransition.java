package com.risopay.paymentapi.workflow;

public record WorkflowTransition(
        BusinessFunctionCode nextBusinessFunction,
        WorkflowState nextState
) {
}
