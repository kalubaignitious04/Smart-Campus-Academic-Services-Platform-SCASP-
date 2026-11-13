package scasp.core;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import scasp.core.DomainException.Kind;
import scasp.core.Model.*;

/**
 * All business rules live here, once. Web, network and RMI layers call these methods.
 * Invariants: unique registration, capacity limit, valid status transitions, authorised operations.
 * A per-course lock keeps "check capacity then register" safe when many threads act at once.
 */
public class AcademicServices {
    public static final List<String> REQUEST_CATEGORIES = List.of("TRANSCRIPT", "CLEARANCE", "APPEAL", "OTHER");

    private final CampusStore store;
    private final NotificationBus bus;
    private final Clock clock;
    private final Map<String, Lock> courseLocks = new ConcurrentHashMap<>();

    public AcademicServices(CampusStore store, NotificationBus bus) { this(store, bus, Clock.systemUTC()); }
    public AcademicServices(CampusStore store, NotificationBus bus, Clock clock) {
        this.store = store; this.bus = bus; this.clock = clock;
    }
    public NotificationBus notifications() { return bus; }

    // ---------------- users ----------------
    public Optional<User> login(String userId, String password) {
        if (userId == null || password == null) return Optional.empty();
        return store.findUser(userId).filter(User::active)
                .filter(u -> store.passwordHash(userId).map(h -> Passwords.verify(password, h)).orElse(false));
    }
    public Optional<User> findUser(String id) { return store.findUser(id); }

    // ---------------- courses ----------------
    /** Search by code/title text and sort by "code" (default), "title" or "seats" (most seats left first). */
    public List<CourseRow> courseRows(String query, String sort, String studentId) {
        String q = query == null ? "" : query.trim().toLowerCase();
        Map<String, Integer> waitPos = new HashMap<>();
        Set<String> mine = new HashSet<>();
        if (studentId != null) {
            store.registrationsOf(studentId).forEach(r -> mine.add(r.courseCode()));
            for (WaitlistEntry w : store.waitlistsOf(studentId)) {
                waitPos.put(w.courseCode(), store.waitlist(w.courseCode()).stream().map(WaitlistEntry::studentId).toList().indexOf(studentId) + 1);
            }
        }
        List<CourseRow> rows = store.courses().stream()
                .filter(c -> q.isEmpty() || c.code().toLowerCase().contains(q) || c.title().toLowerCase().contains(q))
                .map(c -> new CourseRow(c, store.registrationCount(c.code()), store.waitlist(c.code()).size(),
                        mine.contains(c.code()) ? "REGISTERED" : waitPos.containsKey(c.code()) ? "WAITLISTED" : "",
                        waitPos.getOrDefault(c.code(), 0)))
                .collect(Collectors.toCollection(ArrayList::new));
        Comparator<CourseRow> order = switch (sort == null ? "code" : sort) {
            case "title" -> Comparator.comparing(CourseRow::course, Course.BY_TITLE);
            case "seats" -> Comparator.comparingInt(CourseRow::seatsLeft).reversed().thenComparing(CourseRow::course, Course.BY_CODE);
            default -> Comparator.comparing(CourseRow::course, Course.BY_CODE);
        };
        rows.sort(order);
        return Collections.unmodifiableList(rows);
    }

    public Course course(String code) {
        return store.findCourse(code).orElseThrow(() -> new DomainException(Kind.NOT_FOUND, "Course not found"));
    }

    public void saveCourse(User actor, String code, String title, int capacity) {
        requireStaff(actor);
        String c = code == null ? "" : code.trim().toUpperCase();
        Course draft = new Course(c, title == null ? "" : title.trim(), capacity, CourseStatus.OPEN);
        withLock(c, () -> store.atomic(() -> {
            Optional<Course> existing = store.findCourse(c);
            if (existing.isPresent() && capacity < store.registrationCount(c))
                throw new DomainException(Kind.CONFLICT, "Capacity cannot be lower than current registrations");
            Course toSave = existing.map(e -> new Course(c, draft.title(), capacity, e.status())).orElse(draft);
            store.saveCourse(toSave);
            audit(actor, existing.isPresent() ? "UPDATE_COURSE" : "CREATE_COURSE", "Course", c);
            promoteWaiting(actor, toSave);
            return null;
        }));
    }

    public void setCourseStatus(User actor, String code, CourseStatus next) {
        requireStaff(actor);
        withLock(code, () -> store.atomic(() -> {
            Course c = course(code);
            if (!c.status().canMoveTo(next))
                throw new DomainException(Kind.CONFLICT, "Cannot change course from " + c.status() + " to " + next);
            Course updated = new Course(c.code(), c.title(), c.capacity(), next);
            store.saveCourse(updated);
            audit(actor, "COURSE_" + next, "Course", code);
            promoteWaiting(actor, updated);
            return null;
        }));
    }

