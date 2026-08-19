# Phase 2 — Format-Preserving Tokenization (FF1)

## 1. What FPE is

**Format-preserving encryption (FPE)** is a cryptographic technique that
encrypts a plaintext value into a ciphertext which has the **same format** as
the plaintext: the same length and drawn from the same character set.

For example, a 10-digit mobile number `9841234567` is encrypted into another
10-digit number such as `4450187392`. The output *looks like* a mobile number,
so it can be stored in the same database column, logged in the same payloads and
passed to systems that expect a mobile number, without any schema or contract
changes — while the real value stays hidden.

FPE is a **keyed permutation** over the input domain. Given a fixed key:

- the mapping is **deterministic**: the same value always produces the same token;
- the mapping is **reversible**: the original value can be recovered with the
  same key by running the algorithm in reverse (detokenization);
- the mapping is a **bijection**: distinct values always map to distinct tokens,
  so thousands of customers always get different tokens.

The FPE approach used here is defined by **NIST Special Publication 800-38G**,
"Recommendation for Block Cipher Modes of Operation: Methods for Format-
Preserving Encryption".

## 2. Why FF1 is appropriate

**FF1** is one of the two approved FPE modes in NIST SP 800-38G (the other is
FF3-1). It was chosen because:

- **Standards-based**: FF1 is a NIST-approved, widely analyzed mode of
  operation, not an ad-hoc scheme.
- **Arbitrary input length**: FF1 accepts values from 2 up to very large
  lengths, which comfortably covers 10-digit mobile numbers (and future data
  types such as card numbers or national IDs).
- **Arbitrary alphabet/radix**: FF1 works over any alphabet; a radix-10 alphabet
  (digits 0–9) matches the MOBILE format exactly.
- **Variable tweak**: FF1 supports an optional tweak, which can vary per
  application/tenant/context to add domain separation.
- **Reversible by construction**: detokenization is the FF1 inverse, so no
  database lookup table is required to recover values (the spec explicitly
  forbids database storage in this phase).
- **Permutation property**: because FF1 is a bijection on its domain, two
  different mobile numbers can never collide on the same token.

The example output in the phase requirements (`XXXXXXXXXX`, `6702345678`, …)
is **illustrative only**; this implementation does not promise any specific
token value. With the development key in place, every valid 10-digit input
produces exactly one deterministic 10-digit token.

## 3. Difference between normal encryption and FPE

| Aspect | Normal encryption (e.g. AES-CTR/GCM) | FPE (FF1) |
|--------|--------------------------------------|-----------|
| Output format | Arbitrary binary bytes / hex / base64; length differs from input | Same format as input (length + character set preserved) |
| Length | Ciphertext is typically longer (block boundaries, IV/tag) | Identical length to plaintext |
| Storage impact | Needs new columns / encoding; breaks consumers that expect a phone number | Drops into existing columns and contracts unchanged |
| Domain | Arbitrary byte strings | Any alphabet/radix (here: digits 0–9) |
| Standard | Well-known modes (AES) | NIST SP 800-38G (FF1/FF3-1) |

Normal encryption is a good general-purpose choice, but it breaks *format*. The
whole point of the loan tokenization system is that a mobile number must be
replaceable *in place* — the token has to look like a mobile number.

## 4. Why the token remains 10 digits

FF1 encrypts within a fixed alphabet. The MOBILE alphabet is the 10 decimal
digits, and the input domain is fixed at exactly 10 digits. FF1's output is a
member of the *same* domain, so:

- the token always has exactly **10 characters**;
- the token always contains **digits only**;
- the token length equals the input length by construction.

Keeping the format identical is what makes format-preserving tokenization
valuable: downstream systems, database columns (`CHAR(10)`), validation logic
and display layers keep working with zero changes.
## 5. Why custom cryptography was avoided

Writing your own crypto is dangerous:

- **Subtle implementation bugs**: FF1 is intricate (10 Feistel rounds, PRF
  construction, base conversions). A single off-by-one or wrong byte ordering
  can silently break security or interoperability.
- **No review/analysis**: bespoke algorithms have no third-party cryptanalysis.
- **Standard interop**: only a tested implementation of the published standard
  can claim NIST SP 800-38G compliance and interop with other tools.
- **Maintenance**: standards evolve (e.g. FF3 was deprecated and replaced by
  FF3-1); a maintained library tracks these changes.

Therefore the FF1 algorithm is taken verbatim from a reputable, actively
maintained Java cryptographic library rather than being reimplemented.

## 6. Selected Java library

**BouncyCastle** — `org.bouncycastle:bcprov-jdk18on:1.85.2`.

- Why BouncyCastle:
  - Reputable, widely deployed, **actively maintained** (frequent releases;
    1.85.2 is the latest release line at the time of writing).
  - Ships a direct implementation of NIST SP 800-38G FF1 (and FF3-1).
  - Available on Maven Central; no extra repository configuration.
- Where it is declared: `tokenization-service/pom.xml`.
- What it provides in this phase:
  - `org.bouncycastle.crypto.fpe.FPEFF1Engine` — the FF1 engine.
  - `org.bouncycastle.crypto.params.FPEParameters` — key + radix + tweak.
  - `org.bouncycastle.crypto.params.KeyParameter` — the AES key.
  - `org.bouncycastle.util.encoders.Hex` — tweak decoding.
