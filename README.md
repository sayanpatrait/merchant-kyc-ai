# Merchant KYC AI

Spring Boot + Spring AI starter project for merchant KYC operations.

## What it does

- Stores merchant KYC data in H2.
- Tracks PAN, Aadhaar and GST verification status.
- Exposes REST APIs for merchant creation, lookup and verification.
- Provides an AI chat endpoint.
- Gives the AI tools to:
  - report KYC statistics;
  - verify one merchant;
  - verify all remaining pending KYC checks.
- Uses a demo KYC provider so the project can run without real vendor credentials.

## Important production note

The provider in this starter is deliberately a mock. Replace `MockKycProvider` with adapters for your approved PAN, Aadhaar and GST verification vendors.

For Aadhaar, use an authorized/legally compliant verification flow. Do not send Aadhaar data to arbitrary APIs. Add encryption, access controls, audit logging, masking, consent/legal checks, retention rules and secrets management before production.

## Run

Requirements:
- Java 21
- Maven 3.9+

Set an OpenAI API key:

Linux/macOS:
`export OPENAI_API_KEY=your_key`

Windows PowerShell:
`$env:OPENAI_API_KEY="your_key"`

Then:

`mvn spring-boot:run`

The API starts on `http://localhost:8080`.

## Create a merchant

POST `/api/merchants`

Example body:

```json
{
  "merchantName": "ABC Retail Pvt Ltd",
  "email": "ops@example.com",
  "phone": "9999999999",
  "pan": "ABCDE1234F",
  "aadhaar": "masked-or-tokenized-value",
  "gstin": "27ABCDE1234F1Z5"
}
```

## AI chat

POST `/api/chat`

```json
{
  "message": "How many KYC checks are pending?"
}
```

Explicit action:

```json
{
  "message": "Verify all the remaining KYC"
}
```

The model can call the verification tool only when the user explicitly requests the action.

## REST endpoints

- GET `/api/merchants`
- GET `/api/merchants/{id}`
- POST `/api/merchants`
- POST `/api/merchants/{id}/verify`
- GET `/api/merchants/summary`
- POST `/api/merchants/verify-remaining`
- POST `/api/chat`

## Recommended production architecture

Web/UI -> AI Chat Controller -> Spring AI ChatClient -> KYC tools -> KYC orchestration service -> provider adapters -> PAN/Aadhaar/GST vendors.

For bulk verification, replace the simple async loop with a durable queue such as Kafka/RabbitMQ or Spring Batch, with rate limits, retries, idempotency keys, dead-letter handling and an audit table.

The next useful step is to add a real KYC provider adapter layer such as:
`PanVerificationClient`, `AadhaarVerificationClient`, `GstVerificationClient`, each using WebClient and provider-specific request/response DTOs.
