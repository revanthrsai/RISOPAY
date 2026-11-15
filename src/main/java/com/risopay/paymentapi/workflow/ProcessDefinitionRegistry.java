package com.risopay.paymentapi.workflow;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProcessDefinitionRegistry {

    private final Map<BusinessFunctionCode, BusinessFunctionDefinition>
            businessFunctions = Map.of(
                    BusinessFunctionCode.INSTRUCTION_INTAKE,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.INSTRUCTION_INTAKE,
                            List.of(
                                    TaskCode.GENERATE_INSTRUCTION_ID,
                                    TaskCode.CHECK_FILE_LIMITS,
                                    TaskCode.STAGE_XML_INPUT
                            )
                    ),
                    BusinessFunctionCode.XML_VALIDATION,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.XML_VALIDATION,
                            List.of(
                                    TaskCode.IDENTIFY_MESSAGE_TYPE,
                                    TaskCode.VALIDATE_XSD
                            )
                    ),
                    BusinessFunctionCode.TRANSACTION_PROCESSING,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.TRANSACTION_PROCESSING,
                            List.of(
                                    TaskCode.EXTRACT_TRANSACTIONS,
                                    TaskCode.START_TRANSACTION_WORKFLOW
                            )
                    ),
                    BusinessFunctionCode.BUSINESS_VALIDATION,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.BUSINESS_VALIDATION,
                            List.of(TaskCode.APPLY_BUSINESS_RULES)
                    ),
                    BusinessFunctionCode.REPAIR_DECISION,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.REPAIR_DECISION,
                            List.of(TaskCode.DETERMINE_REPAIR_REQUIREMENT)
                    ),
                    BusinessFunctionCode.BULK_READINESS,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.BULK_READINESS,
                            List.of(TaskCode.MARK_READY_TO_BULK)
                    ),
                    BusinessFunctionCode.OUT_INSTRUCTION_CREATION,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.OUT_INSTRUCTION_CREATION,
                            List.of(TaskCode.CREATE_OUT_INSTRUCTION)
                    ),
                    BusinessFunctionCode.OUTPUT_FILE_GENERATION,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.OUTPUT_FILE_GENERATION,
                            List.of(
                                    TaskCode.GENERATE_OUTPUT_FILE,
                                    TaskCode.VALIDATE_OUTPUT_FILE
                            )
                    ),
                    BusinessFunctionCode.DELIVERY,
                    new BusinessFunctionDefinition(
                            BusinessFunctionCode.DELIVERY,
                            List.of(
                                    TaskCode.DELIVER_OUTPUT_FILE,
                                    TaskCode.RECONCILE_DELIVERY
                            )
                    )
            );

    private final Map<WorkflowCode, WorkflowDefinition> workflows = Map.of(
            WorkflowCode.IN_PAIN_001,
            new WorkflowDefinition(
                    WorkflowCode.IN_PAIN_001,
                    List.of(
                            BusinessFunctionCode.INSTRUCTION_INTAKE,
                            BusinessFunctionCode.XML_VALIDATION,
                            BusinessFunctionCode.TRANSACTION_PROCESSING
                    )
            ),
            WorkflowCode.PAYMENT_TRANSACTION,
            new WorkflowDefinition(
                    WorkflowCode.PAYMENT_TRANSACTION,
                    List.of(
                            BusinessFunctionCode.BUSINESS_VALIDATION,
                            BusinessFunctionCode.REPAIR_DECISION,
                            BusinessFunctionCode.BULK_READINESS
                    )
            ),
            WorkflowCode.OUT_PAYMENT_INSTRUCTION,
            new WorkflowDefinition(
                    WorkflowCode.OUT_PAYMENT_INSTRUCTION,
                    List.of(
                            BusinessFunctionCode.OUT_INSTRUCTION_CREATION,
                            BusinessFunctionCode.OUTPUT_FILE_GENERATION,
                            BusinessFunctionCode.DELIVERY
                    )
            )
    );

    public WorkflowDefinition workflow(WorkflowCode workflowCode) {
        return workflows.get(workflowCode);
    }

    public BusinessFunctionDefinition businessFunction(
            BusinessFunctionCode businessFunctionCode
    ) {
        return businessFunctions.get(businessFunctionCode);
    }
}
