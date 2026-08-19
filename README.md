# Loan Tokenization Project

A small microservice-based loan tokenization system built in phases.

> Implement dynamic tokenization within micro-services.

```
loan-tokenization-project/
├── tokenization-service/   # Spring Boot 3.x / Java 21 / Maven
└── docs/
    ├── phase-1.md          # Phase 1 documentation (skeleton + validation)
    └── phase-2.md          # Phase 2 documentation (FF1 format-preserving tokenization)
```

## Status

- **Phase 1 (done)** — project skeleton + Tokenization Service with
  `POST /api/tokenize`, MOBILE validation, global error handling and tests.
- **Phase 2 (done)** — format-preserving tokenization via **FF1** (NIST
  SP 800-38G) using **BouncyCastle** (`bcprov-jdk18on:1.85.2`). The
  `POST /api/tokenize` endpoint now returns a real 10-digit token; the engine
  also exposes reversible `detokenize`. Key/tweak live in temporary dev config
  (to be moved to HashiCorp Vault later). See [docs/phase-2.md](docs/phase-2.md).
- Later phases — dynamic tokenization, HashiCorp Vault + Vault Transit, MySQL
  storage, controlled detokenization, microservice communication and Docker
  Compose (not implemented yet).

## Quick start

```bash
cd tokenization-service
mvn spring-boot:run
```

```bash
curl -X POST http://localhost:8080/api/tokenize \
  -H "Content-Type: application/json" \
  -d '{"value": "9841234567", "type": "MOBILE"}'
```

Example response (the actual token depends on the configured key/tweak):

```json
{"token": "4450187392"}
```

See [docs/phase-1.md](docs/phase-1.md) for full details.