    // ---------------- registration ----------------
    public Outcome register(User actor, String studentId, String code) {
        requireSelfOrStaff(actor, studentId);
        Outcome outcome = withLock(code, () -> store.atomic(() -> {
            store.findStudent(studentId).orElseThrow(() -> new DomainException(Kind.NOT_FOUND, "Student not found"));
            Course c = course(code);
            if (c.status() != CourseStatus.OPEN) throw new DomainException(Kind.CONFLICT, "Course is not open for registration");
            boolean registered = store.registrations(code).stream().anyMatch(r -> r.studentId().equals(studentId));
            boolean waiting = store.waitlist(code).stream().anyMatch(w -> w.studentId().equals(studentId));
            if (registered || waiting) throw new DomainException(Kind.CONFLICT, "Already registered or on the waiting list");
            if (store.registrationCount(code) < c.capacity()) {
                store.addRegistration(new Registration(studentId, code, clock.instant()));
                audit(actor, "REGISTER", "Registration", studentId + "/" + code);
                return Outcome.REGISTERED;
            }
            store.addWaitlist(new WaitlistEntry(studentId, code, clock.instant()));
            audit(actor, "WAITLIST", "Registration", studentId + "/" + code);
            return Outcome.WAITLISTED;
        }));
        bus.publish(outcome.name(), studentId + " " + outcome.name().toLowerCase() + " for " + code);
        return outcome;
    }

    public Withdrawal withdraw(User actor, String studentId, String code) {
        requireSelfOrStaff(actor, studentId);
        Withdrawal w = withLock(code, () -> store.atomic(() -> {
            Course c = course(code);
            if (store.removeRegistration(studentId, code)) {
                audit(actor, "WITHDRAW", "Registration", studentId + "/" + code);
                return new Withdrawal(false, promoteWaiting(actor, c));
            }
            if (store.removeWaitlist(studentId, code)) {
                audit(actor, "LEAVE_WAITLIST", "Registration", studentId + "/" + code);
                return new Withdrawal(true, null);
            }
            throw new DomainException(Kind.NOT_FOUND, "You are not registered or waiting for this course");
        }));
        bus.publish("WITHDRAWN", studentId + " left " + code);
        if (w.promotedStudentId() != null) bus.publish("PROMOTED", w.promotedStudentId() + " promoted from waiting list of " + code);
        return w;
    }

    /** Moves waiting students into free seats, in queue order. Must run inside the course lock + atomic. */
    private String promoteWaiting(User actor, Course c) {
        String last = null;
        while (c.status() == CourseStatus.OPEN && store.registrationCount(c.code()) < c.capacity()) {
            List<WaitlistEntry> q = store.waitlist(c.code());
            if (q.isEmpty()) break;
            WaitlistEntry next = q.get(0);
            store.removeWaitlist(next.studentId(), c.code());
            store.addRegistration(new Registration(next.studentId(), c.code(), clock.instant()));
            audit(actor, "PROMOTE", "Registration", next.studentId() + "/" + c.code());
            last = next.studentId();
        }
        return last;
    }

    // ---------------- service requests ----------------
    public ServiceRequest submitRequest(User actor, String category, Priority priority, String details) {
        if (actor.role() != Role.STUDENT) throw new DomainException(Kind.FORBIDDEN, "Only students submit requests");
        if (!REQUEST_CATEGORIES.contains(category)) throw new DomainException(Kind.INVALID, "Unknown request category");
        if (details == null || details.isBlank() || details.length() > 500)
            throw new DomainException(Kind.INVALID, "Details are required (maximum 500 characters)");
        return store.atomic(() -> {
            var now = clock.instant();
            ServiceRequest r = store.insertRequest(new ServiceRequest(0, actor.userId(), category,
                    priority == null ? Priority.NORMAL : priority, RequestStatus.SUBMITTED, null, details.trim(), now, now));
            audit(actor, "SUBMIT_REQUEST", "ServiceRequest", String.valueOf(r.requestId()));
            bus.publish("REQUEST", "New " + category + " request #" + r.requestId());
            return r;
        });
    }

    public void assignRequest(User actor, long id, String staffId) {
        requireStaff(actor);
        store.atomic(() -> {
            ServiceRequest r = request(id);
            User assignee = store.findUser(staffId).filter(u -> u.role().isStaff())
                    .orElseThrow(() -> new DomainException(Kind.INVALID, "Assignee must be a staff member"));
            if (r.status() == RequestStatus.CLOSED) throw new DomainException(Kind.CONFLICT, "Request is closed");
            RequestStatus next = r.status() == RequestStatus.SUBMITTED ? RequestStatus.ASSIGNED : r.status();
            store.updateRequest(r.with(next, assignee.userId(), clock.instant()));
            audit(actor, "ASSIGN_REQUEST", "ServiceRequest", String.valueOf(id));
            bus.publish("REQUEST", "Request #" + id + " assigned to " + assignee.name());
            return null;
        });
    }

