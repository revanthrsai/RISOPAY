# RISOPAY — MASTER PROJECT CONTEXT AND CODEX HANDOFF

**Product:** RISOPAY  
**Organization/Product Identity:** ISO 20022 Academy  
**Repository:** https://github.com/revanthrsai/RISOPAY.git  
**Local Project Directory:** `R:\ISO-Academy\RISOPAY`  
**Current Development Tool:** OpenAI Codex  
**Primary Developer:** Revanth Sai  
**Project Type:** Enterprise-oriented ISO 20022 Payment Processing and Orchestration Platform

---

# 1. PROJECT VISION

RISOPAY is intended to become an enterprise-scale payment instruction processing and orchestration platform built around ISO 20022.

The objective is not simply to parse XML files. RISOPAY should eventually support the complete lifecycle of payment instructions, including:

- Payment file ingestion.
- ISO 20022 message identification.
- XSD validation.
- Business validation.
- Transaction-level processing.
- Payment batching.
- Rejection and repair workflows.
- Maker-checker approvals.
- Output file generation.
- Downstream payment delivery.
- Resending and reconciliation.
- Auditability and operational monitoring.

The platform must be designed for enterprise workloads and future AWS-native deployment.

## Core architectural philosophy

RISOPAY must not force every customer into one rigid payment workflow.

Customers should have configurable choices over how their payment instructions are validated, repaired, approved, released, and delivered.

The architecture should therefore separate:
- Core payment processing.
- ISO 20022 message-specific processing.
- Customer-configurable business rules.
- Workflow and approval decisions.
- Storage and infrastructure integrations.
- Downstream delivery mechanisms.

The application should remain runnable locally while being designed for eventual AWS deployment.

---

# 2. TECHNOLOGY STACK

Current project baseline:

- Java 21.
- Spring Boot 4.1.2.
- Maven.
- Spring MVC/Web.
- PostgreSQL planned for persistent application data.
- AWS as the intended deployment environment.
- React planned for the frontend.

The Spring Boot application currently runs on port 8080.

Java installation:
`C:\Program Files\Java\jdk-21.0.11`

Local project:
`R:\ISO-Academy\RISOPAY`

Application name:
`risopay`

Important: Inspect the actual repository before assuming a dependency, library, database, or integration has already been implemented.

---

# 3. CURRENT IMPLEMENTATION BASELINE

The latest project archive inspected was `RISOPAY(2).zip`.

The existing source package is:

`com.risopay.paymentapi`

Existing structure:

```text
com.risopay.paymentapi
├── controller
│   ├── HealthController.java
│   └── PaymentController.java
├── dto
│   ├── ApiErrorResponse.java
│   ├── BatchResponse.java
│   ├── PaymentInstructionResponse.java
│   └── TransactionResponse.java
├── exception
│   └── GlobalExceptionHandler.java
├── filter
│   └── XmlRequestSizeLimitFilter.java
├── parser
│   ├── PaymentDataExtractor.java
│   ├── XmlMessageTypeDetector.java
│   └── XsdValidator.java
├── registry
│   └── MessageDefinitionRegistry.java
└── service
    ├── IdGenerator.java
    └── PaymentBatchGrouper.java
```

Resources:

```text
src/main/resources
├── application.properties
└── xsd
    └── pain.001.001.03.xsd
```

Existing tests include:

- RisopayApplicationTests
- IdGeneratorTest
- XML test fixtures:
  - single-batch.xml
  - multiple-batches.xml
  - invalid-message.xml

The exact current repository may have changed since the ZIP inspection. Codex MUST inspect the working tree before making changes.

## Existing PaymentController

Endpoint:

`POST /api/v1/payment-instructions`

Consumes:

`application/xml`

Produces:

`application/json`

Currently accepts:

`@RequestBody String xmlPayload`

The controller coordinates:

- XmlMessageTypeDetector
- MessageDefinitionRegistry
- XsdValidator
- PaymentDataExtractor
- PaymentBatchGrouper
- IdGenerator

Current flow:

