# Debugging Notes

## Issue 1 — Assignee dropdown shows only the logged-in user

### Problem

`GET /bin/ticketing/users` (`TicketUsersServlet`) was built to return all members of the `ticketing-qa` and `ticketing-developers` groups for the assignee dropdown on the create and edit ticket forms.

**Expected:** Every user in both groups appears in the JSON response and in the assignee `<select>`.

**Actual:** When logged in as a normal ticketing user (e.g. `test-dev`), the endpoint returns only that user:

```json
[
  { "userId": "test-dev", "displayName": "test-dev" }
]
```

The create/edit forms therefore only allow assigning tickets to yourself. This blocked FR-10 (select assignees from QA and Developer group members) and FR-17 (only group members may be assigned).

**Affected components:**

- `core/.../servlets/TicketUsersServlet.java`
- `core/.../services/impl/TicketUserServiceImpl.java`
- `ui.apps/.../js/ticket-form.js` and `ticket-detail.js` (consume `/bin/ticketing/users`)
- `TicketCreateServlet` / `TicketUpdateServlet` via `isAssignableUser()` (same visibility bug)

---

### How I Investigated

1. **Reproduced in the UI** — Opened Create New Ticket while logged in as `test-dev`. Assignee dropdown contained only `test-dev`.

2. **Called the API directly** — `GET /bin/ticketing/users` (same-origin, authenticated) returned a single-user array for `test-dev`.

3. **Reviewed the code path:**
   - `TicketUsersServlet` calls `ticketUserService.getAssignableUsers(request.getResourceResolver())`
   - `TicketUserServiceImpl` adapts the resolver to `UserManager`, loads groups `ticketing-qa` and `ticketing-developers`, and iterates `Group.getMembers()`

4. **Checked repoinit** — Groups and content ACLs exist in `RepositoryInitializer~ticketingApp.cfg.json`, but there were no ACLs on `/home/users` or `/home/groups`, and no service user for elevated user reads.

5. **Ruled out secondary causes:**
   - Group IDs in code match repoinit: `ticketing-qa`, `ticketing-developers`
   - Servlet path and frontend `USERS_URL` are correct
   - Nested group members are skipped in code (`member.isGroup()`), but that would hide users—not explain seeing exactly the current user

---

### How AI Helped

1. **Traced the full request flow** from servlet → `TicketUserServiceImpl` → `UserManager` / `Group.getMembers()`.

2. **Identified the root cause:** In AEM/Oak, user directory access is session-scoped. `Group.getMembers()` only returns authorizables the **current** `ResourceResolver` is allowed to read. Non-admin users typically can read their own user node only, so enumeration collapses to the logged-in user.

3. **Explained why repoinit content ACLs did not help** — Permissions on `/content/ticketingApp` do not grant visibility into `/home/users` or `/home/groups`.

4. **Proposed a confirmation test:**
   - As `admin` → `/bin/ticketing/users` should list all group members
   - As `test-dev` → only `test-dev`

5. **Recommended the fix pattern:** Dedicated read-only service user + repoinit ACLs + `ServiceUserMapperImpl` mapping + `ResourceResolverFactory.getServiceResourceResolver()` inside `TicketUserServiceImpl`, while keeping ticket CRUD on the request resolver.

6. **Implemented the fix** (Agent mode): repoinit, OSGi config, and `TicketUserServiceImpl` changes. Corrected bundle symbolic name to `ticketingApp.core` from the built manifest.

---

### What I Validated

| Check | Result |
|-------|--------|
| `GET /bin/ticketing/users` as **admin** | Returns all members of `ticketing-qa` and `ticketing-developers` |
| `GET /bin/ticketing/users` as **test-dev** (before fix) | Returns only `test-dev` — confirms session visibility issue |
| Servlet and group-query logic | Structurally correct; bug is authorization context, not wrong group IDs or servlet wiring |
| `isAssignableUser()` | Same limitation as list endpoint when using request resolver |
| Maven compile (`mvn -pl core,ui.config -am compile`) | Passed after implementation |

**Post-fix validation (after deploy):**

- [ ] Service user `ticketing-user-reader` exists in User Administration
- [ ] `GET /bin/ticketing/users` as `test-dev` lists all QA and Developer users
- [ ] Create/update ticket with another group member as assignee succeeds
- [ ] No `Unable to obtain service resource resolver` errors in `error.log`

