# Architecture

## Components
```
 Browser ──HTTP──► [AuthFilter, EncodingFilter] ─► Servlets ─► JSP views (+ tag files)
 Mobile ──JSON───►                                   │
 TCP client ──────► GatewayServer ───────────────────┤
 RMI admin client ► AdminServer ─────────────────────┤
                                                     ▼
                      RequestProcessor (bounded pool / virtual threads)
                                                     ▼
                      AcademicServices  (all business rules, per-course locks, audit, notifications)
                                                     ▼
                      CampusStore (interface) ─┬─ InMemoryStore  (collections, Stage 1 + tests)
                                               └─ JdbcStore ──► H2 database (pooled by Tomcat DBCP)
 NotificationBus ──► UdpNotifier ──UDP──► listeners        NotificationBus ──► dashboard "Recent activity"
```
Everything runs in one JVM started by `scasp.web.WebServer` (embedded Tomcat 10.1). `AppContextListener` starts and stops
the pool, gateway, UDP notifier and RMI server together with the web application.

## Stage-to-module map
| Unit | Where |
|---|---|
| 1 Collections | `core-domain` (`Model`, `InMemoryStore`, `AcademicServices`) |
| 2 Multithreading | `concurrent-processing` |
| 3 Networking | `network-gateway` |
| 5 JDBC | `persistence-jdbc`, `database/*.sql` |
| 6 Servlets | `web-application/.../scasp/web/*Servlet`, `AuthFilter`, `EncodingFilter`, `AppContextListener` |
| 7 JSP | `web-application/src/main/webapp/WEB-INF/views`, `WEB-INF/tags` |
| 8 RMI | `rmi-administration` |
| 9 CORBA | `docs/corba-migration` |

## Collection selection (Stage 1)
| Data | Collection | Why |
|---|---|---|
| Courses by code | `TreeMap<String,Course>` | unique key, always sorted by code |
| Users, students | `LinkedHashMap` / `HashMap` | fast lookup by id |
| Registrations | `LinkedHashSet<Registration>` | `equals` = (student, course), so duplicates cannot exist; keeps insertion order |
| Waiting list per course | `Deque<WaitlistEntry>` (`ArrayDeque`) | first come, first promoted |
| Service requests | `PriorityQueue<ServiceRequest>` with `URGENCY` comparator | most urgent, then oldest, first |
| Sorting courses | `Comparator` strategies (code, title, seats left) | chosen by the `sort` parameter |
| Reports | streams + `Collectors.groupingBy` / `counting` | counts by status and by assignee |
| Service boundary | `List.copyOf`, `toList()`, `unmodifiableList` | callers get read-only copies |

## Domain invariants (each has a test)
* A student is registered or waiting for a course at most once.
* Registrations never exceed capacity; extra students queue in order; a free seat promotes the first waiting student.
* Course status: OPEN <-> CLOSED, either -> CANCELLED (final). Capacity cannot drop below current registrations.
* Request status moves forward one step (SUBMITTED, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED); RESOLVED can reopen; must be assigned first.
* Students act only for themselves; staff-only operations are checked in the service, not only in the web layer.

## Concurrency design note (Stage 2)
* Business actions run on `RequestProcessor`: platform mode = fixed workers + bounded `ArrayBlockingQueue` + `AbortPolicy` (back-pressure:
  caller gets "server is busy"); virtual mode = one virtual thread per task, bounded by a `Semaphore`.
* `AcademicServices` takes one `ReentrantLock` per course around "check capacity then register/promote", and runs the steps in `store.atomic`.
  This is what stops overbooking, in memory and in the database (see `JdbcTest`, `ProcessorTest`).
* Statistics use `LongAdder`. `shutdown()` stops intake, waits, then interrupts. Timeouts cancel (interrupt) the task.
* Evidence: `docs/testing/race-condition-evidence.txt` (unlocked code admitted 16 and 20 seats for a capacity of 5; locked code always 5),
  `thread-dump.txt`, `platform-vs-virtual.txt`.

## Persistence notes (Stage 5)
* Timestamps are `BIGINT` epoch milliseconds, so every database behaves the same.
* `JdbcStore.atomic` opens one transaction shared by all calls on the thread; rollback on any exception (`JdbcTest`).
* All user values go through `PreparedStatement` parameters. Passwords are salted PBKDF2 hashes.

## Web notes (Stages 6 and 7)
* MVC: servlet (controller) -> request attributes -> JSP (view). Pages contain no Java code; two tag files (`each.tag`, `when.tag`) hold the only loop/if scriptlets.
* **JSTL not used** (jar unavailable offline). To switch: add `jakarta.servlet.jsp.jstl` + an implementation to the classpath, replace
  `<t:each items="${x}" var="r">` by `<c:forEach items="${x}" var="r">` and `<t:when test=...>` by `<c:if test=...>`.
* Output is escaped with `${f:h(...)}` (function in `scasp.tld`). Domain objects are Java records, which Tomcat 10.1's EL cannot read,
  so `RecordELResolver` is registered at startup.
* Security: PBKDF2 passwords, new session on login, HttpOnly cookie, 20 min timeout, CSRF token on every POST form, role checks in filter and servlet,
  UTF-8 and security headers, Post/Redirect/Get, status codes 400/401/403/404/409/503.
* Cookie preference was not added (none needed).

## Deployment
One machine: `./scasp.sh web`. Ports 8080 (HTTP), 9090 (TCP), 9091 (UDP target), 1099 (RMI), all bound to 127.0.0.1 unless `scasp.bind` is set.
There is no TLS: run on a trusted network only.
