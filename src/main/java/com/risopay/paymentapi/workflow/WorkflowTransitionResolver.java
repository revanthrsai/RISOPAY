package com.risopay.paymentapi.workflow;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkflowTransitionResolver {

    private final Map<TransitionKey, WorkflowTransition> transitions = Map.ofEntries(
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.IN_PAIN_001,
                            BusinessFunctionCode.INSTRUCTION_INTAKE,
                            BusinessFunctionResult.COMPLETED
                    ),
                    running(BusinessFunctionCode.XML_VALIDATION)
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.IN_PAIN_001,
                            BusinessFunctionCode.XML_VALIDATION,
                            BusinessFunctionResult.COMPLETED
                    ),
                    running(BusinessFunctionCode.TRANSACTION_PROCESSING)
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.IN_PAIN_001,
                            BusinessFunctionCode.TRANSACTION_PROCESSING,
                            BusinessFunctionResult.COMPLETED
                    ),
                    complete()
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.PAYMENT_TRANSACTION,
                            BusinessFunctionCode.BUSINESS_VALIDATION,
                            BusinessFunctionResult.COMPLETED
                    ),
                    running(BusinessFunctionCode.REPAIR_DECISION)
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.PAYMENT_TRANSACTION,
                            BusinessFunctionCode.REPAIR_DECISION,
                            BusinessFunctionResult.COMPLETED
                    ),
                    running(BusinessFunctionCode.BULK_READINESS)
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.PAYMENT_TRANSACTION,
                            BusinessFunctionCode.REPAIR_DECISION,
                            BusinessFunctionResult.REPAIR_REQUIRED
                    ),
                    waiting()
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.PAYMENT_TRANSACTION,
                            BusinessFunctionCode.REPAIR_DECISION,
                            BusinessFunctionResult.REJECTED
                    ),
                    complete()
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.PAYMENT_TRANSACTION,
                            BusinessFunctionCode.BULK_READINESS,
                            BusinessFunctionResult.COMPLETED
                    ),
                    complete()
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.OUT_PAYMENT_INSTRUCTION,
                            BusinessFunctionCode.OUT_INSTRUCTION_CREATION,
                            BusinessFunctionResult.COMPLETED
                    ),
                    running(BusinessFunctionCode.OUTPUT_FILE_GENERATION)
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.OUT_PAYMENT_INSTRUCTION,
                            BusinessFunctionCode.OUTPUT_FILE_GENERATION,
                            BusinessFunctionResult.COMPLETED
                    ),
                    running(BusinessFunctionCode.DELIVERY)
            ),
            Map.entry(
                    new TransitionKey(
                            WorkflowCode.OUT_PAYMENT_INSTRUCTION,
                            BusinessFunctionCode.DELIVERY,
                            BusinessFunctionResult.COMPLETED
                    ),
                    complete()
            )
    );

    public WorkflowTransition resolve(
            WorkflowCode workflowCode,
            BusinessFunctionCode currentBusinessFunction,
            BusinessFunctionResult result
    ) {
        if (result == BusinessFunctionResult.FAILED) {
            return new WorkflowTransition(null, WorkflowState.FAILED);
        }

        WorkflowTransition transition = transitions.get(
                new TransitionKey(workflowCode, currentBusinessFunction, result)
        );

        if (transition == null) {
            throw new IllegalArgumentException(
                    "No workflow transition is defined for "
                            + workflowCode
                            + ", "
                            + currentBusinessFunction
                            + ", "
                            + result
            );
        }

        return transition;
    }

    private static WorkflowTransition running(
            BusinessFunctionCode nextBusinessFunction
    ) {
        return new WorkflowTransition(nextBusinessFunction, WorkflowState.RUNNING);
    }

    private static WorkflowTransition waiting() {
        return new WorkflowTransition(null, WorkflowState.WAITING);
    }

    private static WorkflowTransition complete() {
        return new WorkflowTransition(null, WorkflowState.COMPLETED);
    }

    private record TransitionKey(
            WorkflowCode workflowCode,
            BusinessFunctionCode currentBusinessFunction,
            BusinessFunctionResult result
    ) {
    }
}