---

### Final Fix

Use a **service user** with read-only access to the user/group tree for assignee enumeration only.

#### 1. Repoinit — service user and ACLs

File: `ui.config/.../org.apache.sling.jcr.repoinit.RepositoryInitializer~ticketingApp.cfg.json`

```
create service user ticketing-user-reader
set ACL for ticketing-user-reader
  allow jcr:read on /home/users
  allow jcr:read on /home/groups
end
```

#### 2. OSGi — service user mapping

File: `ui.config/.../org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~ticketingApp.cfg.json`

```json
{
    "user.mapping": [
        "ticketingApp.core:ticketing-user-reader=[ticketing-user-reader]"
    ]
}
```

#### 3. Core — service resolver in `TicketUserServiceImpl`

- Inject `ResourceResolverFactory`
- Open resolver with subservice `ticketing-user-reader`
- Call `UserManager` / `Group.getMembers()` on the **service** resolver (try-with-resources)
- Leave `TicketUsersServlet` and servlet callers unchanged; elevation is internal to the service

**Design choice:** End users (`ticketing-qa`, `ticketing-developers`) do **not** get broad `/home/users` read. Only the dedicated service user does, limiting exposure to read-only user listing.

#### Deploy note

If repoinit already ran before this change, create `ticketing-user-reader` and its ACLs manually or re-run the updated repoinit script on the instance.

#### Related documentation

- Chat record: `ai-prompts/chat-assignee-users-service-user.md`
- Requirement: FR-10, FR-17 in `requirements-analysis.md`

---

## Issue 2 — Maven build fails: missing Jackson and Jackrabbit versions

### Problem

`mvn` failed while processing `core/pom.xml` before compilation:

```
'dependencies.dependency.version' for com.fasterxml.jackson.core:jackson-databind:jar is missing.
'dependencies.dependency.version' for org.apache.jackrabbit:jackrabbit-commons:jar is missing.
```

**Expected:** `core` module builds using the AEM 6.5.22 BOM / uber-jar like the rest of the archetype.

**Actual:** The project could not be read (`ProjectBuildingException`). Servlets used `com.fasterxml.jackson.databind.ObjectMapper` and `TicketServiceImpl` used `org.apache.jackrabbit.commons.JcrUtils`, but the extra dependencies had **no version** and were **not** in parent `<dependencyManagement>`.

**Affected components:**

- `core/pom.xml`
- `core/.../servlets/TicketCreateServlet.java` (and other ticket servlets using `ObjectMapper`)
- `core/.../services/impl/TicketServiceImpl.java` (`JcrUtils`)

---

### How I Investigated

1. **Reproduced from the terminal** — Maven reported both missing versions at `core/pom.xml` around the Jackson and Jackrabbit declarations.

2. **Checked parent `pom.xml`** — `uber-jar` is managed at `6.5.22` with `provided` scope. There is no `dependencyManagement` entry for `jackson-databind` or `jackrabbit-commons`. Jackrabbit coordinates in the parent POM are FileVault plugin usage, not those JARs.

3. **Checked how the APIs are used** — JSON writing is Jackson `ObjectMapper`; node create uses `JcrUtils.getOrCreateByPath`. Both APIs are available at compile time from the AEM uber-jar on a 6.5.22 project.

4. **Ruled out “just add latest versions”** — Pinning current Maven Central Jackson/Jackrabbit would risk mismatch with the OSGi bundles on AEM 6.5.22.

---

### How AI Helped

1. **Mapped the error** to unmanaged dependencies, not a missing Adobe public repository.

2. **Recommended the fix:** remove both extra `<dependency>` blocks and compile against `com.adobe.aem:uber-jar` already declared in `core/pom.xml`.

3. **Fallback if compile still failed:** add AEM-compatible versions only under root `<dependencyManagement>` with `provided` scope — do not pick arbitrary latest versions.

4. **Noted** the declarations also lacked `<scope>provided</scope>`, which would have been wrong even with versions (would try to embed APIs already on AEM).

---

### What I Validated

