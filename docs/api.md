# OpenAtlas API

`POST /api/openatlas/projects` accepts `key` and `name` (admin only).
`POST /rest/api/3/issue` accepts `fields` with `project: {key}`, `issuetype: {name}`, `summary`, optional `description`.
`GET|PUT|DELETE /rest/api/3/issue/{idOrKey}` reads, updates summary/description through `fields`, or deletes.
`GET|POST /rest/api/3/issue/{idOrKey}/transitions` lists transitions or accepts `transition: {id}`. IDs 11, 21, 31 represent To Do, In Progress, Done.
`GET|POST /rest/api/3/issue/{idOrKey}/comment` lists comments or accepts `body` as ADF.
`GET /rest/api/3/search/jql?jql=project%20%3D%20DEMO` supports optional `AND status = "Done"`, `maxResults` (1–100), `nextPageToken`.
ADF supports version 1 documents containing paragraphs of text nodes. Unsupported structures fail. All project records are shared within a tenant; editors write and viewers read.

Requests require `Authorization: Bearer <token>`. `/healthz` is public. Unknown/repeated query parameters and unknown body fields are rejected. Authentication uses local TOKENS configuration, not vendor OAuth. Transactions commit only successful requests.

`GET|PUT|DELETE /rest/api/3/issue/{idOrKey}/comment/{commentId}` reads (200), replaces the required ADF `body` (200), or deletes (204). Editors may modify their own comments; administrators may modify any tenant comment. Viewers and end users cannot modify comments, including previously authored ones. PUT preserves `author` and `created`, adding `updateAuthor` and `updated`. Unknown issue/comment combinations return 404. No visibility groups, properties, expand, notifications or overrideEditableFlag are supported; unsupported input is rejected.