1. Detect ISO 20022 message type.
2. Find its message definition in the registry.
3. Validate XML against the registered XSD.
4. Extract payment information.
5. Generate transaction identifiers.
6. Group transactions into payment batches.
7. Generate the input instruction identifier.
8. Return a response with status `RECEIVED`.

The current implementation does not yet have the complete persistent payment-processing workflow.

## Existing XmlMessageTypeDetector

Uses DOM parsing and a namespace-aware DocumentBuilderFactory.

XXE protections are already implemented:

- DOCTYPE disabled.
- External general entities disabled.
- External parameter entities disabled.
- External DTD access disabled.
- External schema access disabled.

It currently builds a DOM document just to inspect the root namespace.

## Existing XsdValidator

- Uses a cached Schema.
- Cache is keyed by XSD resource path.
- Uses a fresh Validator for each validation.
- Restricts external DTD and schema access.
- Uses secure XML processing.
- Currently accepts a String XML payload.
- Uses StringReader for validation.

A temporary XSD compilation diagnostic was deliberately retained:

`Loading and compiling XSD: ...`

Do not remove useful diagnostics without discussing the reason.

## Existing PaymentDataExtractor

Currently uses DOM.

It navigates:

`Document → CstmrCdtTrfInitn → GrpHdr`

Extracts:

- MsgId.
- NbOfTxs.
- CtrlSum.
- PmtInfId.
- PmtInf transaction count.
- PmtInf control sum.
- EndToEndId.
- Instructed amount.
- Currency.

It loops through PmtInf elements and their CdtTrfTxInf elements.

It retains extracted transactions in a List.

It performs count and control-sum checks and throws IllegalArgumentException when expected values do not match.

## Existing PaymentBatchGrouper

- Accepts a complete List of ExtractedTransaction.
- Groups by PmtInfId.
- Uses LinkedHashMap.
- Generates batch IDs.
- Retains transaction lists in batch records.
- Returns no batches when there is one or fewer transactions, according to the existing implementation.

Preserve existing grouping behaviour unless an agreed requirement requires a change.

## Existing MessageDefinitionRegistry

Currently supports:

`pain.001.001.03`

Namespace:

`urn:iso:std:iso:20022:tech:xsd:pain.001.001.03`

It maps the namespace to:
- Message type.
- XSD resource path.

This registry should eventually resolve a message-specific processor as well.

## Existing IdGenerator

Uses SecureRandom and eight numeric digits.

Current formats:

- Input instruction: `RISOINS` + 8 digits.
- Batch: `RISOBAT` + 8 digits.
- Transaction: `RISOTXN` + 8 digits.

Future requirement:

- Output instruction: `RISOOUTINS` + 8 digits.

Random identifiers are not collision-proof. Persistent uniqueness constraints and collision handling will eventually be required.

---

# 4. CURRENT REQUEST FILTER AND ERROR HANDLING

`XmlRequestSizeLimitFilter.java` currently enforces a 50 MiB request limit.

Configuration:

`52428800` bytes.

The existing filter buffers accepted request bodies in memory using a byte array/ByteArrayOutputStream approach.

This is a major architectural limitation for large-file streaming.

The application must continue enforcing the 50 MiB limit while moving away from full-request heap buffering.

Existing behaviour and tests:

- A 51 MiB Content-Length request returned HTTP 413.
- Chunked oversized request detection was confirmed in the server log.
- Client-side receipt of HTTP 413 for the chunked request was not conclusively verified.
- Unsupported content type was tested and returned HTTP 415.
- Structured API errors are implemented through ApiErrorResponse and GlobalExceptionHandler.

ApiErrorResponse currently contains:

- timestamp
- status
- error
- errorCode
- message
- path
- details

Preserve existing HTTP status behaviour and error response structure during the streaming migration.

---

# 5. TESTING HISTORY AND VERIFIED BEHAVIOUR

The following outcomes were previously observed:

## Multiple PmtInf grouping

A test with five transactions and two PmtInf groups succeeded.

PMT-INF-001:
- 2 transactions.
- 500 EUR.
- 250 EUR.

PMT-INF-002:
- 3 transactions.
- 100 EUR.
- 200 EUR.
- 450 EUR.

