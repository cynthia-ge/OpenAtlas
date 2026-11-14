# OpenAtlas compatibility ledger

Baseline: empty directory, initialized as an independent repository. Guide: `../skills/open-atlas/SKILL.md` and its bundled inventory. This iteration establishes an API foundation, not complete product parity.

Target: **Jira Cloud REST v3**, primary documentation accessed 2026-11-13: [reference](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/). Tests exercise our implementation; no licensed vendor conformance capture was available.

| ID | Capability | Implemented behavior | Boundary / next work |
|---|---|---|---|
| OA-001 | Projects | Local provisioning API, key uniqueness | No Jira project schemes or roles |
| OA-002 | Issues | Create, read, edit, delete; project keys and numeric IDs | Task/Bug/Story; summary and bounded ADF description only |
| OA-003 | Workflow | Read and apply three local transitions | Not vendor workflow configuration |
| OA-004 | Comments | Persistent ADF comments; individual read/edit/delete; author/admin mutation checks | No mentions, visibility groups, notification delivery or project permission schemes |
| OA-005 | Search | Project equality, optional status equality; paging | Not complete JQL; offset tokens are local |
| OA-006 | Other Atlassian products | Missing | Confluence, JSM, Bitbucket, automation, marketplace and UI |

Cross-cutting foundation: persistent SQLite transactions, local bearer identity, tenant isolation, role checks, strict input validation, Docker deployment and exact coverage gates. Missing across the product: vendor OAuth/SSO, fine-grained ACLs, production migrations/backup tooling, distributed workers and vendor-identical UI. Coverage measures implemented Java, not the missing inventory.

Acceptance: valid lifecycle persists through app restart; invalid inputs fail without partial writes; tenant boundaries and roles hold; exact executable-line coverage includes every production Java class; branch counts are reported separately. Docker smoke exercises a real HTTP server. See [validation](validation.md).

## OA-004 continuation — comment resources, 13 November 2026

Starting HEAD `d83799d`, branch `main`, clean index and working tree. Inventory A2 (comments) and B4 (compatible API facade). Selected over broader JQL operators (OA-005, partial) and issue linking (OA-007, missing): completing the existing collaboration lifecycle supplies a small useful integration surface.

Contract evidence: [official Jira Cloud REST v3 issue comments](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-comments/), retrieved 2026-11-13. Individual GET/PUT return 200 with a comment; DELETE returns 204. The documented own/all comment permissions are mapped to local editor-author/admin roles. Local bearer authentication is always required; this is a bounded implementation, not licensed-instance conformance.

Implemented in `src/main/java/ge/cynthia/openatlas/ApiController.java`; acceptance in `src/test/java/ge/cynthia/openatlas/ContractTest.java` and `src/test/java/ge/cynthia/openatlas/ContractTest.java`. PUT accepts only a required ADF `body`, retains original author/created time, and records update author/time. Every operation resolves the issue and tenant before the comment; foreign issues and tenants return 404. Viewer/end-user mutation and another editor's comment mutation return 403. Unknown fields/query parameters and invalid ADF return 400 without changing the record. Existing stored comments remain readable/editable without a schema change.

Acceptance covers including restart persistence, list consistency, stable identity, owner/admin changes, authentication, tenant/issue boundaries, malformed bodies and issue-delete cleanup. The new tests failed against the prior implementation (missing routes). Current Java measurements and reproduction: [validation](validation.md).

## Java 25 migration

The runtime, HTTP acceptance harness and build now use Java 25 / Spring Boot 4.1.2. Existing persistence and implemented API behavior are retained. Current coverage and test results supersede earlier stack-specific measurements: see [validation](validation.md). This migration does not expand vendor-compatibility claims.
