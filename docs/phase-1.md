# Phase 1 — Project Setup + Tokenization Service Skeleton

## 1. What this phase implements

Phase 1 creates the initial skeleton of the Tokenization Service, the first
microservice of the loan tokenization system. It delivers:

- A **Maven + Spring Boot 3.x** project (Java 21) with a clean layered layout:
  `controller/`, `service/`, `dto/`, `exception/`, `config/`.
- A single REST endpoint, `POST /api/tokenize`, that accepts a value and a data
  type, validates the input, and returns a **placeholder response** that clearly
  states the tokenization engine is not implemented yet. No fake token is
  generated.
- **Input validation** for `MOBILE` values (exactly 10 digits, digits only).
- A **global exception handler** that returns a consistent JSON error body for
  invalid requests, validation failures, unsupported data types and unexpected
  errors.
- **Automated tests** for the controller (MockMvc) and the service (Mockito).

The project intentionally does **not** implement real tokenization, encryption,
storage, detokenization, Vault, or any other service yet. Those belong to later
phases.

## 2. What tokenization is

Tokenization is a technique for protecting sensitive data by replacing it with a
non-sensitive, meaningless surrogate value called a **token**.

The original sensitive value (for example a mobile number or a card number) is
stored in a secure, protected environment (a token vault). The token itself has
no exploitable meaning on its own — an attacker who obtains a token learns
nothing about the real value. Applications that need the real value call a
controlled **detokenization** process that maps the token back to the original
value using the vault.

The key properties of tokenization are:

- The token is a **substitute**, not an encryption of the original.
- The mapping between token and original value is kept in a secure store.
- Detokenization is **controlled**: only authorized callers can reverse a token.

In a loan system, tokenizing mobile numbers, bank accounts or national IDs
reduces the exposure of personal data in databases, logs, and API payloads.

## 3. What format-preserving tokenization means

**Format-preserving tokenization** (FPE) produces tokens that keep the same
**format** as the original value: the same length and the same character set.

For example, a 10-digit mobile number `9841234567` is tokenized into another
10-digit number such as `4450187392`. The token:

- looks and behaves like the original (same type, same length, same character
  set), so it can be stored in the same database columns and passed to systems
  that expect a mobile number, without schema or contract changes;
- is generated with a reversible, keyed transformation (for example the
  **FF1** / **FFX** algorithms), so it can be deterministically mapped back to
  the original value by authorized parties.

This contrasts with **dynamic tokenization**, where tokens are random and may
have any format.

## 4. Why MOBILE validation requires exactly 10 digits

In Phase 1 the mobile number format is restricted to **exactly 10 digits and
digits only**. The reasons:

- **Ten-digit numbering plan**: the mobile numbers used as examples follow a
  10-digit national numbering plan (e.g. a 10-digit subscriber number starting
  with a carrier prefix). This is the most common length for domestic mobile
  numbers in many countries.
- **Determinism for future FPE**: format-preserving tokenization requires a
  well-defined input domain (length + character set). Enforcing exactly 10
  digits gives FPE a fixed, unambiguous input format to preserve.
- **Consistent storage and comparison**: a canonical format avoids the same
  number being stored in different shapes (with/without `+`, dashes, spaces,
  leading zeros), which would break token lookup and detokenization later.
- **Simple, safe start**: country-specific rules, country codes, leading `+`,
  or variable-length numbers are deliberately **not** implemented in Phase 1.
  They can be added in later phases per data type without changing the API.

## 5. Current API

### `POST /api/tokenize`

Request:

```json
{
  "value": "9841234567",
  "type": "MOBILE"
}
```

Successful response (`200 OK`):

```json
{
  "message": "Tokenization engine not implemented in Phase 1"
}
```

Validation rules for `MOBILE`:

| Input           | Result |
|-----------------|--------|
| `9841234567`    | valid   |
| `98412345`      | invalid (too short) |
| `98412345678`   | invalid (too long) |
| `98A1234567`    | invalid (letters) |
| `98-1234567`    | invalid (special characters) |
| `""`            | invalid (empty) |
| `null`          | invalid (missing) |

Error response (`400 Bad Request`), used for invalid requests, validation
failures, and unsupported data types:

```json
{
  "timestamp": "2026-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "value must contain exactly 10 digits and digits only",
  "path": "/api/tokenize"
}
```

Example error messages:

- `value must not be blank` — `value` is missing, `null`, empty or whitespace.
- `value must contain exactly 10 digits and digits only` — format violation.
- `Unsupported data type: BANK. Supported types: [MOBILE]` — the `type` is not
  supported.

Unexpected errors return `500 Internal Server Error` with the same JSON shape.

## 6. Current limitations

- **No real tokenization**: the service returns a fixed placeholder message; it
  never generates or stores a token.
- **No encryption**: values are not encrypted at rest or in transit beyond
  standard TLS.
- **No storage**: there is no token vault / database (MySQL is planned for a
  later phase).
- **No detokenization**: there is no endpoint to reverse a token.
- **MOBILE only**: the only supported data type is `MOBILE`; other types are
  rejected with `400`.
- **No country-specific mobile validation**: any 10-digit value is accepted.
- **No external infrastructure**: no PostgreSQL, no HashiCorp Vault, no Vault
  Transit, no Docker Compose, and no other microservices.

## 7. What will be implemented in Phase 2

Phase 2 will replace the placeholder with real tokenization:

- **Dynamic tokenization** with random, vault-managed tokens.
- **Format-preserving tokenization (FPE)** using the **FF1** algorithm so tokens
  keep the exact format of the original value.
- **HashiCorp Vault + Vault Transit** for key management and encryption
  operations.
- **MySQL** storage for the token-to-value mapping (token + encrypted value).
- **Controlled detokenization** with authorization checks.
- **Microservice communication**: Loan Service, Accrual Service and Notification
  Service consuming the tokenization service.
- **Docker Compose** for local infrastructure and automated integration tests.

## 8. How to run the service

Prerequisites: JDK 21 and Maven 3.9+.

From the `tokenization-service` directory:

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080` (configurable via
`server.port` in `src/main/resources/application.yml`).

Try it:

```bash
curl -X POST http://localhost:8080/api/tokenize \
  -H "Content-Type: application/json" \
  -d '{"value": "9841234567", "type": "MOBILE"}'
```

Expected response:

```json
{"message": "Tokenization engine not implemented in Phase 1"}
```

Or build an executable jar:

```bash
mvn clean package
java -jar target/tokenization-service-0.0.1-SNAPSHOT.jar
```

## 9. How to run tests

From the `tokenization-service` directory:

```bash
mvn test
```

This runs all unit tests:

- `TokenizationControllerTest` — MockMvc web-slice tests covering a valid
  request, missing value, invalid mobile formats, unsupported type and
  malformed JSON.
- `TokenizationServiceTest` — Mockito-based unit tests covering valid input
  reaching the tokenization logic and invalid input (null/blank values,
  wrong length, letters, special characters, null/blank type, unsupported
  type) being rejected.

A full build including tests:

```bash
mvn verify
```

