package scasp.jdbc;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import scasp.core.CampusStore;
import scasp.core.Model.*;

/**
 * Stage 5: CampusStore on JDBC. Every user-controlled value is bound with PreparedStatement,
 * never concatenated into SQL. atomic() opens one transaction that all calls on this thread share.
 */
public class JdbcStore implements CampusStore {
    private interface Rows<T> { T map(ResultSet rs) throws SQLException; }
    private interface Work<T> { T apply(Connection c) throws SQLException; }

    private final DataSource ds;
    private final ThreadLocal<Connection> tx = new ThreadLocal<>();

    public JdbcStore(DataSource ds) { this.ds = ds; }

    // ---------- plumbing ----------
    @Override public <T> T atomic(Supplier<T> work) {
        if (tx.get() != null) return work.get();           // join the running transaction
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            tx.set(c);
            try {
                T result = work.get();
                c.commit();
                return result;
            } catch (RuntimeException | Error e) {
                c.rollback();
                throw e;
            } finally {
                tx.remove();
                c.setAutoCommit(true);
            }
        } catch (SQLException e) { throw new StoreException(e); }
    }

    private <T> T run(Work<T> w) {
        try {
            Connection c = tx.get();
            if (c != null) return w.apply(c);
            try (Connection k = ds.getConnection()) { return w.apply(k); }
        } catch (SQLException e) { throw new StoreException(e); }
    }

    private static void bind(PreparedStatement p, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
    }
    private <T> List<T> list(String sql, Rows<T> m, Object... args) {
        return run(c -> {
            try (PreparedStatement p = c.prepareStatement(sql)) {
                bind(p, args);
                try (ResultSet rs = p.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) out.add(m.map(rs));
                    return Collections.unmodifiableList(out);
                }
            }
        });
    }
    private <T> Optional<T> one(String sql, Rows<T> m, Object... args) { return list(sql, m, args).stream().findFirst(); }
    private int update(String sql, Object... args) {
        return run(c -> { try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, args); return p.executeUpdate(); } });
    }
    private long insertReturningId(String sql, Object... args) {
        return run(c -> {
            try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(p, args);
                p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) { k.next(); return k.getLong(1); }
            }
        });
    }
    private static boolean duplicate(SQLException e) { return "23505".equals(e.getSQLState()); }
    private static long ms(Instant i) { return i == null ? 0 : i.toEpochMilli(); }
    private static Instant at(ResultSet rs, String col) throws SQLException { return Instant.ofEpochMilli(rs.getLong(col)); }

    // ---------- mappers ----------
    private static final Rows<User> USER = rs -> new User(rs.getString("user_id"), rs.getString("name"), Role.valueOf(rs.getString("role")), rs.getBoolean("active"));
    private static final Rows<Course> COURSE = rs -> new Course(rs.getString("code"), rs.getString("title"), rs.getInt("capacity"), CourseStatus.valueOf(rs.getString("status")));
    private static final Rows<Registration> REG = rs -> new Registration(rs.getString("student_id"), rs.getString("course_code"), at(rs, "created_at"));
    private static final Rows<WaitlistEntry> WAIT = rs -> new WaitlistEntry(rs.getString("student_id"), rs.getString("course_code"), at(rs, "created_at"));
    private static final Rows<ServiceRequest> REQ = rs -> new ServiceRequest(rs.getLong("request_id"), rs.getString("student_id"), rs.getString("category"),
            Priority.valueOf(rs.getString("priority")), RequestStatus.valueOf(rs.getString("status")), rs.getString("assigned_to"),
            rs.getString("details"), at(rs, "created_at"), at(rs, "updated_at"));

    // ---------- users ----------
    @Override public Optional<User> findUser(String id) { return one("SELECT * FROM users WHERE user_id = ?", USER, id); }
    @Override public Optional<String> passwordHash(String id) { return one("SELECT password_hash FROM users WHERE user_id = ?", rs -> rs.getString(1), id); }
    @Override public List<User> users() { return list("SELECT * FROM users ORDER BY user_id", USER); }
    @Override public void saveUser(User u, String hash) {
        update("MERGE INTO users(user_id,name,role,active,password_hash) KEY(user_id) VALUES(?,?,?,?,?)", u.userId(), u.name(), u.role().name(), u.active(), hash);
    }
    @Override public Optional<Student> findStudent(String id) {
        return one("SELECT * FROM students WHERE student_id = ?", rs -> new Student(rs.getString("student_id"), rs.getString("programme"), rs.getInt("year_of_study")), id);
    }
    @Override public void saveStudent(Student s) {
        update("MERGE INTO students(student_id,programme,year_of_study) KEY(student_id) VALUES(?,?,?)", s.studentId(), s.programme(), s.yearOfStudy());
    }

    // ---------- courses ----------
    @Override public List<Course> courses() { return list("SELECT * FROM courses ORDER BY code", COURSE); }
    @Override public Optional<Course> findCourse(String code) { return one("SELECT * FROM courses WHERE code = ?", COURSE, code); }
    @Override public void saveCourse(Course c) {
        update("MERGE INTO courses(code,title,capacity,status) KEY(code) VALUES(?,?,?,?)", c.code(), c.title(), c.capacity(), c.status().name());
    }

    // ---------- registrations ----------
    @Override public boolean addRegistration(Registration r) {
        try { return update("INSERT INTO registrations(student_id,course_code,created_at) VALUES(?,?,?)", r.studentId(), r.courseCode(), ms(r.createdAt())) == 1; }
        catch (StoreException e) { if (e.getCause() instanceof SQLException s && duplicate(s)) return false; throw e; }
    }
    @Override public boolean removeRegistration(String studentId, String code) {
        return update("DELETE FROM registrations WHERE student_id = ? AND course_code = ?", studentId, code) == 1;
    }
    @Override public int registrationCount(String code) {
        return one("SELECT COUNT(*) FROM registrations WHERE course_code = ?", rs -> rs.getInt(1), code).orElse(0);
    }
    @Override public List<Registration> registrations(String code) { return list("SELECT * FROM registrations WHERE course_code = ? ORDER BY created_at, student_id", REG, code); }
    @Override public List<Registration> registrationsOf(String studentId) { return list("SELECT * FROM registrations WHERE student_id = ? ORDER BY course_code", REG, studentId); }

    @Override public boolean addWaitlist(WaitlistEntry e) {
        try { return update("INSERT INTO waitlist(student_id,course_code,created_at) VALUES(?,?,?)", e.studentId(), e.courseCode(), ms(e.createdAt())) == 1; }
        catch (StoreException ex) { if (ex.getCause() instanceof SQLException s && duplicate(s)) return false; throw ex; }
    }
    @Override public boolean removeWaitlist(String studentId, String code) {
        return update("DELETE FROM waitlist WHERE student_id = ? AND course_code = ?", studentId, code) == 1;
    }
    @Override public List<WaitlistEntry> waitlist(String code) { return list("SELECT * FROM waitlist WHERE course_code = ? ORDER BY id", WAIT, code); }
    @Override public List<WaitlistEntry> waitlistsOf(String studentId) { return list("SELECT * FROM waitlist WHERE student_id = ? ORDER BY id", WAIT, studentId); }

    // ---------- service requests ----------
    @Override public ServiceRequest insertRequest(ServiceRequest d) {
        long id = insertReturningId("INSERT INTO service_requests(student_id,category,priority,status,assigned_to,details,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?)",
                d.studentId(), d.category(), d.priority().name(), d.status().name(), d.assignedTo(), d.details(), ms(d.createdAt()), ms(d.updatedAt()));
        return new ServiceRequest(id, d.studentId(), d.category(), d.priority(), d.status(), d.assignedTo(), d.details(), d.createdAt(), d.updatedAt());
    }
    @Override public void updateRequest(ServiceRequest r) {
        update("UPDATE service_requests SET status = ?, assigned_to = ?, updated_at = ? WHERE request_id = ?", r.status().name(), r.assignedTo(), ms(r.updatedAt()), r.requestId());
    }
    @Override public Optional<ServiceRequest> findRequest(long id) { return one("SELECT * FROM service_requests WHERE request_id = ?", REQ, id); }
    @Override public List<ServiceRequest> requests() {
        List<ServiceRequest> all = new ArrayList<>(list("SELECT * FROM service_requests", REQ));
        all.sort(ServiceRequest.URGENCY);                      // same ordering rule as the in-memory store
        return Collections.unmodifiableList(all);
    }

    // ---------- announcements, audit, campus services ----------
    @Override public Announcement insertAnnouncement(Announcement a) {
        long id = insertReturningId("INSERT INTO announcements(title,body,audience,published_at) VALUES(?,?,?,?)", a.title(), a.body(), a.audience().name(), ms(a.publishedAt()));
        return new Announcement(id, a.title(), a.body(), a.audience(), a.publishedAt());
    }
    @Override public List<Announcement> announcements() {
        return list("SELECT * FROM announcements ORDER BY published_at DESC, announcement_id DESC",
                rs -> new Announcement(rs.getLong("announcement_id"), rs.getString("title"), rs.getString("body"), Audience.valueOf(rs.getString("audience")), at(rs, "published_at")));
    }
    @Override public void insertAudit(AuditEvent e) {
        update("INSERT INTO audit_events(actor,action,entity_type,entity_id,occurred_at) VALUES(?,?,?,?,?)", e.actor(), e.action(), e.entityType(), e.entityId(), ms(e.occurredAt()));
    }
    @Override public List<AuditEvent> audit(int limit) {
        return list("SELECT * FROM audit_events ORDER BY event_id DESC LIMIT ?",
                rs -> new AuditEvent(rs.getLong("event_id"), rs.getString("actor"), rs.getString("action"), rs.getString("entity_type"), rs.getString("entity_id"), at(rs, "occurred_at")), limit);
    }
    @Override public List<CampusService> campusServices() {
        return list("SELECT * FROM campus_services ORDER BY service_id",
                rs -> new CampusService(rs.getLong("service_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("latitude"), rs.getDouble("longitude")));
    }
    @Override public void saveCampusService(CampusService s) {
        update("MERGE INTO campus_services(service_id,name,description,latitude,longitude) KEY(service_id) VALUES(?,?,?,?,?)", s.serviceId(), s.name(), s.description(), s.latitude(), s.longitude());
    }
}