Total:
1,500 EUR.

Two batches were produced with expected identifier prefixes.

## XSD validation and cache

- First request compiled the XSD.
- Second request reused the cached Schema.
- Invalid XML returned HTTP 400 with `XSD_VALIDATION_FAILED`.
- A valid request after an invalid request continued to work without recompiling the schema.

## Sequential Postman testing

Postman Collection Runner:
- 10 sequential iterations.
- 40 assertions passed.
- 0 errors.
- Average response time approximately 18 ms.

This is sequential repeated-request testing, not proof of concurrent load capacity.

## Concurrency

Previous PowerShell concurrency test attempts encountered working-directory and connection issues.

Actual concurrent load performance remains unverified.

## Maven tests

The earlier ZIP inspection environment did not have Maven installed.

Therefore, no claim should be made that the complete Maven test suite passed during that inspection.

Codex must run the actual project tests in the user's development environment.

---

# 6. PRIMARY ARCHITECTURAL CHANGE — PHASED STREAMING XML PROCESSING

This is the immediate engineering priority.

The existing implementation uses multiple full-document operations:

1. DOM parsing for message detection.
2. XSD validation using the complete String payload.
3. DOM parsing again for payment extraction.

This is not suitable for the intended large-file workload.

The target should support files containing approximately:

- 50,000 transactions.
- 100,000 transactions.
- 150,000 transactions.

Do not arbitrarily reduce these targets.

## Agreed phased architecture

### Phase 1 — Streaming XSD validation

Use a streaming XML validation approach against the registered message XSD.

Evaluate:

- SAX.
- ValidatorHandler.
- StAX integration.
- Secure XML parser configuration.

Requirements:

- No complete DOM document.
- No complete XML String.
- External entity access disabled.
- External DTD access disabled.
- External schema access controlled.
- Preserve existing XSD validation error semantics.

### Phase 2 — Incremental StAX extraction

Use StAX to process XML incrementally.

For the first supported message type, pain.001.001.03:

- Read GrpHdr.
- Retain required group-header context.
- Process each PmtInf.
- Process each CdtTrfTxInf individually.
- Extract required payment information.
- Apply the existing count and control-sum checks.
- Avoid retaining the complete XML document or all transaction objects in memory.

### Phase 3 — Per-transaction mapping and processing

Each extracted transaction should be mapped into a RISOPAY canonical payment transaction representation.

Evaluate:

- JAXB generated from XSD.
- Prowide ISO 20022.
- A controlled custom mapping implementation.

Do not commit to JAXB or Prowide without evaluating the trade-offs.

StAX remains the common streaming foundation.

The message-specific mapping and business rules should remain separate from the common streaming engine.

---

# 7. MESSAGE-SPECIFIC PROCESSING ARCHITECTURE

The common processing engine must not contain hardcoded knowledge of every ISO 20022 message structure.

Proposed responsibilities:

## StreamingXmlEngine

Common XML infrastructure.

Responsibilities:

- Manage the XML input source.
- Manage secure streaming reader creation.
- Coordinate parsing and resource cleanup.
- Provide the common processing infrastructure.

## MessageDefinitionRegistry

Resolve the incoming namespace and message type.

Eventually associate:

- Message type.
- XSD resource.
- Message-specific processor.
- Other message definition metadata.

## MessageProcessor

A common interface for message-specific processing.

The common engine should not need to know PmtInf or CdtTrfTxInf.

## Pain001Processor

The first message-specific processor.

Understands pain.001.001.03 structure and maps its data into RISOPAY canonical transaction models.

Future processors should be independently implementable, for example:

- pain.001.001.09.
- pain.008 variants.
- Other ISO 20022 payment messages.

Do not introduce these message types prematurely. The immediate objective is to establish an extensible architecture with pain.001.001.03 working correctly.

## PaymentTransactionSink

Introduce a transaction sink abstraction.

The streaming processor should emit individual transactions into this abstraction rather than requiring a complete in-memory List.

Possible future implementations:

- In-memory sink for tests.
- Database staging sink.
- Other storage adapters.

