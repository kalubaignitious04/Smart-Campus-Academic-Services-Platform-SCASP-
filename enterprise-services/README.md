# Stage 4 - Container-managed business services (NOT BUILT)

No Jakarta EE server (WildFly, GlassFish, Payara or TomEE) was available when the rest of the platform was built, so
nothing here could be deployed or tested. Do not submit this folder as working code until a team member has done it.

Suggested minimum (matches the brief):
1. `RegistrationBean` - `@Stateless`, local business view, `@Inject`/`@EJB` of `CampusStore`; its `register()` delegates to
   `AcademicServices` so the business rules stay in one place.
2. `MetricsBean` - `@Singleton` holding counters (registrations, requests) with `@PostConstruct` / `@PreDestroy`.
3. One container-managed transaction (`@TransactionAttribute(REQUIRED)`) around register + waitlist + audit.
4. A `@Schedule` timer (e.g. nightly waiting-list report), or document the alternative.
5. Explain why entity beans are not used: JPA/JDBC replaces them (`persistence-jdbc` is the replacement).
6. Package as a WAR/EAR, deploy, keep screenshots and the deployment log as evidence.
Needs `jakarta.ejb-api` on the compile classpath (not in `lib/`).
