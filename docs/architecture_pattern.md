# Microservice Architecture & Coding Patterns Reference

> **Purpose:** This document captures reusable architecture, coding, persistence, messaging, configuration, security, and operational patterns identified from an existing production microservice.
>
> It is a **reference guide for implementing new microservices**.
>
> The objective is to copy the **architecture and engineering patterns**, not business logic or domain-specific implementations.
>
> The reference implementation should remain read-only when using this document.

---

# 1. Technology & Engineering Baseline

| Aspect                | Reference Pattern                                              |
| --------------------- | -------------------------------------------------------------- |
| Framework             | Spring Boot 3.x + Java 21                                      |
| Build                 | Maven Wrapper                                                  |
| Packaging             | WAR when required by deployment infrastructure                 |
| Configuration         | Centralized configuration + environment-specific configuration |
| Persistence           | Spring Data JPA                                                |
| Database migration    | Liquibase                                                      |
| Messaging             | Kafka + Spring Cloud Stream                                    |
| Distributed locking   | Redis / Redisson when required                                 |
| HTTP communication    | Centralized REST client                                        |
| Security              | OAuth2 Resource Server + JWT + method-level authorization      |
| Mapping               | MapStruct + Lombok                                             |
| Financial values      | `BigDecimal`                                                   |
| Sensitive data        | Encryption at rest                                             |
| Monitoring            | Actuator + HealthIndicator + monitoring solution               |
| API documentation     | springdoc OpenAPI                                              |
| Testing               | Unit + integration tests                                       |
| Dependency management | Shared internal libraries where available                      |

---

# 2. Package Architecture

Use **package-by-feature** at the application level with a shared `core` area for cross-cutting functionality.

```text
com.example.<service>
│
├── <Service>Application
│
├── consumer/
│   └── <MessageConsumer>
│
├── core/
│   ├── command/
│   ├── config/
│   ├── constant/
│   ├── entities/
│   │   └── sink/
│   ├── event/
│   ├── factory/
│   ├── mapper/
│   ├── model/
│   ├── redis/
│   ├── repository/
│   └── service/
│       └── impl/
│
├── <feature-a>/
├── <feature-b>/
├── security/
├── scheduler/
└── util/
```

### Principle

Features should own their business functionality.

Shared infrastructure belongs in `core`.

Avoid creating large global packages such as:

```text
controller/
service/
repository/
entity/
```

for the entire application unless there is a specific architectural reason.

---

# 3. Application Entry Point

The main application class should explicitly enable only the infrastructure required by the service.

Common examples:

```java
@SpringBootApplication
@EnableTransactionManagement
@EntityScan
@EnableJpaRepositories
@EnableScheduling
```

Do not enable unnecessary infrastructure.

---

# 4. Layered Service Pattern

Business functionality should follow a clear separation of responsibilities.

```text
Controller
    ↓
Service Interface
    ↓
Service Implementation
    ↓
Repository
    ↓
Database
```

### Controller

Responsible for:

* HTTP request/response
* Validation
* Authorization
* Calling services

### Service

Responsible for:

* Business rules
* Orchestration
* Transactions
* Calling repositories and external services

### Repository

Responsible for:

* Database access
* Queries
* Persistence

Controllers should not contain business logic.

---

# 5. Interface + Implementation Pattern

Use an interface for service contracts where the service may have multiple implementations, require mocking, or represent an important business abstraction.

```text
service/
├── ProcessingService.java
└── impl/
    └── ProcessingServiceImpl.java
```

Example:

```java
public interface ProcessingService {
    void process(Request request);
}
```

```java
@Service
@RequiredArgsConstructor
public class ProcessingServiceImpl implements ProcessingService {
    // business logic
}
```

---

# 6. Strategy + Factory Pattern

When the same operation can be performed using different implementations, use the **Strategy + Factory** pattern.

```text
                 Business Service
                        ↓
                     Factory
                        ↓
              ┌─────────┴─────────┐
              ↓                   ↓
         Strategy A          Strategy B
```

Implementations should share the same interface.

Use qualifiers when multiple Spring beans implement the same interface.

```java
@Qualifier("strategyA")
```

```java
@Qualifier("strategyB")
```

The factory should determine the implementation based on configuration, type, or runtime business rules.

### Avoid

```java
if (type.equals("A")) {
    // implementation A
} else if (type.equals("B")) {
    // implementation B
}
```

inside controllers or large business methods.

Adding a new strategy should ideally require an additive change rather than modifying a large conditional chain.

---

# 7. Utility Class Pattern

Stateless helper functionality may be placed in dedicated utility classes.

```text
util/
├── RequestModelUtil
├── ResponseModelUtil
└── ValidationUtil
```