This is important because streaming the XML alone does not solve memory usage if the application still stores every extracted transaction in a large List.

---

# 8. CRITICAL INPUT DESIGN DECISION

The existing filter buffers accepted requests in memory, and the controller receives a String.

Both must be addressed.

The agreed immediate direction is a bounded, replayable, disk-backed XML input.

Proposed flow:

```text
Incoming HTTP XML request
        |
        v
Request-size enforcement
        |
        v
Bounded temporary-file-backed XML input
        |
        +---------------------------+
        |                           |
        v                           v
Streaming XSD validation      StAX extraction
        |                           |
        +-------------+-------------+
                      |
                      v
             Transaction processing
                      |
                      v
             Temporary-file cleanup
```

The reason for a replayable source is that validation and extraction may need separate reads.

The XML should not be held entirely in application heap merely to make it replayable.

Important implementation considerations:

- Preserve the 50 MiB limit.
- Count actual bytes, including chunked transfer requests.
- Ensure partial temporary files are cleaned up.
- Handle client disconnects.
- Handle parser failures.
- Ensure temporary files are deleted after processing.
- Consider disk-space exhaustion.
- Avoid leaking temporary file paths or content.
- Preserve the existing 413 and 415 behaviours.
- Keep XXE protections.
- Avoid creating an unbounded number of temporary files under concurrent load.

The temporary-file-backed approach is the agreed first step. Future S3-backed staging can be considered when implementing AWS deployment.

---

# 9. BUSINESS LIFECYCLE — INSTRUCTIONS

Use one shared instruction entity/table with a direction field.

Directions:

- IN — incoming instruction.
- OUT — outgoing instruction.

Each incoming bulk payment file receives one input instruction ID.

Each output file sent downstream receives one output instruction ID.

ID formats:

- Input: RISOINS########
- Output: RISOOUTINS########
- Batch: RISOBAT########
- Transaction: RISOTXN########

Each output instruction must reference the original input instruction and its source transaction or transactions.

A single input instruction can produce multiple output instructions over time.

Example:

- Initial file has 100 transactions.
- 99 are ready and released.
- 1 requires repair.
- Later, the repaired transaction is released through a new output instruction.

Never modify an already released output file to include repaired transactions.

## Instruction statuses supplied by the product owner

Positive:

- RECEIVED
- XSD_VALIDATION_COMPLETED
- VALIDATION_RULES_COMPLETED
- RELEASED

Negative:

- INVALID_XML_FORMAT
- UNIDENTIFIED_MESSAGE_TYPE
- XSD_VALIDATION_FAILED
- VALIDATION_RULES_FAILED
- REJECTED

Preserve these business status names unless a revised status model is explicitly agreed.

---

# 10. TRANSACTION LIFECYCLE

Transaction statuses supplied by the product owner:

- PROCESSING
- READY_TO_BULK
- REJECTED
- REPAIR_REQUIRED
- ACCEPTED

These statuses represent different stages of payment processing.

Do not treat instruction status and transaction status as interchangeable.

A single instruction can contain transactions with different outcomes.

For example:

- 99 transactions READY_TO_BULK.
- 1 transaction REJECTED.

The system must support partial release of the 99 ready transactions if the customer chooses not to repair the rejected transaction.

---

# 11. REPAIR WORKFLOW AND MAKER-CHECKER

The repair workflow must support customer-configurable repair decisions.

Requirements:

1. Only configured repairable fields can be edited.
2. Original submitted values must be preserved.
3. Repaired values must be separately recorded.
4. Repair attempts must be recorded.
5. Maker identity must be recorded.
6. Checker identity must be recorded.
7. Timestamps, reasons, decisions, and audit history must be preserved.
8. Maker and checker must be separated.
9. Maker submits a repair for approval.
10. Transaction enters WAITING_FOR_REPAIR_APPROVAL.
11. Checker approves or rejects the repair.
12. Approved repairs must be revalidated.
13. Successful repaired transactions return to READY_TO_BULK.
14. Failed repairs can be attempted again.
15. Users may ultimately choose to reject a transaction even when it is repairable.

