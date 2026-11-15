package com.risopay.paymentapi.workflow;

import java.util.List;

public record WorkflowDefinition(
        WorkflowCode code,
        List<BusinessFunctionCode> businessFunctions
) {
    public WorkflowDefinition {
        businessFunctions = List.copyOf(businessFunctions);
    }
}