Utilities should:

* Be stateless
* Have a single responsibility
* Not contain business state
* Not replace proper service abstractions

Do not put large business workflows inside utility classes.

---

# 8. DTO → Mapper → Entity Pattern

Do not expose JPA entities directly through APIs.

Use separate DTOs and persistence entities.

```text
Request DTO
    ↓
Mapper
    ↓
Entity
    ↓
Repository
    ↓
Entity
    ↓
Mapper
    ↓
Response DTO
```

Use MapStruct for repetitive mapping.

---

# 9. MapStruct Pattern

MapStruct mappers should be Spring-managed where dependency injection is required.

```java
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE
)
```

Abstract mapper classes can be used when the mapper needs injected dependencies such as:

* Repositories
* Encryption services
* Configuration
* Other Spring beans

Custom transformations should use named mapping methods.

```java
@Named("encryptField")
```

```java
@Mapping(
    target = "value",
    source = "value",
    qualifiedByName = "encryptField"
)
```

---

# 10. Persistence Pattern

Entities should follow consistent conventions.

### Base entity

Where appropriate, use a shared base entity:

```text
AbstractEntity
├── id
└── version
```

Common fields:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Version
private Long version;
```

The `@Version` field provides optimistic locking.

### Entity conventions

Prefer:

```java
@Entity
@Table(name = "explicit_table_name")
```

and:

```java
@Column(
    name = "explicit_column_name",
    nullable = false
)
```

Use explicit database names instead of relying on implicit naming.

---

# 11. Optimistic Locking

Use optimistic locking when concurrent updates to the same entity must be detected.

```java
@Version
private Long version;
```

This allows the persistence layer to detect conflicting updates.

Use it for entities where concurrent modification is a realistic business concern.

---

# 12. Repository Pattern

Repositories should use Spring Data abstractions.

```java
public interface ExampleRepository
        extends JpaRepository<ExampleEntity, Long> {
}
```

Use:

* Derived queries
* JPQL
* Projections

Use native SQL only when there is a clear technical requirement.

For atomic database operations, a native query may be appropriate.

---

# 13. Status / Reference Data Pattern

Avoid scattering magic strings throughout business logic.

Instead of:

```java
if (status.equals("ACTIVE")) {
}
```

prefer controlled constants and, where appropriate, database-backed reference data.

```text
Status
├── ACTIVE
├── INACTIVE
└── FAILED
```

For configurable/reference business values, use dedicated database tables and repositories.

---

# 14. Transaction Pattern

Use `@Transactional` around business operations that must execute atomically.

```text
@Transactional
Business Operation
    ↓
Validate
    ↓
Update
    ↓
Persist
```

Use:

```java
Propagation.REQUIRES_NEW
```

only when an operation genuinely requires an independent transaction boundary.

Do not add `REQUIRES_NEW` by default.

---

# 15. Concurrency Pattern

When processing multiple independent business units, isolate failures where possible.

For example:

```text
Batch
├── Item A → success
├── Item B → failure
├── Item C → success
└── Item D → success
```

A failure in one independent unit should not unnecessarily prevent other independent units from being processed.

### Important

Avoid blindly using `parallelStream()` inside transactional business methods.

Database transactions, persistence contexts, thread-local state, and parallel execution can interact poorly.

Prefer explicit concurrency mechanisms when parallel processing is actually required.

---

# 16. Distributed Lock Pattern

Use distributed locking only when multiple application instances can concurrently execute the same critical operation.

```text
Instance A ─┐
            ├── Distributed Lock ──→ Execute
Instance B ─┘
```

Redis/Redisson can be used for this purpose.

Example concept:

```text
lock:<aggregate-id>
```

The lock key should identify the business aggregate being protected.

Do not use distributed locking unless there is a real concurrency requirement.

---

# 17. Messaging Pattern

Use Kafka for asynchronous communication where appropriate.

```text
Producer
    ↓
Command / Event
    ↓
Kafka
    ↓
Consumer
    ↓
Business Service
```

Use typed message payloads rather than unstructured maps whenever possible.

---

# 18. Command / Event Envelope Pattern

Keep commands and events conceptually separate.

### Command

Represents a request to perform an action.

```text
"Perform this operation."
```

### Event

Represents something that already happened.

```text
"This operation happened."
```

Typed envelopes can contain:

```text
Message
├── messageId
├── binding/topic
├── metadata
└── typed payload
```

This makes message processing consistent and traceable.

---

# 19. Functional Kafka Consumer Pattern

Where Spring Cloud Stream is used, prefer functional consumers.

```java
@Bean
public Consumer<Command> processCommand() {
    return service::process;
}
```

This keeps messaging infrastructure separate from business logic.

---

# 20. Idempotent Consumer Pattern

Consumers must consider duplicate delivery.

Recommended flow:

```text
Receive Message
      ↓
