# MAR-KA-007 — Development Worklog

## 2026-10-20

**Today's goal:**
- Configure GitHub Copilot in IntelliJ.
- Review the existing HTTP upload contract.
- Begin the six-question analysis for MAR-KA-007.

### Starting point

- MAR-KA-006 completed.
- 46 automated tests passed.
- Commit `8e63ac6` published.
- `main` synchronized and clean at increment start.
- `POST /documents` currently accepts and registers documents without invoking the complete processing pipeline.

### Completed

- Identified the official GitHub Copilot plugin.
- Reviewed the existing increment documentation structure.
- Defined the documentation approach for MAR-KA-007.

### In progress

- GitHub Copilot installation (first attempt failed due to download timeout).
- MAR-KA-007 analysis and design.

### Verified

- No new automated tests executed yet.
- No code changes verified in this increment.

### Decisions and learning

**Decision:** Maintain two documentation files per increment and use Trello for task tracking.

**Why:** Separate technical reasoning from daily progress without duplicating task management.

**What I learned:** Pending at this point in the increment.

### Blockers

- GitHub Copilot plugin download timed out.
- IntelliJ displayed an IDE error notification; details not yet reviewed.

### Next session — Starting checklist

- [ ] Check GitHub Copilot installation and authentication.
- [ ] Review `DocumentController` and `DocumentService`.
- [ ] Review the `POST /documents` contract.
- [ ] Complete the six-question analysis.
- [ ] Evaluate synchronous versus asynchronous processing before implementing.

### Resume point

**Last completed:** Documentation structure reviewed.

**Next concrete action:** Verify GitHub Copilot installation.

**Files to open:** `MAR-KA-007.md`, `DocumentController.java`, `DocumentService.java`.

## Session — 2026-10-21

### Progress

- Defined `DocumentIngestionService` dependencies: `DocumentService` and `DocumentAsyncProcessor`.
- Defined `ingestDocument(MultipartFile file)` returning `DocumentAcceptanceResult`.
- Established the happy-path flow: accept document, obtain document ID, read PDF bytes, invoke asynchronous processor, return acceptance result.
- Defined `processDocumentAsync(Long documentId, byte[] pdfBytes)`.
- Started `DocumentIngestionServiceTest.shouldIngestDocument()` using Mockito.

### Testing

- Configured `@Mock` and `@InjectMocks`.
- Prepared the acceptance result with document ID `10L`.
- Identified assertions and verifications for the returned ID, document acceptance, and asynchronous processor invocation.
- Test execution was pending at the end of this session.

### Decisions

- Pass `documentId` and `byte[]` to the asynchronous processor instead of passing `MultipartFile`.
- Delegate document failure-state persistence to the existing persistence service.
- Handle PDF byte-reading failures in a separate test.

### Next session (recorded on 2026-10-21)

1. Complete the `Then` section of `shouldIngestDocument()`.
2. Run the test and confirm the expected RED phase.
3. Implement the minimum happy-path logic in `DocumentIngestionService`.
4. Run the test again and confirm GREEN.
5. Continue with error handling and asynchronous processing.

### Resume point (recorded on 2026-10-21)

Start with `DocumentIngestionServiceTest.shouldIngestDocument()`. Do not redesign the architecture or repeat the increment analysis.

## Session — 2026-11-02

### Goal

Finish reviewing MAR-KA-007 verification and prepare an orderly increment closure.

### Completed

- Reviewed `DocumentService`, `Document`, `DocumentAsyncProcessor` and the application's `@EnableAsync` configuration.
- Confirmed that `Document` already enforces allowed status transitions and rejects invalid transitions.
- Reviewed existing `DocumentServiceTest` scenarios to avoid duplicating tests.
- Added `DocumentServiceTest.shouldThrowExceptionWhenDocumentNotFound()`.
- Verified the document-not-found test: one test executed, zero failures and zero errors.
- Confirmed an existing test for fallback error messages when exceptions have no message.
- Confirmed the earlier real PDF ingestion and PostgreSQL chunk/embedding verification.
- Executed `mvnw clean verify` successfully (BUILD SUCCESS).

### Decisions

- Preserve the existing state-transition rules; do not add `FAILED → PROCESSING` in this increment.
- Retain Spring's default uncaught-exception logging for `@Async void` methods for the MVP.
- Do not introduce automatic retry or infrastructure recovery during MAR-KA-007.
- Move the document status query endpoint to MAR-KA-008; finalize its URL and response contract there.
- Keep the existing Trello card and its four checklists instead of creating duplicate tasks.

### Technical learning

- Repository mocks should reproduce repository behavior: `findById()` returns `Optional.empty()` when the record is missing; the service's `orElseThrow()` creates `DocumentNotFoundException`.
- An entity's transition rules and the service's orchestration are different responsibilities.
- A passing test of a mock interaction does not independently prove that the database was updated; integration verification serves that purpose.
- `@Async void` exceptions do not return to the already-completed HTTP request.
- An infrastructure failure is different from an application-level document processing failure.

### Process retrospective

**What slowed us down:**
- Too much time was spent explaining concurrency-test mechanics without improving understanding.
- We revisited error scenarios without first checking whether tests already existed.
- The incremental goal became obscured by tangential technical discussions.
- Trello and the technical documentation were not updated alongside implementation.

**Working agreement for the next increment:**
1. Start with a short goal and the six-question analysis; the developer proposes the initial breakdown.
2. Review existing code and tests before proposing new implementation or coverage.
3. Work on one scenario at a time: requirement → Given/When/Then → test → minimal code → verification.
4. Timebox difficult concepts; record unresolved questions instead of blocking MVP progress.
5. End each session with verified results, remaining work, documentation updates and a clear resume point.

### Verification

- Targeted document-not-found test: PASSED.
- Full Maven verification: BUILD SUCCESS — 2026-11-02.
- Previous real PDF ingestion and PostgreSQL persistence checks: confirmed from the increment's earlier work.

### Known limitations

- Application restarts can interrupt in-memory background work.
- Database/infrastructure failures may leave documents in intermediate states.
- Automatic retries and reconciliation are not implemented.

### Closure checklist

- [x] Technical implementation and Maven verification completed.
- [x] Technical document and worklog content prepared for update.
- [ ] Confirm updated documents are saved in the repository.
- [ ] Update Trello checklists and add the closure comment.
- [ ] Review `git status` and `git diff --stat`.
- [ ] Commit and push MAR-KA-007 changes.
- [ ] Move Trello card to DONE after confirming the push.

### Next session — MAR-KA-008

**Goal:** Define and implement the document status query endpoint.

**First action:** Review the current controller, service, repository and DTOs; then answer the six design questions and derive the test scenarios before writing code.

**Proposed route:** `GET /documents/{id}/status` (to be confirmed during the new increment).
