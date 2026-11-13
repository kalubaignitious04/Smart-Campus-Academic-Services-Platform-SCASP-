package scasp.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import scasp.core.Model.*;

/**
 * Stage 1 store built on the collections framework:
 * Map for lookups, Set for registrations (no duplicates), Deque for waiting lists,
 * PriorityQueue for service requests. All methods are synchronized, and results are copies.
 */
public class InMemoryStore implements CampusStore {
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, String> hashes = new HashMap<>();
    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new TreeMap<>();
    private final Set<Registration> registrations = new LinkedHashSet<>();
    private final Map<String, Deque<WaitlistEntry>> waitlists = new HashMap<>();
    private final PriorityQueue<ServiceRequest> requests = new PriorityQueue<>(ServiceRequest.URGENCY);
    private final List<Announcement> announcements = new ArrayList<>();
    private final Deque<AuditEvent> audit = new ArrayDeque<>();
    private final List<CampusService> services = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(1);

    @Override public synchronized <T> T atomic(Supplier<T> work) { return work.get(); }

    @Override public synchronized Optional<User> findUser(String id) { return Optional.ofNullable(users.get(id)); }
    @Override public synchronized Optional<String> passwordHash(String id) { return Optional.ofNullable(hashes.get(id)); }
    @Override public synchronized List<User> users() { return List.copyOf(users.values()); }
    @Override public synchronized void saveUser(User u, String hash) { users.put(u.userId(), u); hashes.put(u.userId(), hash); }
    @Override public synchronized Optional<Student> findStudent(String id) { return Optional.ofNullable(students.get(id)); }
    @Override public synchronized void saveStudent(Student s) { students.put(s.studentId(), s); }

    @Override public synchronized List<Course> courses() { return List.copyOf(courses.values()); }
    @Override public synchronized Optional<Course> findCourse(String code) { return Optional.ofNullable(courses.get(code)); }
    @Override public synchronized void saveCourse(Course c) { courses.put(c.code(), c); }

    @Override public synchronized boolean addRegistration(Registration r) { return registrations.add(r); }
    @Override public synchronized boolean removeRegistration(String studentId, String code) {
        return registrations.remove(new Registration(studentId, code, null));
    }
    @Override public synchronized int registrationCount(String code) {
        return (int) registrations.stream().filter(r -> r.courseCode().equals(code)).count();
    }
    @Override public synchronized List<Registration> registrations(String code) {
        return registrations.stream().filter(r -> r.courseCode().equals(code)).toList();
    }
    @Override public synchronized List<Registration> registrationsOf(String studentId) {
        return registrations.stream().filter(r -> r.studentId().equals(studentId)).toList();
    }

    @Override public synchronized boolean addWaitlist(WaitlistEntry e) {
        Deque<WaitlistEntry> q = waitlists.computeIfAbsent(e.courseCode(), k -> new ArrayDeque<>());
        if (q.stream().anyMatch(w -> w.studentId().equals(e.studentId()))) return false;
        q.addLast(e);
        return true;
    }
    @Override public synchronized boolean removeWaitlist(String studentId, String code) {
        Deque<WaitlistEntry> q = waitlists.get(code);
        return q != null && q.removeIf(w -> w.studentId().equals(studentId));
    }
    @Override public synchronized List<WaitlistEntry> waitlist(String code) {
        return List.copyOf(waitlists.getOrDefault(code, new ArrayDeque<>()));
    }
    @Override public synchronized List<WaitlistEntry> waitlistsOf(String studentId) {
        return waitlists.values().stream().flatMap(Collection::stream)
                .filter(w -> w.studentId().equals(studentId)).toList();
    }

    @Override public synchronized ServiceRequest insertRequest(ServiceRequest d) {
        ServiceRequest r = new ServiceRequest(ids.getAndIncrement(), d.studentId(), d.category(), d.priority(),
                d.status(), d.assignedTo(), d.details(), d.createdAt(), d.updatedAt());
        requests.add(r);
        return r;
    }
    @Override public synchronized void updateRequest(ServiceRequest r) {
        requests.removeIf(x -> x.requestId() == r.requestId());
        requests.add(r);
    }
    @Override public synchronized Optional<ServiceRequest> findRequest(long id) {
        return requests.stream().filter(r -> r.requestId() == id).findFirst();
    }
    @Override public synchronized List<ServiceRequest> requests() {
        return requests.stream().sorted(ServiceRequest.URGENCY).toList();
    }

    @Override public synchronized Announcement insertAnnouncement(Announcement a) {
        Announcement n = new Announcement(ids.getAndIncrement(), a.title(), a.body(), a.audience(), a.publishedAt());
        announcements.add(0, n);
        return n;
    }
    @Override public synchronized List<Announcement> announcements() { return List.copyOf(announcements); }

    @Override public synchronized void insertAudit(AuditEvent e) {
        audit.addFirst(new AuditEvent(ids.getAndIncrement(), e.actor(), e.action(), e.entityType(), e.entityId(), e.occurredAt()));
    }
    @Override public synchronized List<AuditEvent> audit(int limit) {
        return audit.stream().limit(limit).collect(Collectors.toUnmodifiableList());
    }

    @Override public synchronized List<CampusService> campusServices() { return List.copyOf(services); }
    @Override public synchronized void saveCampusService(CampusService s) {
        services.removeIf(x -> x.serviceId() == s.serviceId());
        services.add(s);
        services.sort(Comparator.comparingLong(CampusService::serviceId));
    }
}