Extract Message ID
      ↓
Already Processed?
   ↙          ↘
 YES           NO
  ↓             ↓
Ignore       Process
                ↓
        Mark Processed
```

A persistent message-log/idempotency table can be used when processing must remain idempotent across application restarts.

### Important

Mark the message as processed **after successful processing**, not before.

---

# 21. Defensive Message Deserialization

Never blindly trust incoming message payloads.

```text
Message
   ↓
Validate metadata
   ↓
Deserialize
   ↓
Optional / safe result
   ↓
Validate business data
   ↓
Process
```

Deserialization utilities may return:

```java
Optional<T>
```

rather than allowing malformed payloads to unexpectedly terminate consumers.

---

# 22. Cross-Service Read Model / Sink Pattern

When a service frequently requires data owned by another service, a local read model can be used.

```text
Source Service
      ↓
Event / Replication
      ↓
Local Sink Table
      ↓
Consumer Service
```

This avoids making synchronous HTTP calls for every lookup.

Example:

```text
core/
└── entities/
    └── sink/
```

Use this only when the benefits outweigh the complexity of maintaining replicated data.

---

# 23. Configuration Pattern

Configuration should be externalized.

```text
Application
    ↓
Configuration Properties
    ↓
Environment / Config Server
```

Prefer `@ConfigurationProperties` for structured configuration.

```java
@ConfigurationProperties(prefix = "tokenization")
public class TokenizationProperties {
}
```

For many flat properties, a centralized property holder may be used.

### Principle

Configuration should be:

* Externalized
* Typed where possible
* Environment-specific
* Centralized
* Easy to inspect

---

# 24. Secrets Management

Secrets must never be hardcoded in source code.

```text
Development
    ↓
Environment Variables
    ↓
Centralized Configuration
    ↓
Secrets Manager / Vault
```

Examples:

* Encryption keys
* API credentials
* Database passwords
* OAuth client secrets
* Signing keys

Never commit real credentials to Git.

---

# 25. Sensitive Data Protection

Sensitive data should be protected at rest.

```text
Sensitive Input
      ↓
Validation
      ↓
Encryption / Tokenization
      ↓
Database
```

MapStruct can perform encryption during persistence mapping when appropriate.

Sensitive values must also be excluded or masked from:

* Logs
* Exceptions
* Debug output
* API responses
* Kafka messages

unless explicitly required.

---

# 26. Money & Numeric Precision

For monetary values:

```java
BigDecimal
```

must be preferred over:

```java
double
float
```

Use dedicated rounding services when rounding rules are business-specific.

```text
Business Calculation
        ↓
Rounding Service
        ↓
Final Amount
```

Do not scatter rounding logic throughout business services.

---

# 27. Error Handling Pattern

Use centralized error handling.

```text
Exception
    ↓
Centralized Exception Handler
    ↓
Standard Error Response
    ↓
Client
```

Separate:

* Business exceptions
* Validation exceptions
* Not-found exceptions
* Conflict exceptions
* Technical exceptions
* External-service failures

Do not expose internal stack traces through APIs.

---

# 28. Response Envelope Pattern

APIs should use a consistent response structure.

Example:

```text
Response
├── status
├── message
├── data
└── error
```

The exact structure should follow the organization's established API standard.

Controllers should not return completely different response formats for similar operations.

---

# 29. External REST Client Pattern

External service communication should be centralized through a reusable client abstraction.

```text
Business Service
      ↓
REST Client Wrapper
      ↓
Authentication
      ↓
HTTP Request
      ↓
External Service
```

Benefits:

* Consistent authentication
* Consistent error handling
* Consistent timeout handling
* Centralized logging
* Less duplicated HTTP code

---

# 30. External-Service Error Pattern

Do not make business logic depend directly on raw HTTP status codes.

Convert external failures into typed business/technical results.

```text
External API
     ↓
REST Client
     ↓
Typed Result
     ↓
Business Logic
```

Possible outcomes:

```text
SUCCESS
FAILURE
TIMEOUT
AMBIGUOUS
UNAUTHORIZED
INSUFFICIENT_RESOURCE
```

The exact result types depend on the business domain.

---

# 31. JSON / Jackson Pattern

Use one primary `ObjectMapper` configuration for the application.

Typical configuration includes:

```text
JavaTimeModule
FAIL_ON_UNKNOWN_PROPERTIES = false
```

Kafka-specific payload conversion can use a dedicated utility that safely converts JSON into typed objects.

Do not create multiple unrelated `ObjectMapper` configurations without a reason.

---

# 32. Scheduler Pattern

Scheduling should only be introduced when the business requirement actually needs it.

If schedules need to change without redeployment, use database-driven configuration:

```text
Database
   ↓
