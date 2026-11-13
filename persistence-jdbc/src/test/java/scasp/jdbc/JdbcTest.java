package scasp.jdbc;

import static org.junit.jupiter.api.Assertions.*;
import static scasp.core.TestSupport.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.junit.jupiter.api.*;
import scasp.core.*;
import scasp.core.Model.*;

class JdbcTest {
    private BasicDataSource ds;
    private JdbcStore store;
    private AcademicServices s;
    private static final AtomicInteger N = new AtomicInteger();

    @BeforeEach void setUp() {
        ds = Database.pooled("jdbc:h2:mem:jdbc" + N.incrementAndGet() + ";DB_CLOSE_DELAY=-1", 10);
        Database.init(ds, true);
        store = new JdbcStore(ds);
        s = new AcademicServices(store, new NotificationBus());
    }
    @AfterEach void tearDown() throws Exception { ds.close(); }

    @Test void scriptsLoadSampleDataAndRunTwiceSafely() {
        assertEquals(5, store.courses().size());
        Database.init(ds, true);                                              // repeatable: nothing duplicated
        assertEquals(5, store.courses().size());
        assertEquals(7, store.users().size());
        assertTrue(s.login("T2001", "Demo#2026").isPresent());
    }

    @Test void businessFlowMatchesTheInMemoryRules() {
        User s2 = user(s, "S1002"), s3 = user(s, "S1003");
        assertEquals(Outcome.REGISTERED, s.register(s2, "S1002", "CS101"));
        assertEquals(Outcome.WAITLISTED, s.register(s3, "S1003", "CS101"));
        assertEquals("S1003", s.withdraw(s2, "S1002", "CS101").promotedStudentId());
        assertEquals(2, store.registrationCount("CS101"));
        List<String> latestFirst = s.auditReport(4).stream().map(AuditEvent::action).toList();
        assertEquals(List.of("PROMOTE", "WITHDRAW", "WAITLIST", "REGISTER"), latestFirst);
    }

    @Test void transactionRollsBackEverythingOnFailure() {
        assertThrows(IllegalStateException.class, () -> store.atomic(() -> {
            store.addRegistration(new Registration("S1002", "CS210", Instant.now()));
            store.insertAudit(new AuditEvent(0, "S1002", "REGISTER", "Registration", "S1002/CS210", Instant.now()));
            throw new IllegalStateException("boom");
        }));
        assertEquals(0, store.registrationCount("CS210"));
        assertTrue(store.audit(50).stream().noneMatch(a -> a.entityId().equals("S1002/CS210")));
    }

    @Test void transactionCommitsAllStepsTogether() {
        store.atomic(() -> {
            store.addRegistration(new Registration("S1002", "CS210", Instant.now()));
            store.insertAudit(new AuditEvent(0, "S1002", "REGISTER", "Registration", "S1002/CS210", Instant.now()));
            return null;
        });
        assertEquals(1, store.registrationCount("CS210"));
        assertEquals("REGISTER", store.audit(1).get(0).action());
    }

    @Test void constraintsAreEnforcedByTheDatabase() {
        assertFalse(store.addRegistration(new Registration("S1001", "CS101", Instant.now())));            // duplicate key -> false
        assertThrows(StoreException.class, () -> store.addRegistration(new Registration("S1002", "NOPE", Instant.now())));   // foreign key
        assertThrows(StoreException.class, () -> store.saveStudent(new Student("S1001", "BSc", 99)));      // CHECK constraint
    }

    @Test void sqlInjectionTextIsTreatedAsPlainData() {
        assertTrue(store.findUser("x' OR '1'='1").isEmpty());
        assertTrue(s.login("S1001' --", "x").isEmpty());
        s.saveCourse(user(s, "T2001"), "Q1", "Robert'); DROP TABLE courses;--", 10);
        assertEquals(6, store.courses().size());
        assertEquals("Robert'); DROP TABLE courses;--", store.findCourse("Q1").orElseThrow().title());
    }

    @Test void concurrentRegistrationNeverExceedsCapacityInTheDatabase() throws Exception {
        addStudents(store, 20);
        s.saveCourse(user(s, "T2001"), "JC", "JDBC race", 3);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Outcome>> fs = new ArrayList<>();
        for (int i = 1; i <= 20; i++) { String id = String.format("P%02d", i); fs.add(pool.submit(() -> { gate.await(); return s.register(user(s, id), id, "JC"); })); }
        gate.countDown();
        for (var f : fs) f.get(20, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(3, store.registrationCount("JC"));
        assertEquals(17, store.waitlist("JC").size());
    }
}