    public void advanceRequest(User actor, long id, RequestStatus next) {
        requireStaff(actor);
        store.atomic(() -> {
            ServiceRequest r = request(id);
            if (r.assignedTo() == null) throw new DomainException(Kind.CONFLICT, "Assign the request first");
            if (!r.status().canMoveTo(next))
                throw new DomainException(Kind.CONFLICT, "Cannot change request from " + r.status() + " to " + next);
            store.updateRequest(r.with(next, r.assignedTo(), clock.instant()));
            audit(actor, "REQUEST_" + next, "ServiceRequest", String.valueOf(id));
            bus.publish("REQUEST", "Request #" + id + " is now " + next);
            return null;
        });
    }

    private ServiceRequest request(long id) {
        return store.findRequest(id).orElseThrow(() -> new DomainException(Kind.NOT_FOUND, "Request not found"));
    }

    /** Students see their own requests; staff see all, most urgent first. */
    public List<ServiceRequest> requestsFor(User actor) {
        return store.requests().stream().filter(r -> actor.role().isStaff() || r.studentId().equals(actor.userId())).toList();
    }

    // ---------------- announcements ----------------
    public void publishAnnouncement(User actor, String title, String body, Audience audience) {
        requireStaff(actor);
        if (title == null || title.isBlank() || body == null || body.isBlank())
            throw new DomainException(Kind.INVALID, "Title and message are required");
        store.atomic(() -> {
            Announcement a = store.insertAnnouncement(new Announcement(0, title.trim(), body.trim(),
                    audience == null ? Audience.ALL : audience, clock.instant()));
            audit(actor, "PUBLISH_ANNOUNCEMENT", "Announcement", String.valueOf(a.announcementId()));
            bus.publish("ANNOUNCEMENT", a.title());
            return null;
        });
    }

    public List<Announcement> announcementsFor(User viewer) {
        return store.announcements().stream().filter(a -> a.audience() == Audience.ALL
                || (a.audience() == Audience.STUDENTS && viewer.role() == Role.STUDENT)
                || (a.audience() == Audience.STAFF && viewer.role().isStaff())).toList();
    }

    public List<CampusService> campusServices() { return store.campusServices(); }

    // ---------------- reports (staff) ----------------
    public List<CapacityLine> capacityReport() {
        return store.courses().stream().map(c -> new CapacityLine(c.code(), c.title(), c.capacity(),
                store.registrationCount(c.code()), store.waitlist(c.code()).size(), c.status())).toList();
    }
    public List<CapacityLine> waitingListReport() {
        return capacityReport().stream().filter(l -> l.waiting() > 0)
                .sorted(Comparator.comparingInt(CapacityLine::waiting).reversed()).toList();
    }
    public Map<RequestStatus, Long> requestsByStatus() {
        Map<RequestStatus, Long> m = store.requests().stream()
                .collect(Collectors.groupingBy(ServiceRequest::status, () -> new EnumMap<>(RequestStatus.class), Collectors.counting()));
        for (RequestStatus s : RequestStatus.values()) m.putIfAbsent(s, 0L);
        return m;
    }
    public Map<String, Long> openRequestsByAssignee() {
        return store.requests().stream().filter(r -> r.status() != RequestStatus.CLOSED && r.status() != RequestStatus.RESOLVED)
                .collect(Collectors.groupingBy(r -> r.assignedTo() == null ? "(unassigned)" : r.assignedTo(), TreeMap::new, Collectors.counting()));
    }
    public List<StudentLine> studentReport() {
        return store.users().stream().filter(u -> u.role() == Role.STUDENT)
                .map(u -> new StudentLine(u.userId(), u.name(), store.registrationsOf(u.userId()).size(), store.waitlistsOf(u.userId()).size()))
                .sorted(Comparator.comparing(StudentLine::studentId)).toList();
    }
    public List<AuditEvent> auditReport(int limit) { return store.audit(limit); }
    public List<User> staff() { return store.users().stream().filter(u -> u.role().isStaff()).toList(); }

    // ---------------- helpers ----------------
    private void audit(User actor, String action, String type, String id) {
        store.insertAudit(new AuditEvent(0, actor.userId(), action, type, id, clock.instant()));
    }
    private void requireStaff(User actor) {
        if (actor == null || !actor.role().isStaff()) throw new DomainException(Kind.FORBIDDEN, "Staff access required");
    }
    private void requireSelfOrStaff(User actor, String studentId) {
        if (actor == null || !(actor.role().isStaff() || actor.userId().equals(studentId)))
            throw new DomainException(Kind.FORBIDDEN, "You may only act for yourself");
    }
    private <T> T withLock(String code, java.util.function.Supplier<T> body) {
        Lock l = courseLocks.computeIfAbsent(code == null ? "" : code, k -> new ReentrantLock());
        l.lock();
        try { return body.get(); } finally { l.unlock(); }
    }
}
