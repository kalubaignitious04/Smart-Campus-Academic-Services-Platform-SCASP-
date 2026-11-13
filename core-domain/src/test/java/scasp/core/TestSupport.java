package scasp.core;

import scasp.core.Model.*;

/** Shared helpers for tests: a ready service on a store loaded with the fictional sample data. */
public final class TestSupport {
    private TestSupport() {}
    public static AcademicServices services(CampusStore store) {
        SampleData.load(store);
        return new AcademicServices(store, new NotificationBus());
    }
    public static AcademicServices memory() { return services(new InMemoryStore()); }
    public static User user(AcademicServices s, String id) { return s.findUser(id).orElseThrow(); }
    /** Adds n extra students P01..Pn so concurrency tests have many distinct people. */
    public static void addStudents(CampusStore store, int n) {
        for (int i = 1; i <= n; i++) {
            String id = String.format("P%02d", i);
            store.saveUser(new User(id, "Person " + i, Role.STUDENT, true), "x");
            store.saveStudent(new Student(id, "Test", 1));
        }
    }
}
