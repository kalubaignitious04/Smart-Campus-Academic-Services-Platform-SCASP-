# Project management (Stage 0 material and group rules)

## Problem definition
Students and staff at a higher-education institution handle course registration, waiting lists and academic service requests
(transcripts, clearance, appeals) with slow, manual processes. SCASP gives students self-service and gives staff one place to manage
courses and requests and to see workload. Users: students (register, request, read notices) and academic/admin staff (manage and report).

## Requirements (prioritised)
Functional: FR1 login with roles (must); FR2 course catalogue with search and sort (must); FR3 register and withdraw (must); FR4 no duplicates, capacity enforced (must);
FR5 waiting list with automatic promotion (must); FR6 submit service requests (must); FR7 prioritise, assign and track requests (must);
FR8 announcements by audience (should); FR9 notifications on events (should); FR10 reports: capacity, waiting, workload, students (must);
FR11 audit trail (must); FR12 campus services with map link (should); FR13 staff manage courses (must); FR14 remote admin (RMI) (should).
Non-functional: NFR1 correct under concurrent use; NFR2 passwords hashed, SQL injection and XSS prevented; NFR3 clear error messages, no stack traces to users;
NFR4 builds from the README on a clean machine; NFR5 automated tests for every layer; NFR6 works on a phone-width screen; NFR7 data survives restart;
NFR8 orderly shutdown and timeouts on network calls.

## Roles and rotation (rotate after Stages 3 and 7)
| Period | Member A: architecture and integration | Member B: backend and data | Member C: client, quality, documentation |
|---|---|---|---|
| Stages 0-3 | name: | name: | name: |
| Stages 4-7 | (rotate) | (rotate) | (rotate) |
| Stages 8-11 | (rotate) | (rotate) | (rotate) |
A role is leadership, not ownership: all three write code. Every major change is reviewed by a different member before merging to `main`.
Use separate Git accounts, protect `main`, branch names `feature/<stage>-<topic>`, small commits with clear messages.

## Contribution log (fill in as you go)
| Date | Task | Owner | Reviewer | Evidence (commit / PR) |
|---|---|---|---|---|

## Risk register
| Risk | Likelihood | Impact | Response |
|---|---|---|---|
| Stage 4 (EJB) and Stage 10 (Android) need tools not set up yet | High | High | Install a Jakarta EE server and Android Studio now; assign owners |
| One member cannot explain submitted code (marks adjusted) | Medium | High | Rotate roles, pair on reviews, each member presents a module |
| Obsolete technology (RMI, CORBA, EJB) confuses | Medium | Medium | Keep scope small; dossier for CORBA; ask lecturer early |
| Integration breaks late | Medium | High | Single build (`./build.sh`) and `./test.sh` before every merge |
| Team availability / deadline | Medium | High | Stage plan below; weekly check-in |
| Secrets or real data committed | Low | High | `.gitignore`, fictional data only, review before submit |
| Demo password and open ports on a shared network | Medium | Medium | Defaults bind to 127.0.0.1; change demo password before any real use |

## Traceability (unit -> evidence)
| Unit | Code | Tests | Document |
|---|---|---|---|
| 1 Collections | `core-domain` | `CoreTest` | architecture.md (collection table) |
| 2 Multithreading | `concurrent-processing` | `ProcessorTest` | race, thread dump, benchmark files |
| 3 Networking | `network-gateway` | `GatewayTest` | protocols.md |
| 4 EJB | not built | - | `enterprise-services/README.md` |
| 5 JDBC | `persistence-jdbc`, `database` | `JdbcTest` | architecture.md |
| 6 Servlets | `web-application` (java) | `WebTest` | architecture.md |
| 7 JSP | `webapp/WEB-INF` | `WebTest` | usability test: not done |
| 8 RMI | `rmi-administration` | `RmiTest` | protocols.md |
| 9 CORBA | - | - | `corba-migration/` |
| 10 Android | not built | - | `android-client/README.md` |
