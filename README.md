# Ticket Management Application

An Adobe Experience Manager (AEM 6.5.22) application for creating, tracking, and resolving support tickets. Tickets are stored as JCR content nodes and managed through a browser UI backed by JSON REST-style Sling servlets.

| Property | Value |
|----------|-------|
| **GroupId** | `com.ai.ticketingApp` |
| **ArtifactId** | `ticketingApp` |
| **Version** | `1.0.0-SNAPSHOT` |
| **AEM Version** | 6.5.22 |
| **Java** | 8 |

## Features

- Create tickets with title, description, priority, and assignee
- Search and filter tickets by keyword, status, and priority
- View ticket details with comments and status history
- Update ticket fields and change status via a defined workflow
- Role-based access for QA and Developer user groups
- CSRF-protected POST mutations

### Status Workflow

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
  ↓         ↓
CANCELLED  CANCELLED
```

### Priorities

`LOW` · `MEDIUM` · `HIGH` · `CRITICAL`

## Architecture

```text
Browser (HTL pages)
    ↓ Fetch JSON
Sling Servlets (/bin/ticketing/*)
    ↓
OSGi Services (TicketService, TicketStateService, …)
    ↓
JCR Repository (/content/ticketingApp/tickets)
```

| Layer | Technology | Module |
|-------|-----------|--------|
| Frontend | HTL, vanilla JavaScript, CSS | `ui.apps`, `ui.frontend` |
| Backend | Java 8, OSGi, Apache Sling | `core` |
| Data | JCR (Jackrabbit Oak) | AEM platform |
| Config | OSGi, repoinit ACLs | `ui.config` |
| Content | Pages, templates | `ui.content` |

## Modules

| Module | Description |
|--------|-------------|
| [core](core/) | OSGi bundle — servlets, services, Sling models, validation, state machine |
| [ui.apps](ui.apps/) | AEM components, HTL templates, clientlibs (`clientlib-ticketing`) |
| [ui.content](ui.content/) | Site pages, templates, experience fragments |
| [ui.config](ui.config/) | Runmode OSGi configs, repoinit scripts, service user mappings |
| [ui.frontend](ui.frontend/) | Webpack/SCSS build for site-wide Core Component styling |
| [ui.apps.structure](ui.apps.structure/) | Repository structure package |
| [it.tests](it.tests/) | Server-side integration tests (Failsafe) |
| [ui.tests](ui.tests/) | Cypress UI tests |
| [all](all/) | Container content package for deployment |
| [dispatcher](dispatcher/) | Apache Dispatcher configuration |

## UI Pages

| Page | Path |
|------|------|
| Ticket list | `/content/ticketingApp/us/en/tickets.html` |
| Create ticket | `/content/ticketingApp/us/en/tickets/create.html` |
| Ticket detail | `/content/ticketingApp/us/en/tickets/detail.html?ticketPath=...` |

## API Endpoints

All endpoints require authentication. POST requests require a Granite CSRF token.

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/bin/ticketing/tickets/search` | Search/filter tickets |
| GET | `/bin/ticketing/ticket/detail` | Get ticket by path |
| POST | `/bin/ticketing/ticket/create` | Create a ticket |
| POST | `/bin/ticketing/ticket/update` | Update ticket fields |
| POST | `/bin/ticketing/ticket/status` | Change ticket status |
| POST | `/bin/ticketing/ticket/comment` | Add a comment |
| GET | `/bin/ticketing/users` | List assignable users |

See [api-contract.md](api-contract.md) for full request/response schemas and error codes.

## Security

Repoinit scripts create two AEM groups with differentiated permissions:

| Group | Permissions on `/content/ticketingApp/tickets` |
|-------|-----------------------------------------------|
| `ticketing-qa` | Read, create, and modify tickets |
| `ticketing-developers` | Read and modify tickets (cannot create) |

Anonymous users are denied read access to ticket data. Authentication is required for all ticket pages and `/bin/ticketing` endpoints.

## Prerequisites

- Java 8 JDK
- Maven 3.3.9+
- Adobe Experience Manager 6.5.22 (author on `localhost:4502`, publish on `4503`)
- Node.js v16.17.0 and npm 8.15.0 (for `ui.frontend` build)

## Build & Deploy

Build all modules:

```bash
mvn clean install
```

Build and deploy the full package to a local AEM author instance:

```bash
mvn clean install -PautoInstallSinglePackage
```

Deploy to publish:

```bash
mvn clean install -PautoInstallSinglePackagePublish
```

Deploy only the OSGi bundle:

```bash
mvn clean install -PautoInstallBundle
```

Build frontend assets (from `ui.frontend`):

```bash
cd ui.frontend
npm install
npm run prod
```

## Testing

### Unit tests

```bash
mvn clean test
```

16 test classes in `core` cover all ticket servlets and models (JUnit 5, Mockito, AEM Mock).

### Integration tests

Requires a running AEM instance:

```bash
mvn clean verify -Plocal
```

| Property | Default |
|----------|---------|
| `it.author.url` | `http://localhost:4502` |
| `it.author.user` | `admin` |
| `it.author.password` | `admin` |
| `it.publish.url` | `http://localhost:4503` |

### UI tests

Cypress tests in the `ui.tests` module. See [ui.tests/README.md](ui.tests/README.md) for Docker-based execution.

See [test-strategy.md](test-strategy.md) for full test scope and coverage details.

## Project Documentation

| Document | Description |
|----------|-------------|
| [requirements-analysis.md](requirements-analysis.md) | Functional/non-functional requirements, assumptions, edge cases |
| [acceptance-criteria.md](acceptance-criteria.md) | Verifiable acceptance checklists |
| [implementation-plan.md](implementation-plan.md) | Build phases, milestones, risks |
| [design-notes.md](design-notes.md) | Architecture, frontend/backend/database design |
| [api-contract.md](api-contract.md) | Full API endpoint documentation |
| [test-strategy.md](test-strategy.md) | Test scope, frameworks, and coverage |

## ClientLibs

| ClientLib | Category | Contents |
|-----------|----------|----------|
| `clientlib-ticketing` | `ticketingApp.ticketing` | Ticket UI JS and CSS |
| `clientlib-base` | `ticketingApp.base` | Core WCM component clientlibs |
| `clientlib-site` | `ticketingApp.site` | Webpack-generated site SCSS |

The `ui.frontend` module builds site assets via Webpack and copies them into `ui.apps` using [aem-clientlib-generator](https://github.com/wcm-io-frontend/aem-clientlib-generator).

## Maven Repository

This project uses the Adobe public Maven repository. To configure it in your Maven settings, see:

https://experienceleague.adobe.com/docs/experience-manager-65/deploying/deploying/custom-manufacturing-install-reference-material.html

## License

Copyright 2015 Adobe Systems Incorporated. Licensed under the Apache License, Version 2.0.
