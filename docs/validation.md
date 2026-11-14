# Java 25 validation — 13 November 2026

`./mvnw clean verify` passes **26 JUnit tests**, with zero failures, errors or skipped tests. Java 25 and Spring Boot 4.1.2 are pinned; Maven 3.9.16 is supplied by the checksum-pinned wrapper. These measurements describe the Java migration and supersede earlier stack-specific counts.

| Production Java metric | Covered / total |
|---|---:|
| Executable lines | 388/388 |
| Methods | 58/58 |
| Branch outcomes | 173/180 |

Every production Java class is instrumented, including the application entry point and startup/configuration paths. JaCoCo 0.8.16 enforces an exact executable-line ratio of 1.0 without exclusions, ignore annotations or rounded-percentage gates. All methods are exercised. Branch coverage is reported honestly above and is not 100%; this migration does not claim full branch coverage.

| Source file | Executable lines | Methods |
|---|---:|---:|
| `ApiController.java` | 192/192 | 16/16 |
| `Core.java` | 193/193 | 40/40 |
| `Application.java` | 3/3 | 2/2 |

Acceptance runs through actual Spring Boot HTTP servers with synthetic local credentials and temporary SQLite files. It covers the existing product workflows, negative input, authorization/tenant isolation, persistence, transactional rollback and restart compatibility. Gzipped contract fixtures preserve local application behavior; they are not vendor-instance conformance captures. Dynamic IDs/timestamps are normalized and error wording may vary while status/shape/code contracts remain asserted. Additional Java tests cover configuration, parsing and boundary behavior.

The Docker image builds and starts as UID 10001. HTTP readiness, representative record creation and persistence across application-container replacement pass using a disposable named volume. Existing application volumes are untouched. CI runs the same Maven verification command.

Reports: `target/site/jacoco/index.html`, `target/site/jacoco/jacoco.xml`, and `target/surefire-reports/`. `clean` removes stale reports before instrumentation. Passing tests and execution coverage establish the documented implemented subset; they do not prove complete vendor parity, missing inventory coverage, production load capacity or enterprise readiness.
