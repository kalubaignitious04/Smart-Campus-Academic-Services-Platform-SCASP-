# SCASP - Smart Campus Academic Services Platform (BoCS3100 Advanced Java)

One system, built up in stages: students register for courses (with waiting lists), raise service requests and read
announcements; staff manage courses, process requests and read reports. The same business rules are reached through a
**web UI**, a **TCP gateway**, a **JSON API** and an **RMI admin client**, all on one database.

## Run it (Java 21 JDK, no internet needed: every library is in `lib/`)
```
./build.sh          # Windows: build.bat
./scasp.sh web      # Windows: scasp.bat web      ->  http://localhost:8080
./test.sh           # Windows: test.bat           ->  43 automated tests
```
Demo accounts (all fictional). Password for every account: `Demo#2026`

| User | Role |
|---|---|
| S1001 to S1005 | student |
| T2001 | academic staff |
| A3001 | admin |

Starting the web server also starts: TCP gateway (9090), UDP notifications (to 9091), RMI registry (1099).
The database is `data/scasp` (H2 file, created on first start; delete the folder to reset).

Other commands: `./scasp.sh gateway`, `./scasp.sh udp`, `./scasp.sh admin localhost 1099 capacity`,
`./scasp.sh race-demo`, `./scasp.sh benchmark`.
Settings are Java `-D` properties or environment variables (e.g. `scasp.web.port` / `SCASP_WEB_PORT`):
`scasp.web.port`, `scasp.gateway.port`, `scasp.udp.port`, `scasp.rmi.port`, `scasp.db.url`, `scasp.workers`, `scasp.queue`,
`scasp.virtual`, `scasp.bind` (default 127.0.0.1), `scasp.admin.token` (default random, printed at start).

## Layout (matches the brief's suggested structure)
| Folder | Stage | Contents |
|---|---|---|
| `core-domain` | 1 | Records, rules (`AcademicServices`), collections store (`InMemoryStore`), store interface |
| `concurrent-processing` | 2 | `RequestProcessor` (bounded pool / virtual threads), race demo, benchmark, thread dump |
| `network-gateway` | 3 | TCP server/client, UDP notifier, authorised URL fetch |
| `persistence-jdbc` | 5 | `JdbcStore` (PreparedStatement, transactions, DBCP pool) |
| `web-application` | 6, 7 | Servlets, filters, listener, JSP views, tag files, JSON API |
| `rmi-administration` | 8 | Remote interface, server, command-line client |
| `database` | 5 | `schema.sql`, `sample-data.sql` |
| `docs` | all | Architecture, protocols, CORBA dossier, testing, user guide, project management |
| `enterprise-services`, `android-client` | 4, 10 | **Not built yet** - see the README in each folder |

All modules compile together into `out/classes` (one `javac` run); folders are separate by source, not by Maven module.

## Status - read this before submitting
* Built and tested here: Stages 1, 2, 3, 5, 6, 7 (web), 8, and the Stage 9 dossier (documents only).
* **Stage 4 (EJB) and Stage 10 (Android) are not included.** They need a Jakarta EE server and the Android SDK,
  which were not available when this was built, so nothing could be run or verified. Plans are in their folders.
* Stage 7 usability test with two outside students has **not** been done - template in `docs/testing/testing.md`.
* JSTL is not used: its jar could not be fetched offline, so views use two small tag files (`each`, `when`). The brief
  allows "approved tag libraries": **get lecturer approval, or swap in JSTL** (`c:forEach`, `c:if`) - see `docs/architecture/architecture.md`.
* No Maven/Gradle file: build is `javac` via scripts with bundled jars. If your lecturer requires Maven or Gradle, add one.
* Windows `.bat` scripts were written but not run.
* Library jars in `lib/` are the Ubuntu/Debian packages of H2 2.2.220, Tomcat 10.1.55, Eclipse JDT core 3.32, JUnit console 1.9.1.
  Check your licence/submission rules about committing jars.

## Group work
Roles, rotation, review rule, contribution log and risk register: `docs/project-management.md`.
Everyone must be able to explain the code they submit. Sample data is fictional; never commit real student data or secrets.