| Check | Result |
|-------|--------|
| Parent manages `jackson-databind` / `jackrabbit-commons` | No — that is why Maven required a version |
| `uber-jar` already on `core` | Yes, `6.5.22`, `provided` |
| Servlet JSON API | Jackson, not Gson (Gson was never added) |

**Post-fix validation (after POM change):**

- [ ] `mvn -pl core -am compile` succeeds without those two artifacts
- [ ] Ticket servlets still compile (`ObjectMapper`, `JcrUtils`)

---

### Final Fix

**Remove** the unversioned dependencies from `core/pom.xml`:

- `com.fasterxml.jackson.core:jackson-databind`
- `org.apache.jackrabbit:jackrabbit-commons`

Keep using `uber-jar` for compile-time APIs. Do not re-add those artifacts unless versions are defined in the parent POM.

#### Related documentation

- Chat record: `ai-prompts/chat-ticketing-implementation.md` (Turn 4)

---

## Issue 3 — Ticket components in CRX but not on pages or in the insert dialog

### Problem

`ticket-list`, `ticket-detail`, and `ticket-form` existed under `/apps/ticketingApp/components` in CRX, but they did not render on the ticket pages and did not appear when trying to drag/drop onto a page.

**Expected:** List/create/detail pages show the matching component; authors can insert them from the component browser on the page content container.

**Actual:** Pages looked empty of ticket UI. The component browser showed nothing useful when dropping on locked structure (Page Main / `root`).

**Affected components:**

- `ui.content/.../us/en/tickets/.content.xml`
- `ui.content/.../us/en/tickets/create/.content.xml`
- `ui.content/.../us/en/tickets/detail/.content.xml`
- Template: `conf/ticketingApp/settings/wcm/templates/page-content`
- Apps: `ui.apps/.../components/ticket-list|ticket-detail|ticket-form`

---

### How I Investigated

1. **Confirmed components exist in apps** — `.content.xml` uses `componentGroup="Ticket Management Application - Content"` (same pattern as `helloworld`). Vault filter already includes `/apps/ticketingApp/components`.

2. **Compared working home page vs ticket pages** — Home page (`us/en/.content.xml`) nests authored nodes under `jcr:content/root/container/container`. Ticket pages placed `ticket-list` / `ticket-form` / `ticket-detail` as **direct children of `root`**.

3. **Checked editable template structure** — `page-content` locks `root` (header XF, Page Main container, footer XF). Only the **nested** container has `editable="{Boolean}true"`. Structure containers do not render extra page-only children under `root`.

4. **Checked policies** — Inner container policy `policy_1574695586800` already allows `group:Ticket Management Application - Content`. Page Main policy has no `components` list. No allow-list change was required.

5. **Checked package filter** — `/content/ticketingApp` is `mode="merge"`, so a later package install would **not** move nodes already stored under `root`.

---

### How AI Helped

1. **Identified wrong JCR placement** as the render bug, not missing component definitions.

2. **Explained empty insert dialog** — dropping on locked `root` / Page Main uses a policy that does not expose the Content group the way the inner grid does.

3. **Specified the required path:** `jcr:content/root/container/container/<component>`.

4. **Applied the XML nesting** in Agent mode on all three ticket pages.

5. **Warned about merge:** delete `/content/ticketingApp/us/en/tickets` (or move nodes in CRXDE) after deploy so stale `root/ticket-*` nodes are not left behind.

---

### What I Validated

| Check | Result |
|-------|--------|
| Component `componentGroup` | Matches policy allow-list; defs are valid |
| Template policies | Already allow Content group on inner container |
| Ticket page XML before fix | Components under locked `root` — not rendered |
| Ticket page XML after fix | Nested under `root/container/container` |

**Post-fix validation (after deploy):**

- [ ] Tickets, Create, Detail pages show the HTL component
- [ ] Insert dialog on the **inner** responsive grid lists Ticket List / Form / Detail
- [ ] Stale `root/ticket-*` nodes removed or moved if merge left them in CRX

---

### Final Fix

Nest each ticket component under the template’s editable container (same shape as the English home page):

```xml
<root ...>
    <container sling:resourceType="ticketingApp/components/container">
        <container sling:resourceType="ticketingApp/components/container" layout="responsiveGrid">
            <ticket-list sling:resourceType="ticketingApp/components/ticket-list"/>
        </container>
    </container>
</root>
```