- API note: BouncyCastle ≥ 1.85 replaced the older `FF1FPEEncryptorEngine` /
  `BasicAlphabet` API with the `FPEFF1Engine` + `RadixConverter` API used here.

The engine is hidden behind a small interface,
`com.loan.tokenization.service.TokenizationEngine`
(`tokenize(value, dataType)` / `detokenize(token, dataType)`), so the
cryptographic implementation can be swapped later without touching the
controller or DTOs.

## 7. Key/tweak/domain configuration

FF1 parameters used by `FF1TokenizationEngine`:

| Parameter | Value / source | Notes |
|-----------|----------------|-------|
| Algorithm | FF1 (NIST SP 800-38G) | BouncyCastle `FPEFF1Engine` |
| Base cipher | AES (AES-256) | 32-byte key |
| Alphabet / radix | Decimal digits, radix 10 | `0-9`, one byte per digit |
| Domain | Exactly 10 digits | enforced before processing |
| Key | `app.tokenization.ff1.key-base64` (Base64) | **TEMPORARY dev-only value** |
| Tweak | `app.tokenization.ff1.tweak-hex` (hex) | **TEMPORARY dev-only value**; optional, empty allowed |

The key and tweak are **not hard-coded in Java source**. They are read from
`application.yml` via `TokenizationProperties`
(`@ConfigurationProperties(prefix = "app.tokenization")`) and injected into
`FF1TokenizationEngine` at startup.

```yaml
app:
  tokenization:
    ff1:
      key-base64: "<32-byte AES-256 key>"
      tweak-hex: "f83524b7eb52a221"
```

> **TEMPORARY DEVELOPMENT CONFIGURATION**: these values exist for local
> development only and are clearly marked in `application.yml` and
> `TokenizationProperties`. They will be moved to HashiCorp Vault (Vault
> Transit) in a later phase. Nothing here is suitable for production.

A fresh random key/tweak were generated for this phase; both are committed as
dev-only placeholders and are deliberately documented as temporary.
## 8. Test cases

All tests run via `mvn test` (38 tests in total).

`FF1TokenizationEngineTest` (17 tests) covers the engine directly:

- **Format**
  - `token_hasSameLengthAsInput` — output length equals input length (10).
  - `token_containsDigitsOnly` — output is digits only.
- **Determinism**
  - `sameInput_alwaysProducesSameToken` — identical input/config → identical token.
- **Different values**
  - `differentInputs_produceDifferentTokens` — different mobile numbers → different
    tokens (guaranteed by FF1's permutation property).
- **Reversibility**
  - `detokenize_returnsOriginalValue` (parameterized over 5 values) —
    `detokenize(tokenize(v)) == v`.
- **Invalid values** (all rejected with `DataValidationException`)
  - `nullValue_isRejected`, `emptyValue_isRejected`
  - `valueTooShort_isRejected`, `valueTooLong_isRejected`
  - `valueWithLetters_isRejected`, `valueWithSpecialCharacters_isRejected`
  - `detokenize_rejectsInvalidToken`
- **Unsupported type** — `nullDataType_isRejected` (`UnsupportedDataTypeException`).

`TokenizationServiceTest` (11 tests) verifies the service validates requests and
delegates to the engine (engine mocked), and that invalid input never reaches
the engine.

`TokenizationControllerTest` (10 tests) verifies the HTTP endpoint: valid request
returns a `token`, missing/null/invalid values and unsupported types return
`400`, malformed JSON returns `400`.

## 9. Security limitations

This phase is a functional engine, **not** a production security solution:

- **Key at rest in config**: the AES key and tweak live in `application.yml` in
  plaintext (Base64/hex). Anyone with repository access can read them.
- **No key rotation / versioning**: the key is fixed at startup; rotation is not
  supported.
- **No central secret management**: no Vault, no HSM, no KMS.
- **No access control / audit on detokenize**: `detokenize` is a plain method on
  the engine; there is no authorization or audit trail. (No detokenization HTTP
  endpoint is exposed in this phase.)
- **Determinism enables frequency analysis**: identical plaintext values always
  map to identical tokens, so an attacker who knows the distribution of inputs
  (e.g. mobile prefixes) can guess values by pattern. This is inherent to
  deterministic FPE and is why tokens must later be paired with a secure token
  vault and encrypted-value storage.
- **No token vault mapping**: nothing is stored; if the key is compromised, all
  tokens can be reversed directly.
- **Single shared key**: all data types/tenants share one key and tweak; there
  is no per-tenant separation.
- **Dev-only defaults**: the committed key/tweak are placeholders and must not
  be used outside local development.

## 10. Why the key must later be moved to Vault

- **Centralized secrets management**: secrets should live in a dedicated store,
  not in source control. Vault provides encryption, access control, and audit
  logging for secrets at rest and in transit.
- **Rotation and versioning**: Vault supports key versioning and rotation so a
  compromised key can be replaced without redeploying services.
- **Least privilege**: Vault policies can restrict which service/identity may
  read the FF1 key, and Vault Transit can even perform cryptographic operations
  server-side so the raw key never reaches the application.
- **Auditability**: every key access can be logged and reviewed.
- **Operational safety**: no secrets in git history, CI logs, or packaged
  artifacts — a plaintext key in `application.yml` would leak to anyone with
  repo or artifact access.

In a later phase the temporary development key will be removed from
`application.yml` and replaced with a HashiCorp Vault (Vault Transit) integration.

