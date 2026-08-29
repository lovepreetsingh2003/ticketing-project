# API Contract

All endpoints require authenticated AEM users. POST endpoints require a Granite CSRF token in the `CSRF-Token` request header and use `Content-Type: application/x-www-form-urlencoded`. All responses use `Content-Type: application/json;charset=UTF-8`.

### Shared Ticket Object

Returned by search and detail endpoints:

```json
{
  "ticketId": "TKT-AB12CD34",
  "path": "/content/ticketingApp/tickets/TKT-AB12CD34",
  "title": "Login page broken on mobile",
  "description": "Users cannot log in from iOS Safari.",
  "priority": "HIGH",
  "status": "OPEN",
  "assignee": "john.doe",
  "reporter": "jane.qa",
  "created": 1690000000000,
  "updated": 1690000000000,
  "comments": [
    { "author": "john.doe", "body": "Investigating now.", "created": 1690001000000 }
  ],
  "statusHistory": [
    { "user": "jane.qa", "from": "OPEN", "to": "IN_PROGRESS", "created": 1690000500000 }
  ]
}
```

### Shared Error Shape

```json
{ "error": "Human-readable error message" }
```

---

## Endpoint: Search Tickets

**Method:** GET  
**Path:** `/bin/ticketing/tickets/search`  
**Purpose:** Search and filter tickets by keyword, status, and priority. Results ordered by creation date descending.

### Request

Query parameters (all optional):

| Parameter | Type | Description |
|-----------|------|-------------|
| `keyword` | string | LIKE search on `title` OR `description` |
| `status` | string | Filter by status: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED` |
| `priority` | string | Filter by priority: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |

Example: `GET /bin/ticketing/tickets/search?keyword=login&status=OPEN&priority=HIGH`

### Response

**200 OK:**
```json
{
  "tickets": [ { /* ticket object */ } ],
  "total": 3
}
```

### Validation Rules

- All parameters are optional; omitting all returns all tickets.
- Invalid `status` or `priority` values are silently ignored (treated as no filter).

### Error Responses

| Status | Body |
|--------|------|
| 500 | `{ "error": "Search failed: <message>" }` |

---

## Endpoint: Get Ticket Detail

**Method:** GET  
**Path:** `/bin/ticketing/ticket/detail`  
**Purpose:** Retrieve a single ticket with comments and status history.

### Request

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ticketPath` | Yes | Full JCR path, e.g. `/content/ticketingApp/tickets/TKT-AB12CD34` |

Example: `GET /bin/ticketing/ticket/detail?ticketPath=/content/ticketingApp/tickets/TKT-AB12CD34`

### Response

**200 OK:** Full ticket object (see shared ticket object above).

### Validation Rules

- `ticketPath` must not be blank.

### Error Responses

| Status | Body |
|--------|------|
| 400 | `{ "error": "ticketPath parameter is required" }` |
| 404 | `{ "error": "Ticket not found: <path>" }` |
| 500 | `{ "error": "Failed to load ticket: <message>" }` |

---

## Endpoint: Create Ticket

**Method:** POST  
**Path:** `/bin/ticketing/ticket/create`  
**Purpose:** Create a new ticket with auto-generated ID and initial status `OPEN`.

### Request

Form parameters:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `title` | Yes | Max 200 characters |
| `description` | Yes | Max 5000 characters |
| `priority` | Yes | `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL` |
| `assignee` | Yes | Must be a member of `ticketing-qa` or `ticketing-developers` |
| `reporter` | Auto | Set from logged-in user session (not sent by client) |

Example body: `title=Login+broken&description=Cannot+login&priority=HIGH&assignee=john.doe`

### Response

**201 Created:**
```json
{
  "success": true,
  "path": "/content/ticketingApp/tickets/TKT-AB12CD34",
  "ticketId": "TKT-AB12CD34"
}
```

### Validation Rules

- Title required, max 200 characters.
- Description required, max 5000 characters.
- Priority required, must be valid enum value.
- Assignee required, must be assignable user.
- Reporter derived from session; must not be blank.

### Error Responses

| Status | Body |
|--------|------|
| 400 | `{ "error": "Title is required" }` |
| 400 | `{ "error": "Title must not exceed 200 characters" }` |
| 400 | `{ "error": "Description is required" }` |
| 400 | `{ "error": "Description must not exceed 5000 characters" }` |
| 400 | `{ "error": "Priority is required" }` |
| 400 | `{ "error": "Invalid priority value: X. Valid values: LOW, MEDIUM, HIGH, CRITICAL" }` |
| 400 | `{ "error": "Assignee is required" }` |
| 400 | `{ "error": "Assignee must be a QA or Developer user" }` |
| 500 | `{ "error": "Failed to create ticket: <message>" }` |

---

## Endpoint: Update Ticket

