package scasp.core;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

/** Domain model for SCASP. Records are immutable; changes create new instances. */
public final class Model {
    private Model() {}

    public enum Role { STUDENT, ACADEMIC_STAFF, ADMIN; public boolean isStaff() { return this != STUDENT; } }
    public enum Audience { ALL, STUDENTS, STAFF }
    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    public enum CourseStatus {
        OPEN, CLOSED, CANCELLED;
        public boolean canMoveTo(CourseStatus next) {
            return switch (this) {
                case OPEN -> next == CLOSED || next == CANCELLED;
                case CLOSED -> next == OPEN || next == CANCELLED;
                case CANCELLED -> false;
            };
        }
    }

    public enum RequestStatus {
        SUBMITTED, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED;
        /** Forward one step at a time; a resolved request may be reopened. */
        public boolean canMoveTo(RequestStatus next) {
            return next.ordinal() == ordinal() + 1 || (this == RESOLVED && next == IN_PROGRESS);
        }
    }

    public enum Outcome { REGISTERED, WAITLISTED }

    public record User(String userId, String name, Role role, boolean active) {
        public User {
            Objects.requireNonNull(userId); Objects.requireNonNull(name); Objects.requireNonNull(role);
        }
    }

    /** A student is a user with the same id (studentId == userId). */
    public record Student(String studentId, String programme, int yearOfStudy) {}

    public record Course(String code, String title, int capacity, CourseStatus status) {
        public Course {
            if (code == null || code.isBlank()) throw new DomainException(DomainException.Kind.INVALID, "Course code is required");
            if (title == null || title.isBlank()) throw new DomainException(DomainException.Kind.INVALID, "Course title is required");
            if (capacity < 1 || capacity > 1000) throw new DomainException(DomainException.Kind.INVALID, "Capacity must be between 1 and 1000");
            Objects.requireNonNull(status);
        }
        public static final Comparator<Course> BY_CODE = Comparator.comparing(Course::code);
        public static final Comparator<Course> BY_TITLE = Comparator.comparing(c -> c.title().toLowerCase());
    }

    /** Equality is (studentId, courseCode): a Set can never hold a duplicate registration. */
    public record Registration(String studentId, String courseCode, Instant createdAt) {
        @Override public boolean equals(Object o) {
            return o instanceof Registration r && studentId.equals(r.studentId) && courseCode.equals(r.courseCode);
        }
        @Override public int hashCode() { return Objects.hash(studentId, courseCode); }
    }

    public record WaitlistEntry(String studentId, String courseCode, Instant createdAt) {}

    public record ServiceRequest(long requestId, String studentId, String category, Priority priority,
                                 RequestStatus status, String assignedTo, String details,
                                 Instant createdAt, Instant updatedAt) {
        /** Most urgent first, then oldest first. */
        public static final Comparator<ServiceRequest> URGENCY =
                Comparator.comparing(ServiceRequest::priority, Comparator.reverseOrder())
                        .thenComparing(ServiceRequest::createdAt)
                        .thenComparingLong(ServiceRequest::requestId);
        public ServiceRequest with(RequestStatus s, String assignee, Instant now) {
            return new ServiceRequest(requestId, studentId, category, priority, s, assignee, details, createdAt, now);
        }
    }

    public record Announcement(long announcementId, String title, String body, Audience audience, Instant publishedAt) {}
    public record AuditEvent(long eventId, String actor, String action, String entityType, String entityId, Instant occurredAt) {}
    public record CampusService(long serviceId, String name, String description, double latitude, double longitude) {}
    public record Notification(String type, String message, Instant at) {}

    // ---- view / report rows ----
    public record CourseRow(Course course, int registered, int waiting, String myState, int myPosition) {
        public int seatsLeft() { return Math.max(0, course.capacity() - registered); }
    }
    public record Withdrawal(boolean fromWaitlist, String promotedStudentId) {}
    public record CapacityLine(String code, String title, int capacity, int registered, int waiting, CourseStatus status) {
        public int percentFull() { return capacity == 0 ? 0 : Math.round(100f * registered / capacity); }
    }
    public record StudentLine(String studentId, String name, int registered, int waiting) {}
}
