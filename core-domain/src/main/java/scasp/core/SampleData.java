package scasp.core;

import java.time.Instant;
import scasp.core.Model.*;

/** Fictional demo data for in-memory use and tests. Demo password for every account: Demo#2026 */
public final class SampleData {
    public static final String DEMO_PASSWORD = "Demo#2026";
    private SampleData() {}

    public static void load(CampusStore s) {
        String hash = Passwords.hash(DEMO_PASSWORD);
        String[][] students = {{"S1001", "Chola Demo", "BSc Computer Science", "3"}, {"S1002", "Mwansa Demo", "BSc Computer Science", "3"},
                {"S1003", "Natasha Demo", "BSc Information Systems", "2"}, {"S1004", "Kondwani Demo", "BSc Computer Science", "1"},
                {"S1005", "Lungile Demo", "BSc Information Systems", "2"}};
        for (String[] st : students) {
            s.saveUser(new User(st[0], st[1], Role.STUDENT, true), hash);
            s.saveStudent(new Student(st[0], st[2], Integer.parseInt(st[3])));
        }
        s.saveUser(new User("T2001", "Registrar Demo", Role.ACADEMIC_STAFF, true), hash);
        s.saveUser(new User("A3001", "Admin Demo", Role.ADMIN, true), hash);
        Object[][] courses = {{"CS101", "Introduction to Programming", 2}, {"CS210", "Data Structures", 40},
                {"DB201", "Database Systems", 3}, {"NET220", "Computer Networks", 30}, {"ENT301", "Entrepreneurship", 25}};
        for (Object[] c : courses) s.saveCourse(new Course((String) c[0], (String) c[1], (Integer) c[2], CourseStatus.OPEN));
        Instant now = Instant.now();
        s.addRegistration(new Registration("S1001", "CS101", now));
        s.addRegistration(new Registration("S1001", "DB201", now));
        s.insertAnnouncement(new Announcement(0, "Welcome to SCASP", "Register for courses and raise service requests here.", Audience.ALL, now));
        s.insertAnnouncement(new Announcement(0, "Registry hours", "The Registry Office is open 08:00-16:00, Monday to Friday.", Audience.STUDENTS, now));
        s.saveCampusService(new CampusService(1, "Library", "Study space and book loans", -15.4167, 28.2833));
        s.saveCampusService(new CampusService(2, "Computer Lab", "Open-access computers", -15.4172, 28.2841));
        s.saveCampusService(new CampusService(3, "Registry Office", "Transcripts, clearance and appeals", -15.4160, 28.2825));
        s.saveCampusService(new CampusService(4, "Health Clinic", "Basic student health services", -15.4179, 28.2819));
    }
}
