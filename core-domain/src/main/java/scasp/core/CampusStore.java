package scasp.core;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import scasp.core.Model.*;

/**
 * Storage boundary. Stage 1 implements it with collections (InMemoryStore);
 * Stage 5 implements it with JDBC. AcademicServices only depends on this interface.
 */
public interface CampusStore {
    /** Runs work as one unit: a database transaction (JDBC) or a critical section (memory). */
    <T> T atomic(Supplier<T> work);

    Optional<User> findUser(String userId);
    Optional<String> passwordHash(String userId);
    List<User> users();
    void saveUser(User user, String passwordHash);
    Optional<Student> findStudent(String studentId);
    void saveStudent(Student student);

    List<Course> courses();
    Optional<Course> findCourse(String code);
    void saveCourse(Course course);

    boolean addRegistration(Registration r);                 // false when it already exists
    boolean removeRegistration(String studentId, String courseCode);
    int registrationCount(String courseCode);
    List<Registration> registrations(String courseCode);
    List<Registration> registrationsOf(String studentId);

    boolean addWaitlist(WaitlistEntry e);
    boolean removeWaitlist(String studentId, String courseCode);
    List<WaitlistEntry> waitlist(String courseCode);         // in queue order
    List<WaitlistEntry> waitlistsOf(String studentId);

    ServiceRequest insertRequest(ServiceRequest draft);      // returns the stored request with its id
    void updateRequest(ServiceRequest r);
    Optional<ServiceRequest> findRequest(long id);
    List<ServiceRequest> requests();                         // most urgent first

    Announcement insertAnnouncement(Announcement a);
    List<Announcement> announcements();                      // newest first

    void insertAudit(AuditEvent e);
    List<AuditEvent> audit(int limit);                       // newest first

    List<CampusService> campusServices();
    void saveCampusService(CampusService s);
}
