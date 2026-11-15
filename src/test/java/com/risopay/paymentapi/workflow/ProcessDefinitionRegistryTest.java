package com.risopay.paymentapi.workflow;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessDefinitionRegistryTest {

    private final ProcessDefinitionRegistry registry =
            new ProcessDefinitionRegistry();

    private final WorkflowTransitionResolver transitionResolver =
            new WorkflowTransitionResolver();

    @Test
    void definesPain001IncomingProcessingOrder() {
        assertEquals(
                java.util.List.of(
                        BusinessFunctionCode.INSTRUCTION_INTAKE,
                        BusinessFunctionCode.XML_VALIDATION,
                        BusinessFunctionCode.TRANSACTION_PROCESSING
                ),
                registry.workflow(WorkflowCode.IN_PAIN_001)
                        .businessFunctions()
        );
    }

    @Test
    void definesXsdValidationTasks() {
        assertEquals(
                java.util.List.of(
                        TaskCode.IDENTIFY_MESSAGE_TYPE,
                        TaskCode.VALIDATE_XSD
                ),
                registry.businessFunction(BusinessFunctionCode.XML_VALIDATION)
                        .tasks()
        );
    }

    @Test
    void startsIncomingInstructionWorkflowAtItsFirstBusinessFunction() {

        WorkflowInstance instance = WorkflowInstance.start(
                registry.workflow(WorkflowCode.IN_PAIN_001),
                WorkflowSubjectType.INSTRUCTION,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        assertEquals(WorkflowState.RUNNING, instance.state());
        assertEquals(
                BusinessFunctionCode.INSTRUCTION_INTAKE,
                instance.currentBusinessFunction()
        );
    }

    @Test
    void movesValidatedPain001InstructionToTransactionProcessing() {

        WorkflowTransition transition = transitionResolver.resolve(
                WorkflowCode.IN_PAIN_001,
                BusinessFunctionCode.XML_VALIDATION,
                BusinessFunctionResult.COMPLETED
        );

        assertEquals(
                BusinessFunctionCode.TRANSACTION_PROCESSING,
                transition.nextBusinessFunction()
        );
        assertEquals(WorkflowState.RUNNING, transition.nextState());
    }

    @Test
    void waitsWhenTransactionRequiresRepair() {

        WorkflowTransition transition = transitionResolver.resolve(
                WorkflowCode.PAYMENT_TRANSACTION,
                BusinessFunctionCode.REPAIR_DECISION,
                BusinessFunctionResult.REPAIR_REQUIRED
        );

        assertEquals(null, transition.nextBusinessFunction());
        assertEquals(WorkflowState.WAITING, transition.nextState());
    }
}
