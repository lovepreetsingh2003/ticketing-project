# Chat record: Assignee users servlet — service user fix

Workspace: `ticketingApp` (AEM 6.5.22).  
This file records the user prompts, AI responses, and what was accepted for the `TicketUsersServlet` / assignee-dropdown bug conversation. Related earlier work: `chat-auth-and-form-improvements.md` (introduced `TicketUsersServlet` and assignee dropdown).

---

## Turn 1 — Diagnose why only the logged-in user is returned (Ask mode)

### Prompt (summary)

`TicketUsersServlet` (`GET /bin/ticketing/users`) was created to list all users in the `ticketing-qa` and `ticketing-developers` groups for the assignee dropdown. When logged in as a normal user (e.g. `test-dev`), the servlet returns only that user instead of every member of both groups. What went wrong?

### AI response (summary)

Traced the flow:

- `TicketUsersServlet` passes `request.getResourceResolver()` to `TicketUserService.getAssignableUsers()`
- `TicketUserServiceImpl` uses that session’s `UserManager` and calls `Group.getMembers()` for `ticketing-qa` and `ticketing-developers`

**Root cause:** In AEM/Oak, `UserManager` and `Group.getMembers()` are access-controlled. A non-admin session can typically read only its own user node, so `getMembers()` returns only the logged-in user. Repoinit ACLs on `/content/ticketingApp` do not grant read on `/home/users` or `/home/groups`.

**Secondary checks noted (less likely):**

- Group ID in code is `ticketing-developers` (not `ticketing-dev`)
- Nested group members are skipped (`member.isGroup()` → `continue`)
- `isAssignableUser()` has the same visibility limitation

**Suggested fix:** Use a dedicated service user with read access to `/home/users` and `/home/groups`, mapped via `ServiceUserMapperImpl`, and open a service `ResourceResolver` inside `TicketUserServiceImpl` for user enumeration. Keep the request resolver for ticket CRUD.

**Confirmation test proposed:**

1. Call `/bin/ticketing/users` as `admin` → all group members
2. Call as `test-dev` → only `test-dev`

### Accepted

The diagnosis (Oak user visibility / session scoping). User later confirmed both confirmation tests hold on their system.

---

## Turn 2 — Confirm diagnosis (Ask mode)

### Prompt (summary)

Confirmation tests 1 and 2 hold: admin sees all users; `test-dev` sees only themselves.

### AI response (summary)

Confirmed the root cause. Restated the implementation steps:

1. Repoinit — create `ticketing-user-reader` service user; grant `jcr:read` on `/home/users` and `/home/groups`
2. OSGi — `ServiceUserMapperImpl.amended` mapping subservice `ticketing-user-reader` to that user
3. `TicketUserServiceImpl` — inject `ResourceResolverFactory`, use service resolver for `getMembers()`
4. Redeploy and verify assignee list and `isAssignableUser()` for non-admin users

Offered Agent mode to implement in the repo.

### Accepted

The service-user approach as the chosen fix. User proceeded to implementation in the next turn.

---

## Turn 3 — Implement service user approach (Agent mode)

### Prompt

Implement the above approach.

### AI response (summary)

Implemented the following:

| Area | Change |
|------|--------|
| Repoinit | `RepositoryInitializer~ticketingApp.cfg.json` — `create service user ticketing-user-reader`; ACL read on `/home/users` and `/home/groups` |
| OSGi | New `org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~ticketingApp.cfg.json` — `ticketingApp.core:ticketing-user-reader=[ticketing-user-reader]` |
| Core | `TicketUserServiceImpl` — `@Reference ResourceResolverFactory`; subservice `ticketing-user-reader`; try-with-resources service resolver for `loadAssignableUsers()` |

**Unchanged:**

- `TicketUsersServlet` — still calls `getAssignableUsers(request.getResourceResolver())`; interface unchanged
- `TicketCreateServlet` / `TicketUpdateServlet` — still call `isAssignableUser()` with request resolver (implementation now uses service resolver internally)
- Group IDs: `ticketing-qa`, `ticketing-developers`

**Deploy notes provided:**

- Deploy `ui.config` and `core`
- Verify service user exists in User Administration
- On instances where repoinit already ran, may need manual service-user creation or repoinit re-run for new script lines
- Check error log for `Unable to obtain service resource resolver` if mapping is missing

`mvn -pl core,ui.config -am compile` succeeded after correcting bundle symbolic name to `ticketingApp.core` (from generated `MANIFEST.MF`).

### Accepted

Full implementation as described. Servlet and service interface signatures left stable; elevation isolated to `TicketUserServiceImpl`.

---

## Turn 4 — This file

### Prompt (summary)

Using the whole chat, create a markdown file in `ai-prompts` describing prompt text/summary, AI response summary, and what was accepted for the complete chat.

### AI response

This document.

### Accepted

(Pending user review of this file.)

---

## Decisions accepted for the product (rollup)

1. **Problem:** Assignee user list was scoped to the logged-in user because `Group.getMembers()` ran under the request session, not because servlet or group-query logic was wrong.
2. **Fix pattern:** Service user `ticketing-user-reader` with read-only access to `/home/users` and `/home/groups`.
3. **Subservice name:** `ticketing-user-reader` mapped to bundle `ticketingApp.core`.
4. **Scope of elevation:** Only user/group enumeration in `TicketUserServiceImpl`; ticket writes remain under the logged-in user’s resolver.
5. **Assignee groups:** `ticketing-qa` and `ticketing-developers` (unchanged).
6. **Verification:** Non-admin users should see all assignable group members at `GET /bin/ticketing/users` after deploy.

## Not accepted / out of scope

- Granting broad `/home/users` read to `ticketing-qa` / `ticketing-developers` end users (rejected in favour of a dedicated read-only service user).
- Changing group IDs or servlet path.
- Removing nested-group skip in `addGroupMembers()` (not part of this fix).
- Adding unit tests for service-user resolution (not requested in this chat).

## Files touched in accepted implementation

- `ui.config/.../org.apache.sling.jcr.repoinit.RepositoryInitializer~ticketingApp.cfg.json`
- `ui.config/.../org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~ticketingApp.cfg.json` (new)
- `core/.../services/impl/TicketUserServiceImpl.java`
