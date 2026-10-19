# MAR-KA-006 — Persist document chunks and embeddings

**Status:** Implementation and automated verification completed
**Trello:** [MAR-KA-006](https://trello.com/c/yDx9khr0/55-mar-ka-006-persist-document-chunks-and-embeddings)
**Last reviewed:** 2026-10-20

## Goal

Persist processed document chunks and their embeddings in PostgreSQL with pgvector for future RAG similarity search.

## Current implementation

- `Document` stores PDF metadata, processing status, and error information.
- `DocumentChunk` stores chunk text, index, embedding, and document association.
- Flyway manages the PostgreSQL schema, including `VECTOR(1024)`.
- `DocumentService` orchestrates text extraction, chunking, embedding generation, and persistence.
- `DocumentPersistenceService` handles transactional chunk persistence and document status updates.
- Successful processing persists chunks and transitions the document to `PROCESSED`.
- Processing failures are handled by transitioning the document to `FAILED` with an error message.
- Database transactions do not span external Bedrock calls.
- Integration tests verify persistence, status updates, and rollback behavior.

## Business rules

- Initial status: `ACCEPTED`.
- Allowed transitions:
    - `ACCEPTED → PROCESSING`
    - `PROCESSING → PROCESSED`
    - `PROCESSING → FAILED`
- `PROCESSED` and `FAILED` are terminal states for this MVP.
- `markAsFailed()` requires a non-null, non-blank error message.
- Invalid transitions must leave document state unchanged.
- Processing failures must not leave partially persisted chunks.

## Acceptance criteria

- [x] Document processing can be invoked through the service, persisting its chunks and embeddings.
- [x] Successful processing results in `PROCESSED`.
- [x] Failed processing results in `FAILED` with an error message.
- [x] Failures do not leave partially persisted chunks.
- [x] Chunks can be retrieved by document ID with valid vector dimensions.
- [x] Automated tests pass (46 tests, 0 failures).

## Scope limitation

End-to-end PDF ingestion through `POST /documents` is not yet
implemented. The endpoint currently accepts and registers documents.

Connecting the endpoint to the processing pipeline and manually
verifying a real PDF will be addressed in the next increment.

## Scenarios

### Successful processing

**Given** a PDF document in `ACCEPTED` status.

**When** processing starts, text is extracted, embeddings are generated, and all chunks are successfully persisted.

**Then** the document transitions to `PROCESSING` at the start and finishes in `PROCESSED`, with its chunks associated with the document.

### Processing failure — Bedrock

**Given** a document in `PROCESSING` status.

**When** Bedrock fails to generate embeddings.

**Then** the document transitions to `FAILED`, an error message is persisted, and no partial chunks remain.

### Processing failure — General

**Given** a document in `PROCESSING` status.

**When** text extraction or database persistence fails.

**Then** the document transitions to `FAILED`, an error message is persisted, and no partial chunks remain.

### Invalid transition

**Given** a document in an invalid state for the requested transition.

**When** the transition is attempted.

**Then** an `IllegalStateException` is thrown and the document remains unchanged.

## Test coverage map

| Behavior | Test | Status |
|---|---|---|
| Document state transitions | DocumentTest | Passed |
| Document persistence | DocumentRepositoryTest | Passed |
| Chunk and vector persistence | DocumentChunkRepositoryTest | Passed |
| Text extraction, chunking and embeddings | DocumentServiceTest | Passed |
| Processing orchestration | DocumentServiceTest | Passed |
| Transactional persistence and rollback | DocumentPersistenceServiceIntegrationTest | Passed |
| Failure status persistence | DocumentServiceIntegrationTest | Passed |
| HTTP acceptance contract | DocumentControllerTest | Passed |

**Full Maven test suite:** 46 tests, 0 failures, 0 errors, 0 skipped.

**Pending:** End-to-end processing of a real PDF through the HTTP API.

## Technical decisions

- PostgreSQL 17 and pgvector run through Docker Compose.
- Flyway manages database migrations.
- Hibernate maps embeddings using `float[]` and `@JdbcTypeCode(SqlTypes.VECTOR)`.
- `DocumentChunk` has a unidirectional `@ManyToOne` relationship with `Document`.

### Transaction management

- Document processing and Bedrock calls execute outside database transactions.
- DocumentPersistenceService manages transactional persistence.
- Chunks and the PROCESSED status are persisted atomically.
- A persistence failure rolls back the transaction.
- The FAILED status and error message are persisted in a separate transaction.
- Transactional methods are invoked through a Spring-managed service.

## Next implementation tasks

- [x] Complete `Document` state-transition unit tests.
- [x] Define successful processing and failure scenarios for `DocumentService`.
- [x] Implement processing orchestration and status persistence.
- [x] Define and implement transaction boundaries.
- [x] Add orchestration and failure-handling tests.
- [x] Verify transaction rollback and status persistence in PostgreSQL.
- [ ] Process a real PDF end-to-end (moved to the next increment).
- [x] Run the full Maven test suite.
- [ ] Update Trello, commit, and push changes.

## Definition of Done

- [x] Persistence implementation completed.
- [x] Document status transitions implemented and tested.
- [x] Transaction boundaries and rollback behavior verified.
- [x] PostgreSQL and pgvector integration tests passed.
- [x] Full Maven test suite passed (46 tests).
- [x] Technical documentation updated.
- [ ] Trello updated.
- [ ] Changes committed and pushed to GitHub.

End-to-end PDF ingestion through the HTTP API is outside
the scope of this increment and will be addressed separately.