If a repair succeeds, it must enter the next downstream release job and be placed in a NEW output instruction and NEW output file.

The previous output file must remain unchanged.

## Release flexibility

The user must decide whether eligible rejected transactions should be repaired or left rejected.

If the user chooses to repair them, the system must hold the relevant release decision until the repair decision/workflow is resolved.

If the user elects to leave rejected transactions, the remaining READY_TO_BULK transactions may be released.

Release eligibility should be explicitly modelled at instruction level.

Do not infer the entire release decision merely from whether a transaction is repairable.

---

# 12. OUTPUT INSTRUCTIONS AND DOWNSTREAM DELIVERY

Each output file receives one output instruction ID.

Format:

`RISOOUTINS` + 8 digits.

Each output instruction should be traceable to its original input instruction and source transaction records.

## Resend behaviour

The resend operation must remain available until the output instruction reaches RELEASED.

Resend must reuse:

- The same output instruction ID.
- The same output file.
- The same checksum.
- The same idempotency key.

Do not generate a new output instruction ID for every resend attempt.

## Ambiguous delivery

Handle scenarios where a downstream system may have received the file but RISOPAY did not receive the acknowledgement.

Do not blindly resend in ambiguous situations.

Provide reconciliation before retrying so duplicate payment submissions can be avoided.

## Delivery versus business acceptance

Keep downstream delivery state separate from downstream business acceptance.

Proposed delivery states:

- PENDING
- FAILED
- DELIVERED

Proposed downstream business processing states:

- PENDING
- ACCEPTED
- REJECTED

The exact meaning of RELEASED still requires a clearly documented state transition.

The intended direction is that RELEASED represents completion of the configured downstream delivery confirmation process. Do not assume that successful file receipt means every payment transaction was accepted.

---

# 13. TARGET AWS ARCHITECTURE

The platform should be AWS-native-ready without coupling the core processing engine to AWS services.

Proposed mapping:

| Capability | Target technology |
|---|---|
| Original and output file storage | Amazon S3 |
| Large-file staging | S3 / controlled temporary storage |
| Async job publishing | Amazon SQS |
| Long-running processing workers | ECS Fargate |
| Relational application data | Amazon RDS PostgreSQL |
| Monitoring and logs | Amazon CloudWatch |
| Secrets | AWS Secrets Manager |

ECS Fargate is a strong candidate for long-running large-file processing.

Do not force the 150K transaction processing workload into a Lambda-oriented design without evaluating execution limits and processing characteristics.

Introduce infrastructure abstractions where appropriate:

- PaymentFileStorage
- PaymentJobPublisher
- PaymentTransactionSink

Local development implementations should remain possible.

The core payment-processing logic should not require an AWS account to run locally.

---

# 14. DEVELOPMENT AND CHANGE MANAGEMENT RULES

These rules are mandatory for Codex.

1. Inspect the existing repository before editing.
2. Treat the current working tree as the source of truth.
3. Never replace the application with a new project or unrelated architecture.
4. Preserve existing working code unless a change is necessary.
5. Reuse existing classes and logic where appropriate.
6. Do not remove existing tests or fixtures just to make tests pass.
7. Do not silently change API endpoints, JSON DTO structures, identifier formats, business status names, or HTTP error behaviour.
8. Do not introduce large sets of dependencies without explaining why they are required.
9. Do not introduce database persistence before its architecture and requirements are agreed.
10. Do not implement the entire future architecture in one change.
11. Make small, logically isolated changes.
12. Explain each change and its impact.
13. Run relevant tests after each stage.
14. Run the full Maven test suite when appropriate.
15. Do not claim that tests passed unless they were actually executed successfully.
16. Identify existing failures separately from newly introduced failures.
17. Do not overwrite or discard the developer's uncommitted work.
18. Inspect Git status before changes.
19. Do not commit or push without explicit approval.
20. Update documentation when an architectural decision or implementation changes.

## Required implementation cycle

For every stage:

1. Inspect relevant existing code.
2. Explain the current implementation.
3. Explain the proposed change.
4. Identify files that will be modified or introduced.
5. Explain compatibility risks.
6. Make the smallest required changes.
7. Compile.
8. Run relevant tests.
9. Verify existing functionality.
10. Summarize changed files and test evidence.
11. Wait for approval before moving to the next stage.

