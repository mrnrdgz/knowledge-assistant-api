# MAR-KA-007 — Connect document upload to ingestion pipeline

**Status:** Implementation and verification completed; Git/Trello closure pending  
**Trello:** MAR-KA-007  
**Started:** 2026-10-20  
**Last updated:** 2026-11-02

## Goal

Connect `POST /documents` to the existing document processing pipeline so that a valid PDF upload triggers asynchronous processing, and its chunks and embeddings are persisted in PostgreSQL with pgvector.

## Delivered behavior

- `POST /documents` accepts a PDF, registers a document and returns HTTP `202 Accepted` with its persisted `documentId`, `fileName` and initial `ACCEPTED` status.
- The upload content is copied to `byte[]` before background processing begins.
- Spring `@Async` executes the existing extraction, chunking, embedding and persistence pipeline without making the HTTP request wait for completion.
- The document can transition to `PROCESSING`, then `PROCESSED` or `FAILED` as appropriate.
- A real PDF was processed and its chunks and embeddings were verified in PostgreSQL.

## Analysis — Six questions

### 1. Objective

Connect the existing HTTP upload endpoint to the complete ingestion pipeline without duplicating processing or persistence logic.

### 2. Inputs and outputs

- **Input:** PDF uploaded as `MultipartFile`.
- **Immediate HTTP output:** `202 Accepted`, with `documentId`, `fileName`, and `ACCEPTED`.
- **Background result:** Persisted document, chunks and embeddings; eventual `PROCESSED` or `FAILED` when application-level processing completes or fails.
- **Deferred output:** Status query endpoint, planned for MAR-KA-008.

### 3. Processing flow

1. `DocumentController` receives the PDF upload.
2. `DocumentIngestionService` calls `DocumentService.acceptDocument(file)` to validate and register it.
3. The ingestion service reads the upload into `byte[]` before the request ends.
4. `DocumentAsyncProcessor.processDocumentAsync(documentId, pdfBytes)` starts background processing.
5. `DocumentService.processAndPersistDocument()` retrieves the document and marks it `PROCESSING`.
6. The existing PDF extraction, text chunking and embedding generation logic runs.
7. `DocumentPersistenceService` persists the chunks and updates the successful final status transactionally.
8. Processing exceptions within the guarded pipeline are handled by recording `FAILED` with a nonblank error message, then propagating the original exception.

### 4. Components and responsibilities

| Component | Responsibility |
|---|---|
| `DocumentController` | HTTP upload endpoint and response contract |
| `DocumentIngestionService` | Register document, copy PDF bytes, dispatch background processing |
| `DocumentAsyncProcessor` | Delegate processing asynchronously via Spring `@Async` |
| `DocumentService` | Document acceptance, processing orchestration, extraction, chunking and embedding generation |
| `DocumentPersistenceService` | Transactional chunk persistence and failure-state persistence |
| `DocumentRepository`, `DocumentChunkRepository` | JPA data access |
| `Document` | Enforce allowed status transitions and error-message rules |

**Key contracts:**

- `DocumentAcceptanceResult ingestDocument(MultipartFile file)`
- `void processDocumentAsync(Long documentId, byte[] pdfBytes)`
- `void processAndPersistDocument(Long documentId, byte[] pdfBytes)`

**Design constraints:**

- Do not pass request-bound `MultipartFile` or JPA entities to background execution.
- Reuse existing processing and persistence logic.
- Avoid circular dependencies and long-running Bedrock calls inside database transactions.

### 5. Implementation tasks

- [x] Return the persisted document ID and preserve HTTP `202 Accepted`.
- [x] Select asynchronous processing as the execution model.
- [x] Implement ingestion orchestration using `DocumentIngestionService`.
- [x] Copy PDF content into `byte[]` before asynchronous dispatch.
- [x] Handle PDF byte-reading failures using the existing failure-state persistence behavior.
- [x] Implement `DocumentAsyncProcessor` with `@Async` and enable asynchronous execution with `@EnableAsync`.
- [x] Connect `POST /documents` to the ingestion pipeline.
- [x] Reuse extraction, chunking, embeddings and transactional persistence.
- [x] Verify a real PDF and execute automated verification.
- [ ] Complete administrative closure: documentation, Trello, commit and push.

**Moved to MAR-KA-008:** Implement a document status query endpoint. The earlier draft included `GET /documents/{id}`; the next increment will define the final path and response contract (proposed: `GET /documents/{id}/status`).

### 6. Verification and testing

- Unit tests cover document acceptance, processing orchestration, embedding failures and fallback error messages.
- Entity tests cover valid and invalid document status transitions.
- The document-not-found scenario was tested on 2026-11-02.
- Asynchronous dispatch and HTTP `202 Accepted` were verified.
- Real PDF ingestion and PostgreSQL persistence of chunks and embeddings were verified during the increment.
- Final Maven verification on 2026-11-02: `mvnw clean verify` — **BUILD SUCCESS**.

