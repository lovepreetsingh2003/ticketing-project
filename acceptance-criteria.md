# Acceptance Criteria

## Core

- [ ] Authenticated users can access the ticket list page at `/content/ticketingApp/us/en/tickets`
- [ ] Ticket list displays cards with title, status badge, priority badge, assignee, and reporter
- [ ] Users can filter tickets by keyword (debounced 400ms), status, and priority
- [ ] Search API returns tickets ordered by creation date descending
- [ ] QA users can create a ticket via the create page with title, description, priority, and assignee
- [ ] Created tickets receive auto-generated ID `TKT-XXXXXXXX` and initial status `OPEN`
- [ ] Reporter is auto-populated from the logged-in user and is read-only on the create form
- [ ] Users can navigate from list to detail page via ticket title link
- [ ] Detail page displays ticket metadata, comments, and status history
- [ ] Users can update ticket title, description, priority, and assignee on non-terminal tickets
- [ ] Users can change ticket status via action buttons matching allowed transitions
- [ ] Users can add comments; comments appear immediately in the list after submission
- [ ] Assignee dropdown is populated from `/bin/ticketing/users` (QA + Developer members)
- [ ] QA group can create tickets (JCR `addChildNodes` permission)
- [ ] Developer group can update tickets but cannot create new ones
- [ ] Anonymous users cannot read ticket data at `/content/ticketingApp/tickets`

## Validation

- [ ] Title is required on create and update; max 200 characters enforced server-side
- [ ] Description is required on create and update; max 5000 characters enforced server-side
- [ ] Priority is required on create; must be one of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- [ ] Assignee is required on create; must be a member of `ticketing-qa` or `ticketing-developers`
- [ ] Invalid priority or status values return HTTP 400 with descriptive error message
- [ ] Client-side validation mirrors server rules in `ticket-form.js` and `ticket-detail.js`
- [ ] Missing `ticketPath` on detail/update/status/comment endpoints returns HTTP 400

## Error Handling

- [ ] All API errors return JSON `{ "error": "<message>" }` with appropriate HTTP status
- [ ] Validation failures return HTTP 400
- [ ] Ticket not found returns HTTP 404
- [ ] Invalid status transitions return HTTP 409
- [ ] Unexpected server errors return HTTP 500 with error message
- [ ] Successful create returns HTTP 201 with `{ "success": true, "path", "ticketId" }`
- [ ] Successful comment create returns HTTP 201
- [ ] POST mutations require valid CSRF token via `window.TicketingCsrf.postForm()`
- [ ] UI displays inline error banners for API failures
- [ ] Status transition UI handles 409 specifically with user-friendly message
- [ ] Malformed stored JSON in comments/history is skipped without breaking ticket load

## Testing

- [ ] All 7 ticket servlets have unit tests in `core/src/test/java`
- [ ] Models (`TicketModel`, `TicketListModel`, `CommentModel`, `StatusHistoryModel`) have unit tests
- [ ] Unit tests pass via `mvn clean test` from project root
- [ ] Integration tests in `it.tests` run against live AEM with `-Plocal` profile
- [ ] Cypress UI tests in `ui.tests` cover login and basic page flows
- [ ] Ticket-specific E2E tests exist for create/search/detail workflows *(gap — not yet implemented)*

## Documentation

- [ ] `requirements-analysis.md` documents functional and non-functional requirements
- [ ] `acceptance-criteria.md` defines verifiable acceptance checklists
- [ ] `implementation-plan.md` describes build phases and milestones
- [ ] `design-notes.md` covers architecture, frontend, backend, and database design
- [ ] `api-contract.md` documents all `/bin/ticketing` endpoints
- [ ] `test-strategy.md` defines test scope and approach
- [ ] Root README updated to describe ticketing features *(gap — still archetype boilerplate)*
