# Protocols

## TCP gateway (default port 9090) - one UTF-8 line per message, ending in `\n`, max 512 characters
Replies: `OK|<payload>` or `ERR|<CODE>|<message>`. Lists use `;` between items and `,` between fields (those characters are removed from data).
Idle connections are closed after 60 s. Three failed logins close the connection. A password cannot contain spaces.

| Command | Reply |
|---|---|
| `PING` | `OK|PONG` |
| `INFO` | `OK|host=..;address=..;port=..` (uses `InetAddress`) |
| `STATUS` | `OK|web=UP` (server fetches its own `/health` through the allow-listed fetcher) or `OK|web=DOWN` |
| `COURSES` | `OK|code,title,capacity,registered,waiting,status;...` |
| `COURSE <code>` | one course row |
| `LOGIN <userId> <password>` | `OK|name,role` |
| `ANNOUNCEMENTS` | needs login; `OK|title,body;...` |
| `REGISTER <code>` / `WITHDRAW <code>` | needs login; `OK|REGISTERED`, `OK|WAITLISTED`, `OK|WITHDRAWN`, `OK|LEFT_WAITLIST` |
| `MYREG` | `OK|code,state;...` |
| `QUIT` | `OK|BYE` |

Error codes: `BAD_REQUEST`, `UNKNOWN_COMMAND`, `AUTH_REQUIRED`, `AUTH_FAILED`, `NOT_FOUND`, `CONFLICT`, `FORBIDDEN`, `INVALID`, `BUSY`, `UNAVAILABLE`, `SERVER_ERROR`.

Example:
```
> LOGIN S1004 Demo#2026      < OK|Kondwani Demo,STUDENT
> REGISTER CS210             < OK|REGISTERED
> REGISTER CS210             < ERR|CONFLICT|Already registered or on the waiting list
```

## UDP notifications (target localhost:9091)
One datagram per event: `SCASP|<TYPE>|<message>` (types: REGISTERED, WAITLISTED, WITHDRAWN, PROMOTED, REQUEST, ANNOUNCEMENT). Fire and forget.
Listen with `./scasp.sh udp`.

## JSON API (same host as the web UI, session cookie)
| Method and path | Result |
|---|---|
| `POST /api/login` (form: userId, password) | `{"userId","name","role"}` or 401 |
| `GET /api/courses?q=&sort=` | array of `{code,title,capacity,registered,waiting,status,myState}` |
| `GET /api/my-registrations` | array of `{code,title,state,position}` |
| `POST /api/register`, `POST /api/withdraw` (form: code) | `{"result":"REGISTERED"|"WAITLISTED"|"WITHDRAWN"}`; errors 400/403/404/409/503 with `{"error":".."}` |
| `GET /api/campus-services` | array of `{name,description,latitude,longitude}` |
Not logged in: 401 `{"error":"login required"}`.

## RMI (registry port 1099, name `SCASPAdmin`)
`AdminService`: `ping()`, `capacityReport()`, `waitingListReport()`, `requestWorkload()`, `recentAudit(limit)`, and
`setCourseStatus(token, code, status)`. DTOs (`CourseLine`, `AuditLine`) are serializable and copied by value; the server object is used by remote
reference (stub). The token is random, printed when the server starts. Compared with HTTP: RMI is Java-only and tightly coupled but needs no
parsing code; HTTP works from any language and through proxies (see the CORBA dossier for the full comparison).