## Acceptance criteria

- [x] Uploading a valid PDF invokes the ingestion pipeline.
- [x] Existing processing and persistence components are reused.
- [x] Successful processing persists document chunks and embeddings.
- [x] Application-level processing failures use the existing failure-handling rules.
- [x] The HTTP endpoint returns `202 Accepted` with the persisted document ID without waiting for full background processing.
- [x] A real PDF was verified end-to-end.
- [x] Automated Maven verification passed.

**Scope clarification:** Document status querying is not an acceptance criterion for MAR-KA-007 after the scope split; it belongs to MAR-KA-008.

## Scenarios — Given / When / Then

### Scenario 1 — Successful PDF ingestion

**Given** a valid PDF and a document acceptance service that persists the document.  
**When** `POST /documents` invokes the ingestion flow.  
**Then** the response is `202 Accepted`, the background processor receives the document ID and copied bytes, and the pipeline can persist chunks and embeddings and mark the document `PROCESSED`.

### Scenario 2 — PDF reading failure

**Given** an accepted document and a failure while copying PDF bytes.  
**When** the ingestion service handles the reading error.  
**Then** the existing failure persistence behavior records `FAILED` from `ACCEPTED`.

### Scenario 3 — Background processing failure

**Given** a document being processed and an application-level failure in extraction, chunking, embeddings or guarded persistence.  
**When** the processing service catches the exception.  
**Then** it requests `FAILED` persistence with a nonblank message and rethrows the exception.

### Scenario 4 — Document not found

**Given** `DocumentRepository.findById(documentId)` returns `Optional.empty()`.  
**When** `processAndPersistDocument(documentId, pdfBytes)` is called.  
**Then** `DocumentNotFoundException` is thrown, no document save is attempted and `DocumentPersistenceService` is not invoked.

### Scenario 5 — Invalid state transition

**Given** a document in `PROCESSING`, `PROCESSED` or `FAILED`.  
**When** `markAsProcessing()` is called.  
**Then** the entity throws `IllegalStateException` and preserves its state and error message.

### Scenario 6 — HTTP contract

**Given** a valid PDF upload.  
**When** `POST /documents` accepts it.  
**Then** the API responds with `202 Accepted`, `documentId`, `fileName` and `ACCEPTED`, without waiting for Bedrock processing.

## Test coverage map

| Behavior | Evidence |
|---|---|
| Document acceptance and ID | `DocumentServiceTest` |
| Document-not-found behavior | `DocumentServiceTest.shouldThrowExceptionWhenDocumentNotFound()` — passed 2026-11-02 |
| Fallback error message | `DocumentServiceTest.shouldPersistFallbackErrorMessageWhenExceptionHasNoMessage()` |
| Processing and chunk persistence delegation | `DocumentServiceTest.shouldPersistGeneratedDocumentChunks()` |
| Embedding processing failure | `DocumentServiceTest.shouldMarkDocumentAsFailedWhenEmbeddingGenerationFails()` |
| Valid and invalid status transitions | `DocumentTest` parameterized and regular tests |
| Async dispatch and HTTP contract | Existing asynchronous and MVC tests; previously verified |
| Real PDF processing and PostgreSQL persistence | Manual verification during MAR-KA-007 |
| Complete regression verification | `mvnw clean verify` — BUILD SUCCESS, 2026-11-02 |

## Technical decisions

### Asynchronous ingestion

Use Spring `@Async` and return HTTP `202 Accepted`. Copy PDF bytes before returning from the HTTP request. The background processor delegates to the existing document processing service.

### Failure handling

Reuse `DocumentPersistenceService` for application-level processing failures. A missing document produces `DocumentNotFoundException`; it cannot be marked `FAILED` because there is no persisted document to update.

Keep the current state transitions:

- `ACCEPTED → PROCESSING`
- `ACCEPTED → FAILED`
- `PROCESSING → PROCESSED`
- `PROCESSING → FAILED`

`FAILED → PROCESSING` is **not** enabled. Retrying failed documents is deferred.

### Async exception reporting

Spring's default handler for uncaught exceptions in `@Async void` methods is retained for the MVP. It logs unhandled background exceptions; it does not provide automatic retries or guarantee a `FAILED` status for infrastructure failures.

## Known limitations and follow-up

- In-memory asynchronous execution does not recover unfinished work after application restart.
- Database failures before or during state persistence can leave a document in an intermediate state; no automatic reconciliation is implemented.
- No automatic retry mechanism for failed documents.
- PDF size and concurrency limits remain future hardening tasks.
- The document status query endpoint is planned for MAR-KA-008.

## Closure

**Technical implementation and verification:** Completed on 2026-11-02.  
**Administrative closure:** Update Trello and worklog, review Git changes, commit and push, then move the card to DONE. Do not mark GitHub publication complete until confirmed.
