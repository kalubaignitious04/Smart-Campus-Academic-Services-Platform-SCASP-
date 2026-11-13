# Legacy interoperability dossier: CORBA (Stage 9)

No ORB is used in this project. Java removed the CORBA modules from the JDK in Java 11 (JEP 320), so a real prototype
needs a third-party ORB (for example JacORB) or a lecturer-provided environment. This dossier is analysis only.

## 1. CORBA interaction, in SCASP terms
| CORBA element | Role | RMI equivalent in SCASP |
|---|---|---|
| IDL | Language-neutral contract (`scasp.idl`) | the `AdminService` Java interface |
| ORB | Runtime that carries calls between client and server | RMI runtime / JRMP |
| Object reference (IOR) | Address of a remote object | RMI stub |
| Naming service | Finds an object by name | RMI registry (`SCASPAdmin`) |
| Stub (client) / Skeleton (server) | Generated code that marshals calls | generated stub / dynamic proxies |
| Servant | Server-side implementation | `AdminServer` |

## 2. IDL and mapping
`scasp.idl` defines `Admin.capacityReport()` and `Admin.setCourseStatus(...)`, equivalent to the RMI operations.
| IDL | Domain / Java |
|---|---|
| `struct CourseLine` | `AdminService.CourseLine` (from `Model.CapacityLine`) |
| `sequence<CourseLine>` | `List<CourseLine>` |
| `enum CourseStatus` | `Model.CourseStatus` |
| `exception NotAuthorised` / `InvalidRequest` | `RemoteException("Not authorised")` / `DomainException` |
| `long` | `int` |

## 3. Invocation flow
Legacy client -> client stub -> ORB (marshals to IIOP) -> network -> server ORB -> POA -> skeleton -> servant (`CorbaAdminAdapter`) ->
`AcademicServices` -> `CampusStore` -> result marshalled back the same way. The client found the servant through the naming service.

## 4. Comparison
| | CORBA | RMI | HTTP (servlets/JSON) |
|---|---|---|---|
| Language support | Many (via IDL) | Java only | Any |
| Coupling | Contract-first, generated code | Shares Java interface and classes | Loose: JSON text |
| Deployment | ORB, naming service, ports, IOR handling | Registry + exported objects | Web server, one port, proxies/firewalls friendly |
| Security | CORBA security service, rarely configured | Needs own token, SSL socket factories | TLS, sessions, standard tooling |
| Maintainability | Legacy, few skills, no JDK support | Old but in JDK | Most common, best tooling |

## 5. Migration pattern: adapter / facade
Keep `AcademicServices` as the single place for rules. Add a `CorbaAdminAdapter` (servant) that only converts CORBA types to the
existing DTOs and calls the same methods the RMI server calls. Run it beside RMI and HTTP while the legacy client still exists, then switch
the client to HTTP and delete the adapter. No business rule is copied.

## 6. Risks
* Compatibility: ORB versions and IIOP details differ; test with the real legacy client early.
* Data mapping: IDL `long` vs Java `int`/`long`, string encodings, enum evolution (adding values breaks old clients).
* Transactions: our operations are single calls with their own transaction; CORBA Object Transaction Service would not be used.
* Operations: naming service availability, firewall ports for IIOP, IOR changes after restarts, no maintained JDK support.