Use `ticket-form` and `ticket-detail` on the create and detail pages.

Authors must target the inner content container, not locked Page Main.

If the pages already exist on the instance, delete them and reinstall `ui.content`, or move the component nodes in CRXDE.

#### Related documentation

- Chat record: `ai-prompts/chat-ticketing-implementation.md` (Turn 5)
- Template structure: `ui.content/.../templates/page-content/structure/.content.xml`

---

## Issue 4 — Keyword search: where the logic lives and how it behaves

### Problem

Needed to debug **keyword search**: which layer filters tickets, which files implement it, and why a typed keyword might not match as a simple substring.

**Expected:** Typing in the list search box filters tickets by title/description (with optional status/priority filters) via the backend.

**Actual (as implemented and traced):** The browser does **not** filter a local list. Every keystroke (debounced) calls `GET /bin/ticketing/tickets/search`. Matching is done in JCR. The first implementation used JCR-SQL2 `CONTAINS` on `title` and `description`, which depends on Oak **full-text indexing**, not Java `String.contains()`. Ticket ID, assignee, and comments were not searched.

**Affected components:**

- `ui.apps/.../components/ticket-list/ticket-list.html` (`#ticket-keyword`)
- `ui.apps/.../clientlib-ticketing/js/ticket-list.js`
- `core/.../servlets/TicketSearchServlet.java`
- `core/.../services/TicketService.java` (`searchTickets`)
- `core/.../services/impl/TicketServiceImpl.java` (query)

---

### How I Investigated

1. **UI** — Search input `#ticket-keyword`; status/priority selects sit beside it.

2. **Frontend JS** — `fetchTickets()` builds `keyword`, `status`, `priority` query params, GET `/bin/ticketing/tickets/search`, 400ms debounce on `input`. Empty keyword still loads the list (filters only if set).

3. **Servlet** — `TicketSearchServlet.doGet()` reads the three parameters, maps status/priority to enums (`fromString` → null if blank/invalid), calls `ticketService.searchTickets(...)`.

4. **Service** — Original query: descendants of `/content/ticketingApp/tickets` with `ticketId` set; optional exact `status` / `priority`; if keyword present, `CONTAINS(title)` OR `CONTAINS(description)`; `ORDER BY created DESC`. Single quotes in the keyword escaped as `''`.

5. **Ruled out client-side search** — `ticket-list.js` only renders `data.tickets` from the JSON response.

---

### How AI Helped

1. **Traced the full path** from HTL → JS → servlet → `TicketServiceImpl`.

2. **Clarified `CONTAINS`** — Jackrabbit/Oak full-text, so empty results can mean missing/incorrect index, not a JS bug.

3. **Listed files that contain logic vs pass-through** — servlet does not tokenize keywords; JS does not match titles locally.

4. **Noted a follow-up** if `CONTAINS` is empty in AEM: switch to `LIKE` or QueryBuilder property `like` / in-memory filter.

---

### What I Validated

| Check | Result |
|-------|--------|
| Frontend filters tickets in memory | No — re-queries the servlet |
| Servlet search algorithm | Pass-through of `keyword` / filters |
| Fields searched (original) | `title` and `description` only |
| Status / priority | Exact property match when provided |

**Post-fix validation (on AEM):**

- [ ] Blank keyword lists tickets (subject to status/priority)
- [ ] Keyword matches title and description as expected for the query type in use
- [ ] Combined keyword + status/priority still returns the intersection

---

### Final Fix

No servlet change was required for “where is search?” — keep the chain:

1. `ticket-list.js` → `/bin/ticketing/tickets/search`
2. `TicketSearchServlet` → `TicketService.searchTickets`
3. Query implementation in `TicketServiceImpl.searchTickets()`

**Original chat implementation:** JCR-SQL2 `CONTAINS` on title/description.

**Current repo (if already updated):** AEM QueryBuilder predicates with `title`/`description` `like` (`%keyword%`), still under `/content/ticketingApp/tickets`, `ticketId` exists, optional status/priority, order by `created` desc. That is the substring-friendly replacement for `CONTAINS` if full-text index was the empty-result cause.

#### Related documentation

- Chat record: `ai-prompts/chat-ticketing-implementation.md` (Turn 6)
- API: `api-contract.md` (search endpoint)

