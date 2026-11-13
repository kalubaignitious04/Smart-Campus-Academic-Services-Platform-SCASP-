# Testing

Run everything: `./test.sh` (43 tests, about 15 seconds). Latest output: `test-report.txt`.

| Area | Test class | What it proves |
|---|---|---|
| Collections and rules | `CoreTest` | equality/duplicates, waitlist order and promotion, capacity, status transitions, ordering, invalid operations, read-only copies, hashed logins, audience filter |
| Concurrency | `ProcessorTest` | 40 simultaneous registrations for 5 seats give exactly 5 (platform and virtual threads); concurrent withdrawals; full queue rejects; timeout interrupts; shutdown. Uses latches, no sleeps for correctness |
| Network | `GatewayTest` | commands, login/register/withdraw, malformed and oversized lines, 3 failed logins, rude disconnect, idle timeout, server shutdown, 20 concurrent clients, UDP datagram, fetcher allow-list and failure |
| Persistence | `JdbcTest` | scripts repeatable, same rules as memory, commit, rollback, constraints, SQL injection text stored as data, 20 concurrent registrations |
| Web | `WebTest` | real Tomcat+JSP: login/logout, redirect when not logged in, CSRF 403, role 403, 400/404 handling, registration and waiting list in the UI, requests and announcements flow, HTML escaping, JSON API |
| RMI | `RmiTest` | reports, token check, bad value, server unavailable |

## Evidence files
* `race-condition-evidence.txt` - five runs of `./scasp.sh race-demo`: unlocked version admitted 16 or 20 seats for capacity 5; locked version always 5.
* `thread-dump.txt` - taken while 16 workers were busy (`./scasp.sh benchmark docs/testing/thread-dump.txt`).
* `platform-vs-virtual.txt` - 2000 tasks that each wait 20 ms: 16-thread pool 2558 ms, virtual threads 126 ms. This is a blocking-I/O workload; it says nothing about CPU-bound work.

## Manual tests to record (screenshots) - not done yet
1. Student: login, register until a course is full, join waiting list, withdraw, see promotion. 2. Staff: close a course, assign and advance a request, publish an announcement, read reports.
3. Direct URL access while logged out and as a student to `/reports`. 4. Browser with JavaScript off (the UI does not need it). 5. Phone-width window.
6. Clean machine: unzip, `./build.sh`, `./scasp.sh web`, restart and check data persisted.

## Stage 7 usability test (template - to be done by the group)
Two students outside the group each try: register for a course, submit a request, find a campus service. Record time, mistakes, comments, and the change you made.

| Tester | Task | Time | Problems | Improvement made |
|---|---|---|---|---|
| | | | | |