**Method:** POST  
**Path:** `/bin/ticketing/ticket/update`  
**Purpose:** Update ticket title, description, priority, and assignee.

### Request

Form parameters:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ticketPath` | Yes | Full JCR path to the ticket |
| `title` | Yes | Max 200 characters |
| `description` | Yes | Max 5000 characters |
| `priority` | No | Valid enum if provided |
| `assignee` | No | Must be assignable user if provided |

Example body: `ticketPath=/content/ticketingApp/tickets/TKT-AB12CD34&title=Updated+title&description=Updated+desc&priority=MEDIUM&assignee=john.doe`

### Response

**200 OK:**
```json
{ "success": true }
```

### Validation Rules

- `ticketPath` required.
- Title required, max 200 characters.
- Description required, max 5000 characters.
- Priority validated if provided.
- Assignee validated via `isAssignableUser()` if provided.

### Error Responses

| Status | Body |
|--------|------|
| 400 | `{ "error": "ticketPath parameter is required" }` |
| 400 | Validation error messages (same as create) |
| 400 | `{ "error": "Assignee must be a QA or Developer user" }` |
| 404 | `{ "error": "<not found message>" }` |
| 500 | `{ "error": "Failed to update ticket: <message>" }` |

---

## Endpoint: Change Ticket Status

**Method:** POST  
**Path:** `/bin/ticketing/ticket/status`  
**Purpose:** Transition ticket status following the state machine. Records status history entry.

### Request

Form parameters:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ticketPath` | Yes | Full JCR path to the ticket |
| `status` | Yes | Target status enum value |

Example body: `ticketPath=/content/ticketingApp/tickets/TKT-AB12CD34&status=IN_PROGRESS`

### Response

**200 OK:**
```json
{
  "success": true,
  "status": "IN_PROGRESS"
}
```

### Validation Rules

- `ticketPath` and `status` required.
- `status` must be a valid enum: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.
- Transition must be allowed by state machine:

| Current Status | Allowed Next Status |
|----------------|---------------------|
| `OPEN` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED`, `CANCELLED` |
| `RESOLVED` | `CLOSED` |
| `CLOSED` | *(none — terminal)* |
| `CANCELLED` | *(none — terminal)* |

### Error Responses

| Status | Body |
|--------|------|
| 400 | `{ "error": "ticketPath parameter is required" }` |
| 400 | `{ "error": "status parameter is required" }` |
| 400 | `{ "error": "Invalid status value: X" }` |
| 404 | `{ "error": "<not found message>" }` |
| 409 | `{ "error": "Cannot transition from OPEN to CLOSED" }` |
| 500 | `{ "error": "Failed to change status: <message>" }` |

---

## Endpoint: Add Comment

**Method:** POST  
**Path:** `/bin/ticketing/ticket/comment`  
**Purpose:** Append a comment to a ticket. Author is set from the logged-in user.

### Request

Form parameters:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ticketPath` | Yes | Full JCR path to the ticket |
| `body` | Yes | Comment text |
| `author` | Auto | Set from logged-in user session (not sent by client) |

Example body: `ticketPath=/content/ticketingApp/tickets/TKT-AB12CD34&body=Investigating+now`

### Response

**201 Created:**
```json
{
  "success": true,
  "ticketPath": "/content/ticketingApp/tickets/TKT-AB12CD34",
  "author": "john.doe"
}
```

### Validation Rules

- `ticketPath` and `body` required.
- Author derived from session.

### Error Responses

| Status | Body |
|--------|------|
| 400 | `{ "error": "ticketPath parameter is required" }` |
| 400 | `{ "error": "Comment body is required" }` |
| 404 | `{ "error": "<not found message>" }` |
| 500 | `{ "error": "Failed to add comment: <message>" }` |

---

## Endpoint: List Assignable Users

**Method:** GET  
**Path:** `/bin/ticketing/users`  
**Purpose:** Return users eligible for ticket assignment (members of `ticketing-qa` and `ticketing-developers` groups).

### Request

No parameters.

Example: `GET /bin/ticketing/users`

### Response

**200 OK** (with `Cache-Control: no-store`):

```json
[
  { "userId": "john.doe", "displayName": "john.doe" },
  { "userId": "jane.qa", "displayName": "jane.qa" }
]
```

### Validation Rules

None.

### Error Responses

No explicit error handling in servlet; service-level failures may result in an empty array or 500.

---

## Authentication & CSRF

| Requirement | Detail |
|-------------|--------|
| Authentication | Required for all `/bin/ticketing/*` endpoints via `TicketingAuthenticationRequirements` |
| CSRF | POST endpoints require `CSRF-Token` header obtained from `/libs/granite/csrf/token.json` |
| Content-Type (POST) | `application/x-www-form-urlencoded` |
