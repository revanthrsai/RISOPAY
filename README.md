# RISOPAY

**RISOPAY by ISO 20022 Academy**  
A learning-focused payment-processing platform built around ISO 20022 message handling.

RISOPAY is being developed as a backend-first project. The current implementation accepts
`pain.001.001.03` customer credit transfer initiation messages, validates the XML against the
registered XSD, extracts payment data, groups transactions into logical batches, and returns
a structured JSON response.

## Current capabilities

- Detects the ISO 20022 message namespace.
- Resolves the supported message definition through a registry.
- Validates incoming XML against the `pain.001.001.03` XSD.
- Extracts message, payment-information, and transaction details.
- Checks declared transaction counts and control sums against extracted values.
- Groups transactions by unique `PmtInfId`. A single-transaction instruction does not create a batch.
- Avoids duplicating transactions at the top level when batch responses are present.
- Generates RISOPAY identifiers with consistent fixed-length formats.

## Identifier formats

All RISOPAY identifiers are 15 characters long:

| Entity | Prefix | Suffix | Example |
|---|---|---:|---|
| Payment instruction | `RISOINS` | 8 characters | `RISOINS7A2B9C4D` |
| Payment batch | `RISOBAT` | 8 characters | `RISOBAT8F3D1A6C` |
| Transaction | `RISOTXN` | 8 characters | `RISOTXN12AB34CD` |

The suffix uses uppercase letters and digits. The generator uses `SecureRandom`.
When persistent storage is introduced, enforce a unique constraint on each identifier and
retry generation if a collision is detected.

## Technology

- Java 21
- Spring Boot
- Maven
- Spring Web MVC
- Spring Boot Actuator

## Run locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

Health endpoints:

- `GET http://localhost:8080/api/v1/health`
- `GET http://localhost:8080/actuator/health`

## Submit a payment instruction

Send an ISO 20022 `pain.001.001.03` XML document:

```http
POST http://localhost:8080/api/v1/payment-instructions
Content-Type: application/xml
Accept: application/json
```

The current API scope is intentionally limited to the initial `pain.001.001.03` intake flow.
Further payment rails, message types, persistence, asynchronous processing, and UI capabilities
will be introduced in later phases.

## Product positioning

RISOPAY is the payment-processing product within the ISO 20022 Academy ecosystem.
The Academy remains the learning and education platform; RISOPAY is the separate processing
application. The two may share versioned ISO 20022 engine components in the future.
