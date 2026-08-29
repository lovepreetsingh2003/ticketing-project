# Test Strategy

## Test Scope

| Layer | Module | Framework | Coverage |
|-------|--------|-----------|----------|
| Unit tests | `core` | JUnit 5, Mockito, AEM Mock | All ticket servlets, models, and archetype demos |
| Integration tests | `it.tests` | JUnit 4, AEM Testing Clients, Failsafe | Generic page create/get/publish smoke tests |
| UI tests | `ui.tests` | Cypress | Login, basic page, assets, console error checks |
| Component tests | — | Not implemented | HTL rendering and client-side validation |
| Ticket E2E | — | Not implemented | Full create/search/detail/status/comment workflows |

**Run commands:**

```bash
# Unit tests
mvn clean test

# Integration tests (requires AEM on localhost:4502/4503)
mvn clean verify -Plocal

# UI tests (Docker-based)
mvn verify -Pui-tests-docker-execution
```

---

## Unit Tests

**Location:** `core/src/test/java/com/ai/ticketingApp/core/`  
**Context helper:** `AppAemContext` (Core Components + CAConfig plugins)

### Servlet Tests (7 ticket servlets)

| Test Class | Servlet Under Test | Key Scenarios |
|------------|-------------------|---------------|
| `TicketCreateServletTest` | `TicketCreateServlet` | Valid create, validation failures, invalid assignee, server error |
| `TicketUpdateServletTest` | `TicketUpdateServlet` | Valid update, missing ticketPath, validation failures, not found |
| `TicketDetailServletTest` | `TicketDetailServlet` | Valid detail, missing ticketPath, not found, server error |
| `TicketSearchServletTest` | `TicketSearchServlet` | Search with/without filters, server error |
| `TicketStatusServletTest` | `TicketStatusServlet` | Valid transition, invalid transition (409), missing params, not found |
| `TicketCommentServletTest` | `TicketCommentServlet` | Valid comment, missing params, not found |
| `TicketUsersServletTest` | `TicketUsersServlet` | Returns assignable users list |

### Model Tests

| Test Class | Model Under Test | Key Scenarios |
|------------|-----------------|---------------|
| `TicketModelTest` | `TicketModel` | Property parsing, comments/history deserialization, allowed transitions |
| `TicketListModelTest` | `TicketListModel` | Lists child tickets under tickets root |
| `CommentModelTest` | `CommentModel` | JSON serialization/deserialization |
| `StatusHistoryModelTest` | `StatusHistoryModel` | JSON serialization/deserialization |

### Archetype Demo Tests

| Test Class | Component |
|------------|-----------|
| `HelloWorldModelTest` | `HelloWorldModel` |
| `SimpleServletTest` | `SimpleServlet` |
| `LoggingFilterTest` | `LoggingFilter` |
| `SimpleResourceListenerTest` | `SimpleResourceListener` |
| `SimpleScheduledTaskTest` | `SimpleScheduledTask` |

### Approach

- Mock OSGi service dependencies (`TicketService`, `TicketValidationService`, `TicketUserService`) using `@Mock` and `@InjectMocks`.
- Use AEM Mock `AemContext` to simulate Sling requests and responses.
- Assert HTTP status codes and JSON response bodies.
- Verify service method invocations with Mockito `verify()`.

---

## Component Tests

**Status:** Not implemented.

### Recommended Approach

| Area | Tool | Scenarios |
|------|------|-----------|
| HTL rendering | AEM Mock + HTL use-object tests | Verify ticket-list, ticket-form, ticket-detail render expected markup and `data-cmp-is` attributes |
| Client-side validation | Manual or Jest (if extracted) | Title/description length, required fields, priority enum |
| CSRF helper | Unit test in isolation | Token fetch, POST header injection |

---

## API / Integration Tests

**Location:** `it.tests/src/main/java/com/ai/ticketingApp/it/tests/`  
**Framework:** JUnit 4, AEM Testing Clients (`cq-testing-clients-65`), Maven Failsafe  
**Profile:** `-Plocal` (requires AEM author on `localhost:4502`, publish on `4503`)

### Existing Tests (Archetype Smoke Tests)

| Test Class | Scenario |
|------------|----------|
| `CreatePageIT` | Create a page via Sling POST |
| `GetPageIT` | Retrieve a page via HTTP GET |
| `PublishPageValidationIT` | Validate publish workflow |

### Gaps — Recommended Ticket Integration Tests

| Test | Scenario |
|------|----------|
| `CreateTicketIT` | POST to `/bin/ticketing/ticket/create` as QA user; verify JCR node created |
| `SearchTicketIT` | Create tickets, search with filters; verify JSON response |
| `StatusTransitionIT` | Create ticket, transition OPEN → IN_PROGRESS → RESOLVED → CLOSED |
| `InvalidTransitionIT` | Attempt OPEN → CLOSED; verify 409 response |
| `CommentIT` | Add comment; verify stored in JCR property |
| `AclIT` | Verify anonymous cannot read tickets; developer cannot create |

### Configuration

```bash
mvn clean verify -Plocal \
  -Dit.author.url=http://localhost:4502 \
  -Dit.author.user=admin \
  -Dit.author.password=admin \
  -Dit.publish.url=http://localhost:4503
```

---

## Edge Case Tests

### Covered in Unit Tests

| Edge Case | Test Location | Expected Result |
|-----------|---------------|-----------------|
| Missing required params | All servlet tests | HTTP 400 |
| Title > 200 chars | `TicketCreateServletTest`, `TicketUpdateServletTest` | HTTP 400 |
| Description > 5000 chars | `TicketCreateServletTest`, `TicketUpdateServletTest` | HTTP 400 |
| Invalid priority enum | `TicketCreateServletTest` | HTTP 400 |
| Invalid assignee | `TicketCreateServletTest`, `TicketUpdateServletTest` | HTTP 400 |
| Ticket not found | Detail, update, status, comment servlet tests | HTTP 404 |
| Invalid status transition | `TicketStatusServletTest` | HTTP 409 |
| Invalid status enum | `TicketStatusServletTest` | HTTP 400 |
| Empty comment body | `TicketCommentServletTest` | HTTP 400 |
| Search server error | `TicketSearchServletTest` | HTTP 500 |
| Malformed JSON in comments/history | `TicketModelTest` | Skipped gracefully |

### Not Yet Covered

| Edge Case | Recommended Test |
|-----------|------------------|
| Concurrent updates to same ticket | Integration test with parallel POST requests |
| Large search result set (no pagination) | Performance/load test |
| CSRF token missing on POST | Integration test without `CSRF-Token` header |
| Unauthenticated API access | Integration test without session cookie |
| Developer user attempting create | Integration test with developer credentials |
| Terminal state edit attempt via API | Integration test updating CLOSED ticket |
| Special characters in title/description | Unit test with Unicode, HTML entities |
| Empty search (no tickets exist) | Unit/integration test verifying empty array |

---

## Cross-References

- Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md)
- Design and validation strategy: [design-notes.md](design-notes.md)
- API endpoint details: [api-contract.md](api-contract.md)
