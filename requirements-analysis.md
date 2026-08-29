# Requirement Analysis

## Selected Project Option

**Ticket Management Application** — an Adobe Experience Manager (AEM 6.5.22) web application for creating, tracking, and resolving support tickets. Built using the AEM Project Archetype with a custom ticketing domain layered on top of standard AEM patterns (OSGi services, Sling servlets, HTL components, JCR content storage).

## My Understanding (in your own words)

This application lets authenticated QA and Developer users manage support tickets through a browser-based UI hosted on AEM. Tickets are stored as JCR content nodes (not in an external database) under `/content/ticketingApp/tickets`. Users can search and filter tickets, create new ones, view details, update fields, change status through a defined workflow, and add comments. Access is role-based: QA users can create tickets; both QA and Developer users can view and modify existing tickets. All ticket operations are exposed as JSON REST-style endpoints under `/bin/ticketing`, consumed by vanilla JavaScript client-side code embedded in AEM HTL components.

## Functional Requirements

### Ticket Management
- **FR-01**: Users shall be able to create a ticket with title, description, priority, and assignee.
- **FR-02**: The system shall auto-generate a unique ticket ID in the format `TKT-XXXXXXXX` (8 uppercase hex characters from UUID).
- **FR-03**: The reporter shall be automatically set to the currently logged-in AEM user.
- **FR-04**: New tickets shall always start with status `OPEN`.
- **FR-05**: Users shall be able to search tickets by keyword (title or description), status, and priority.
- **FR-06**: Users shall be able to view full ticket details including comments and status history.
- **FR-07**: Users shall be able to update ticket title, description, priority, and assignee.
- **FR-08**: Users shall be able to change ticket status following a defined state machine.
- **FR-09**: Users shall be able to add comments to a ticket; the author is recorded as the logged-in user.
- **FR-10**: Users shall be able to select assignees from a list of QA and Developer group members.

### Status Workflow
- **FR-11**: Allowed transitions:
  - `OPEN` → `IN_PROGRESS`, `CANCELLED`
  - `IN_PROGRESS` → `RESOLVED`, `CANCELLED`
  - `RESOLVED` → `CLOSED`
  - `CLOSED` and `CANCELLED` are terminal states (no further transitions).
- **FR-12**: Each status change shall be recorded in a status history log with user, from-status, to-status, and timestamp.

### Access Control
- **FR-13**: All ticket pages and `/bin/ticketing` endpoints shall require authentication.
- **FR-14**: Anonymous users shall be denied read access to ticket data.
- **FR-15**: QA group members (`ticketing-qa`) shall be able to create and modify tickets.
- **FR-16**: Developer group members (`ticketing-developers`) shall be able to read and modify tickets but not create new ones.
- **FR-17**: Only users in `ticketing-qa` or `ticketing-developers` groups may be assigned as ticket assignees.

### UI Pages
- **FR-18**: Ticket list page at `/content/ticketingApp/us/en/tickets`.
- **FR-19**: Create ticket page at `/content/ticketingApp/us/en/tickets/create`.
- **FR-20**: Ticket detail page at `/content/ticketingApp/us/en/tickets/detail?ticketPath=...`.

## Non-Functional Requirements

- **NFR-01**: Platform — Adobe Experience Manager 6.5.22 on Java 8 / OSGi / Apache Sling.
- **NFR-02**: API responses shall be JSON with `Content-Type: application/json;charset=UTF-8`.
- **NFR-03**: POST mutations shall require a Granite CSRF token (`CSRF-Token` header).
- **NFR-04**: Data persistence shall use JCR (Jackrabbit Oak); no external relational database.
- **NFR-05**: Security configuration shall be applied via repoinit scripts on deployment.
- **NFR-06**: Frontend shall use HTL templates with vanilla JavaScript (no SPA framework for ticket UI).
- **NFR-07**: Site-wide styling shall be built via Webpack/SCSS (`ui.frontend` module).
- **NFR-08**: The application shall follow AEM Project Archetype multi-module Maven structure.
- **NFR-09**: Backend business logic shall be unit-tested using JUnit 5, Mockito, and AEM Mock.
- **NFR-10**: Search results shall be ordered by creation date descending.

## Assumptions

- Assignees are limited to direct members of AEM groups `ticketing-qa` and `ticketing-developers` (nested group members are excluded).
- There is no email or push notification on ticket events.
- There is no file attachment support on tickets.
- Search returns all matching tickets with no pagination limit (`p.limit = -1`).
- Display names for assignable users default to the AEM user ID (no separate profile lookup).
- Terminal tickets (`CLOSED`, `CANCELLED`) hide the edit form in the UI but remain readable.
- Comments and status history are stored as JSON strings in JCR multi-value properties, not as child nodes.
- Only English (`us/en`) locale content is provisioned (`singleCountry=y` in archetype).

## Clarifications (questions for a product owner)

1. Should users receive email or in-app notifications when a ticket is assigned or status changes?
2. Is pagination required for the ticket search/list view? If so, what is the default page size?
3. Should file attachments be supported on tickets or comments?
4. Is an Admin role needed to manage all tickets regardless of assignee?
5. Should developers be allowed to create tickets, or is create restricted to QA only by design?
6. What is the data retention policy for closed/cancelled tickets?
7. Should ticket deletion (hard or soft delete) be supported?
8. Is audit logging beyond status history required (e.g., field-level change tracking)?
9. Should the ticket list support sorting by columns other than creation date?
10. Is multi-language support planned beyond `us/en`?

## Edge Cases

| Edge Case | Expected Behavior |
|-----------|-------------------|
| Invalid status transition (e.g., OPEN → CLOSED) | HTTP 409 with error message; UI shows transition-specific error |
| Ticket not found (invalid `ticketPath`) | HTTP 404 with `{ "error": "Ticket not found: ..." }` |
| Assignee not in QA/Developer groups | HTTP 400 with `{ "error": "Assignee must be a QA or Developer user" }` |
| Missing required form fields | HTTP 400 with validation error message |
| Title exceeds 200 characters | HTTP 400 — "Title must not exceed 200 characters" |
| Description exceeds 5000 characters | HTTP 400 — "Description must not exceed 5000 characters" |
| Invalid priority or status enum value | HTTP 400 with list of valid values |
| Unauthenticated access to `/bin/ticketing` | Redirect to AEM login (Sling auth requirements) |
| Anonymous access to ticket JCR data | Denied by repoinit ACL |
| Malformed JSON in stored comments/history | Warned and skipped during parse; ticket still loads |
| Empty search results | UI shows empty state with link to create page |
| Edit attempted on CLOSED/CANCELLED ticket | Edit form hidden in UI; server-side update still possible if API called directly |
| CSRF token missing or expired on POST | Request rejected by Granite CSRF filter |
| Concurrent updates to same ticket | Last write wins (no optimistic locking) |