Do not jump ahead.

---

# 15. IMMEDIATE IMPLEMENTATION ROADMAP

## Stage 1 — Bounded, replayable XML input

This is the agreed immediate task.

Existing blockers:

- XmlRequestSizeLimitFilter buffers the full accepted request in memory.
- PaymentController accepts the complete XML as a String.

Objective:

Replace this with a bounded, temporary-file-backed input abstraction.

Requirements:

- Keep the same POST endpoint.
- Keep the same content types.
- Preserve the 50 MiB request limit.
- Preserve HTTP 413 for oversized requests.
- Preserve HTTP 415 for unsupported content types.
- Count actual incoming bytes.
- Support chunked request enforcement.
- Avoid buffering the complete XML in heap.
- Make the source replayable.
- Ensure reliable temporary-file cleanup.
- Preserve existing error handling.
- Do not yet replace DOM extraction.
- Do not yet implement streaming XSD validation.
- Do not introduce database persistence.

Expected verification:

- Existing valid XML request still works.
- Existing invalid XML request still produces the expected error.
- Existing multiple-batch request still works.
- Unsupported content type still returns 415.
- Oversized Content-Length request still returns 413.
- Chunked oversized request is correctly rejected.
- Temporary files are cleaned up after success and failure.
- Existing tests continue to pass.

Only after Stage 1 is verified should Stage 2 begin.

## Stage 2 — Streaming XSD validation

Replace the current String-based validation path with a secure streaming validation path.

Keep the existing XSD registry and schema caching design where appropriate.

## Stage 3 — StAX streaming extraction

Replace DOM extraction with incremental StAX processing.

Retain the existing pain.001.001.03 business checks and output accuracy.

## Stage 4 — Transaction sink

Introduce PaymentTransactionSink and an in-memory test implementation.

Remove the requirement to retain every extracted transaction in one large List in the processing path.

## Stage 5 — Incremental batch processing

Adapt grouping while preserving PmtInf grouping and totals.

## Stage 6 — Canonical payment transaction model

Introduce the common transaction model and message-specific mapping boundary.

## Stage 7 — Persistent processing architecture

Design PostgreSQL entities and persistence for:

- Instructions.
- Transactions.
- Batches.
- Output instructions.
- Repair records.
- Audit events.
- Delivery and reconciliation state.

Do not implement this before the streaming foundation and data model are agreed.

## Stage 8 — Workflow and delivery

Introduce configurable repair, maker-checker, release, output generation, resend, and reconciliation workflows.

## Stage 9 — AWS integration

Add S3, SQS, ECS Fargate, RDS PostgreSQL, CloudWatch, and Secrets Manager adapters behind appropriate interfaces.

---

# 16. OPEN ARCHITECTURAL DECISIONS

Do not silently decide these:

- Exact SAX/ValidatorHandler versus StAX validation integration.
- Whether validation and extraction will use two passes or a combined event pipeline in a future iteration.
- JAXB versus Prowide versus custom message-specific mapping.
- Exact transaction staging and persistence strategy.
- Exact semantics and transition conditions for RELEASED.
- Exact delivery and downstream business acceptance state model.
- Customer configuration model for repair and release.
- Database schema and transaction uniqueness constraints.

Bring options, trade-offs, and a recommendation before implementation.

---

# 17. FINAL PRODUCT REQUIREMENTS

The architecture must remain:

- Extensible across ISO 20022 message types.
- Secure against XML external entity attacks.
- Suitable for large XML payment files.
- Incrementally processable.
- Auditable.
- Configurable by customer.
- Compatible with local development.
- AWS-native-ready.
- Safe against duplicate downstream payment submission.
- Capable of partial release.
- Capable of maker-checker repair.
- Maintainable and testable.

The immediate goal is NOT to implement every future feature.

The immediate goal is to evolve the existing working codebase into a secure, memory-efficient streaming foundation without breaking current functionality.

**Start with Stage 1 only.**