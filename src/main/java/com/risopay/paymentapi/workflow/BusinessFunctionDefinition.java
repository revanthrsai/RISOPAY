package com.risopay.paymentapi.workflow;

import java.util.List;

public record BusinessFunctionDefinition(
        BusinessFunctionCode code,
        List<TaskCode> tasks
) {
    public BusinessFunctionDefinition {
        tasks = List.copyOf(tasks);
    }
}