Scheduler Configuration
   ↓
Scheduler
   ↓
Business Service
```

Avoid adding scheduler infrastructure to services that do not require scheduled processing.

---

# 33. Security Pattern

Security should be introduced according to the service's actual requirements.

For protected services:

```text
Request
   ↓
JWT Validation
   ↓
Authentication
   ↓
Scope / Permission Check
   ↓
Controller
```

Use method-level authorization when appropriate.

For local development, a controlled security toggle may be used if permitted by the project's security standards.

Security should never be disabled accidentally in production.

---

# 34. Observability Pattern

Services should provide:

* Actuator
* Health checks
* Application metrics
* Structured logging
* Dependency health
* Request/business correlation
* Execution timing

Example:

```text
Request
 ↓
Correlation ID
 ↓
Business Operation
 ↓
External Calls
 ↓
Database
 ↓
Response
```

Sensitive information must never be included in logs.

---

# 35. API Documentation

Use OpenAPI/springdoc for service APIs.

Document:

* Endpoints
* Request models
* Response models
* Authentication
* Required scopes
* Error responses

The API documentation should reflect the actual implementation.

---

# 36. Testing Standard

Tests are mandatory for new business functionality.

At minimum, test:

```text
Controller
Service
Repository where required
Mapper where complex
Message Consumer
Critical business flows
Failure scenarios
```

The build should remain green.

```text
Code Change
    ↓
Tests
    ↓
mvn test
    ↓
Build
```

Do not leave tests permanently disabled or commented out.

---

# 37. Code Quality Rules

Avoid:

```text
❌ Dead code
❌ Commented-out production code
❌ printStackTrace()
❌ Swallowed exceptions
❌ Broad catch(Exception) without handling
❌ Business logic in controllers
❌ Business logic in utility classes
❌ Hardcoded secrets
❌ Magic strings
❌ Unnecessary distributed locks
❌ Unnecessary schedulers
❌ Unnecessary parallel processing
```

Prefer:

```text
✅ Typed exceptions
✅ Centralized error handling
✅ Clear interfaces
✅ Small focused services
✅ Reusable infrastructure
✅ Explicit configuration
✅ Defensive message processing
✅ Tests for business behaviour
```

---

# 38. What to Copy vs What Not to Copy

## Copy the pattern

```text
Package structure
Service abstraction
Strategy/factory structure
DTO/mapper separation
Transaction boundaries
Idempotency
Messaging conventions
Configuration approach
Error handling
Logging
Observability
Testing approach
Database migration approach
```

## Do not copy blindly

```text
Business rules
Domain names
Database table names
External service names
Bank/product-specific logic
Unused dependencies
Unused infrastructure
Unnecessary schedulers
Unnecessary locks
Known anti-patterns
```

The goal is **architectural consistency without unnecessary coupling**.

---

# 39. Pattern Adoption Rule

Before implementing a new component, ask:

1. Does an established pattern already solve this problem?
2. Does the existing shared infrastructure already provide this functionality?
3. Can the pattern be reused without copying domain-specific logic?
4. Is the pattern actually required by the new service?
5. Is there a known limitation or anti-pattern in the reference implementation?
6. If a different approach is chosen, is there a clear technical reason?

A new pattern should not be introduced merely because another implementation style is available.

---

# 40. Standard Development Flow

For a new microservice:

```text
1. Establish package-by-feature structure
              ↓
2. Establish shared core
              ↓
3. Configure application infrastructure
              ↓
4. Configure database + Liquibase if required
              ↓
5. Configure external integrations
              ↓
6. Implement feature
              ↓
7. Controller → Service → Repository
              ↓
8. Add DTOs + MapStruct
              ↓
9. Add transactions where required
              ↓
10. Add messaging/idempotency where required
              ↓
11. Add security where required
              ↓
12. Add observability
              ↓
13. Add tests
              ↓
14. Validate against this reference
```

---

# 41. Golden Rule

> **Copy the architecture and engineering patterns — never blindly copy the business implementation.**

Every new service should follow the established patterns where they are relevant.

When a pattern is not required, **do not introduce it just for consistency**.

When a new requirement requires a different pattern:

1. Explain the technical reason.
2. Keep the implementation consistent with the overall architecture.
3. Document the deviation.
4. Consider whether the new pattern should become a future standard.

This document should remain a **generic architecture and coding reference** that can be applied to any new microservice without depending on a specific project, company, package, or business domain.
