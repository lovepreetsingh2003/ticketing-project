# Chat record: Unit tests for models and servlets

Workspace: `ticketingApp` (AEM 6.5.22).  
This file records the user prompts, AI responses, and what was accepted for the unit-test and build-validation conversation. Related prior work: `chat-ticketing-implementation.md`, `chat-auth-and-form-improvements.md`.

---

## Turn 1 — Unit tests and Maven build validation (Agent mode)

### Prompt (summary)

Write unit test classes for all the model and servlet classes created in the codebase. Validate the classes by running:

```bash
mvn clean install -PautoInstallPackage
```

to confirm the build runs successfully.

### AI response (summary)

Explored `core/src/main/java` and existing tests under `core/src/test/java`, then added tests following `AppAemContext`, `AemContextExtension`, and `SimpleServletTest` patterns.

#### Model tests added

| Test class | Covers |
|------------|--------|
| `TicketModelTest` | Resource adaptation, field mapping, allowed transitions, terminal status (`CLOSED` / `CANCELLED`), malformed comment/status-history JSON ignored |
| `TicketListModelTest` | Tickets loaded from `/content/ticketingApp/tickets`, empty list when root missing, non-ticket children skipped |
| `CommentModelTest` | Constructor fields and `fromJson` deserialization |
| `StatusHistoryModelTest` | Constructor fields and `fromJson` deserialization |

Existing model test retained: `HelloWorldModelTest`.

`TicketAuditModelsTest` was removed and split into `CommentModelTest` and `StatusHistoryModelTest` (one test class per model).

#### Servlet tests added

| Test class | Covers |
|------------|--------|
| `TicketCreateServletTest` | Success (201), validation error (400), non-assignable user (400), create failure (500) |
| `TicketUpdateServletTest` | Success, missing `ticketPath` (400), not found (404) |
| `TicketCommentServletTest` | Success (201), missing path/body (400), not found (404) |
| `TicketDetailServletTest` | Success, missing path (400), not found (404) |
| `TicketSearchServletTest` | Success with keyword/status/priority filters, search failure (500) |
| `TicketStatusServletTest` | Success, missing params (400), invalid status (400), invalid transition (409) |
| `TicketUsersServletTest` | Returns assignable users JSON with `Cache-Control: no-store` |

Existing servlet test retained: `SimpleServletTest`.

Servlet tests mock `TicketService`, `TicketValidationService`, and `TicketUserService` with Mockito, register OSGi services via `context.registerService()`, and activate servlets with `context.registerInjectActivateService()`. Reporter/comment author assertions use `context.resourceResolver().getUserID()` instead of hard-coded user IDs.

#### Build fix

First `mvn clean install -PautoInstallPackage` failed with:

- Test compile errors: `MockSlingHttpServletRequest` has no `setParameter()` — fixed by using `setParameterMap()`
- Mockito `when(...).thenThrow()` on void service methods — fixed with `doThrow().when()`
- Runtime: `NoClassDefFoundError: com.fasterxml.jackson.databind.util.internal.PrivateMaxEntriesMap$Builder` — Jackson classpath conflict between uber-jar and `ObjectMapper` usage in models/servlets during tests

Added to `core/pom.xml` (test scope only):

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
    <scope>test</scope>
</dependency>
```

This does not affect the OSGi bundle runtime (test scope); production still relies on Jackson from AEM uber-jar.

#### Build result (accepted run)

```
mvn clean install -PautoInstallPackage
```

- **37 tests** — 0 failures, 0 errors, 0 skipped
- **BUILD SUCCESS** (~3 minutes)
- Full reactor completed including `autoInstallPackage` profile modules

### Accepted

1. **Test coverage scope** — unit tests for all ticketing models (`TicketModel`, `TicketListModel`, `CommentModel`, `StatusHistoryModel`) and all ticketing servlets (`TicketCreate`, `TicketUpdate`, `TicketComment`, `TicketDetail`, `TicketSearch`, `TicketStatus`, `TicketUsers`), plus existing archetype tests.
2. **Test patterns** — AEM Mock (`AppAemContext`), Mockito for service injection, `registerInjectActivateService` for servlet OSGi references.
3. **Split audit model tests** — `TicketAuditModelsTest` replaced by dedicated `CommentModelTest` and `StatusHistoryModelTest`.
4. **Jackson test dependency** — `jackson-databind` 2.17.2 test scope in `core/pom.xml` to fix unit-test classpath; not a runtime bundle dependency.
5. **Build validation** — `mvn clean install -PautoInstallPackage` must pass with all unit tests green.

### Not requested / not done

- Unit tests for service implementations (`TicketServiceImpl`, `TicketValidationServiceImpl`, etc.)
- Integration tests in `it.tests`
- UI / Cypress tests in `ui.tests`
- Git commit or pull request

---

## Turn 2 — This file

### Prompt (summary)

Take context of the whole above chat and create a markdown file inside the `ai-prompts` folder that describes the prompt text or summary, the AI response summary, and what response was accepted for the complete chat.

### AI response

This document (`ai-prompts/chat-unit-tests.md`).

### Accepted

(Pending user review of this file.)

---

## Decisions accepted for the product (rollup)

1. **Models and servlets** have JUnit 5 unit tests in `core/src/test/java`.
2. **Test tooling** — `io.wcm.testing.aem-mock.junit5`, Mockito, `AppAemContext`.
3. **Servlet tests** mock OSGi `@Reference` services; do not require a live AEM instance.
4. **Model tests** use `context.addModelsForClasses()` and `resource.adaptTo()` / `request.adaptTo()`.
5. **CI / local gate** — `mvn clean install -PautoInstallPackage` is the validation command; all 37 core tests must pass.
6. **Test-only Jackson** — explicit `jackson-databind` test dependency resolves uber-jar vs `ObjectMapper` conflicts in Surefire.

## Files touched in this chat

| Path | Change |
|------|--------|
| `core/src/test/java/.../models/TicketModelTest.java` | Added |
| `core/src/test/java/.../models/TicketListModelTest.java` | Added |
| `core/src/test/java/.../models/CommentModelTest.java` | Added |
| `core/src/test/java/.../models/StatusHistoryModelTest.java` | Added |
| `core/src/test/java/.../models/TicketAuditModelsTest.java` | Removed (split) |
| `core/src/test/java/.../servlets/TicketCreateServletTest.java` | Added |
| `core/src/test/java/.../servlets/TicketUpdateServletTest.java` | Added |
| `core/src/test/java/.../servlets/TicketCommentServletTest.java` | Added |
| `core/src/test/java/.../servlets/TicketDetailServletTest.java` | Added |
| `core/src/test/java/.../servlets/TicketSearchServletTest.java` | Added |
| `core/src/test/java/.../servlets/TicketStatusServletTest.java` | Added |
| `core/src/test/java/.../servlets/TicketUsersServletTest.java` | Added |
| `core/pom.xml` | Added `jackson-databind` test dependency |
