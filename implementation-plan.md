# Implementation Plan

## Overview

Build a Ticket Management Application on Adobe Experience Manager 6.5.22 using the AEM Project Archetype as a foundation. The application stores tickets as JCR content nodes, exposes JSON APIs via Sling servlets, and provides a browser UI through HTL components with vanilla JavaScript. Role-based access is enforced via repoinit ACLs and Sling authentication requirements.

**Tech stack:** Java 8, OSGi, Apache Sling, HTL, Webpack/SCSS, Core WCM Components 2.26.0, JUnit 5, Cypress.

## Task Breakdown

### Phase 1 — Backend Core (`core` module)

| Task | Description | Key Files |
|------|-------------|-----------|
| 1.1 | Define enums for `TicketStatus` and `TicketPriority` | `enums/TicketStatus.java`, `enums/TicketPriority.java` |
| 1.2 | Implement `TicketService` — CRUD, search, comments, status changes | `services/impl/TicketServiceImpl.java` |
| 1.3 | Implement status state machine | `services/impl/TicketStateServiceImpl.java` |
| 1.4 | Implement field validation | `services/impl/TicketValidationServiceImpl.java` |
| 1.5 | Implement assignable user lookup via service user | `services/impl/TicketUserServiceImpl.java` |
| 1.6 | Create Sling servlets for all 7 API endpoints | `servlets/Ticket*.java` |
| 1.7 | Create Sling Models for ticket data | `models/TicketModel.java`, `TicketListModel.java`, etc. |
| 1.8 | Register authentication requirements | `services/impl/TicketingAuthenticationRequirements.java` |
| 1.9 | Write unit tests for servlets and models | `core/src/test/java/**` |

### Phase 2 — AEM Components & Clientlibs (`ui.apps` module)

| Task | Description | Key Files |
|------|-------------|-----------|
| 2.1 | Create ticket-list HTL component | `components/ticket-list/` |
| 2.2 | Create ticket-form HTL component | `components/ticket-form/` |
| 2.3 | Create ticket-detail HTL component | `components/ticket-detail/` |
| 2.4 | Implement clientlib-ticketing JS (list, form, detail, CSRF) | `clientlibs/clientlib-ticketing/js/` |
| 2.5 | Implement ticket UI CSS (Jira-inspired design) | `clientlibs/clientlib-ticketing/css/ticketing.css` |
| 2.6 | Wire clientlibs into page component footer | `components/page/customfooterlibs.html` |

### Phase 3 — Content & Pages (`ui.content` module)

| Task | Description | Key Files |
|------|-------------|-----------|
| 3.1 | Create ticket list, create, and detail pages | `content/ticketingApp/us/en/tickets/` |
| 3.2 | Configure page template and policies | `conf/ticketingApp/settings/wcm/` |
| 3.3 | Set up experience fragments (header/footer) | `experience-fragments/ticketingApp/` |

### Phase 4 — Configuration & Security (`ui.config` module)

| Task | Description | Key Files |
|------|-------------|-----------|
| 4.1 | Repoinit: ticket storage path, groups, ACLs, service user | `RepositoryInitializer~ticketingApp.cfg.json` |
| 4.2 | Service user mapping for `ticketing-user-reader` | `ServiceUserMapperImpl.amended~ticketingApp.cfg.json` |
| 4.3 | CORS policy for local development | `CORSPolicyImpl~ticketingApp.cfg.json` |
| 4.4 | Publish URL mapping | `JcrResourceResolverFactoryImpl.cfg.json` |

### Phase 5 — Frontend Build (`ui.frontend` module)

| Task | Description | Key Files |
|------|-------------|-----------|
| 5.1 | Configure Webpack build for site SCSS/JS | `webpack.common.js`, `clientlib.config.js` |
| 5.2 | Style Core WCM Components | `src/main/webpack/components/` |
| 5.3 | Generate clientlib-site and clientlib-dependencies | Output to `ui.apps/clientlibs/` |

### Phase 6 — Testing & Deployment

| Task | Description | Key Files |
|------|-------------|-----------|
| 6.1 | Unit tests (16 test classes) | `core/src/test/java/` |
| 6.2 | Integration test scaffold | `it.tests/` |
| 6.3 | Cypress UI test scaffold | `ui.tests/test-module/` |
| 6.4 | Container package assembly | `all/pom.xml` |
| 6.5 | Dispatcher configuration | `dispatcher/` |

## Milestones

| Milestone | Deliverable | Status |
|-----------|-------------|--------|
| M1 — Backend API | All 7 servlets, services, models, unit tests | Complete |
| M2 — Security | Repoinit ACLs, auth requirements, CSRF, service user | Complete |
| M3 — UI Components | List, form, detail HTL + clientlib JS/CSS | Complete |
| M4 — Content Pages | Ticket pages deployed under `/content/ticketingApp/us/en/tickets` | Complete |
| M5 — Unit Test Coverage | 16 test classes covering servlets and models | Complete |
| M6 — Documentation | Root-level project documentation (6 files) | In progress |
| M7 — E2E Tests | Ticket-specific Cypress/integration tests | Not started |
| M8 — README Update | Replace archetype boilerplate with project-specific docs | Not started |

## AI Usage Plan

| Activity | AI Role |
|----------|---------|
| Archetype extension | Generate OSGi service interfaces, servlet scaffolding, and Sling Model boilerplate from requirements |
| Validation & state machine | Implement `TicketValidationService` and `TicketStateService` with transition rules |
| Unit test generation | Create servlet and model test classes using AEM Mock patterns |
| Client-side JS | Generate vanilla JS for list search, form validation, detail page interactions |
| Documentation | Reverse-engineer requirements, API contract, design notes, and test strategy from implemented code |
| Code review | Identify gaps (missing E2E tests, pagination, README updates) |

**Guidelines for AI-assisted development:**
- Follow existing AEM archetype conventions (module structure, naming, OSGi annotations).
- Validate all generated code against existing patterns in the codebase.
- Do not introduce new dependencies unless justified.
- Human review required for security-sensitive code (ACLs, auth requirements, CSRF).

## Risks

| Risk | Impact | Likelihood |
|------|--------|------------|
| No ticket-specific E2E tests | Regressions in UI workflows undetected | High |
| Search with no pagination limit | Performance degradation with large ticket volumes | Medium |
| Last-write-wins on concurrent updates | Data loss if two users edit same ticket simultaneously | Medium |
| README still archetype boilerplate | Onboarding confusion for new developers | High |
| Comments/history stored as JSON strings in properties | Harder to query/index individual comments | Low |
| No notification system | Users unaware of ticket changes | Medium |
| Generic integration tests only | Backend ticket flows not validated against live AEM | Medium |

## Mitigation

| Risk | Mitigation |
|------|------------|
| No E2E tests | Add Cypress tests for create, search, detail, status change, and comment flows |
| No pagination | Add `offset`/`limit` query params to search servlet; paginate in UI |
| Concurrent updates | Add JCR mix:lastModified check or version-based optimistic locking |
| README gap | Update root README with ticketing feature overview, setup, and API summary |
| JSON property storage | Acceptable for MVP; migrate to child nodes if query requirements grow |
| No notifications | Document as out-of-scope; add email servlet in future phase if PO confirms |
| Generic IT tests | Add ticket-specific integration tests in `it.tests` module |
