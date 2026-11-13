package scasp.core;

import static org.junit.jupiter.api.Assertions.*;
import static scasp.core.TestSupport.*;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import scasp.core.Model.*;

class CoreTest {
    @Test void registrationEqualityIgnoresTimeSoSetRejectsDuplicates() {
        Set<Registration> set = new HashSet<>();
        assertTrue(set.add(new Registration("S1", "C1", Instant.now())));
        assertFalse(set.add(new Registration("S1", "C1", Instant.now().plusSeconds(5))));
        assertEquals(new Registration("S1", "C1", null).hashCode(), new Registration("S1", "C1", Instant.now()).hashCode());
    }

    @Test void fillCourseThenWaitlistThenWithdrawPromotes() {
        var s = memory();
        User s2 = user(s, "S1002"), s3 = user(s, "S1003"), s4 = user(s, "S1004");
        assertEquals(Outcome.REGISTERED, s.register(s2, "S1002", "CS101"));   // S1001 already holds seat 1 of 2
        assertEquals(Outcome.WAITLISTED, s.register(s3, "S1003", "CS101"));
        assertEquals(Outcome.WAITLISTED, s.register(s4, "S1004", "CS101"));
        Withdrawal w = s.withdraw(s2, "S1002", "CS101");
        assertFalse(w.fromWaitlist());
        assertEquals("S1003", w.promotedStudentId());                        // first in queue is promoted
        var row = s.courseRows("CS101", null, "S1004").get(0);
        assertEquals(2, row.registered());
        assertEquals("WAITLISTED", row.myState());
        assertEquals(1, row.myPosition());
    }

    @Test void invalidOperationsAreRejectedWithClearKinds() {
        var s = memory();
        User st = user(s, "S1002"), staff = user(s, "T2001");
        assertEquals(DomainException.Kind.NOT_FOUND, assertThrows(DomainException.class, () -> s.register(st, "S1002", "NOPE")).kind());
        s.register(st, "S1002", "CS210");
        assertEquals(DomainException.Kind.CONFLICT, assertThrows(DomainException.class, () -> s.register(st, "S1002", "CS210")).kind());
        assertEquals(DomainException.Kind.FORBIDDEN, assertThrows(DomainException.class, () -> s.register(st, "S1003", "CS210")).kind());
        assertEquals(DomainException.Kind.FORBIDDEN, assertThrows(DomainException.class, () -> s.saveCourse(st, "X1", "X", 5)).kind());
        assertEquals(DomainException.Kind.INVALID, assertThrows(DomainException.class, () -> s.saveCourse(staff, "X1", "X", 0)).kind());
        assertEquals(DomainException.Kind.NOT_FOUND, assertThrows(DomainException.class, () -> s.withdraw(st, "S1002", "NET220")).kind());
        s.setCourseStatus(staff, "NET220", CourseStatus.CLOSED);
        assertEquals(DomainException.Kind.CONFLICT, assertThrows(DomainException.class, () -> s.register(st, "S1002", "NET220")).kind());
        s.setCourseStatus(staff, "NET220", CourseStatus.CANCELLED);
        assertThrows(DomainException.class, () -> s.setCourseStatus(staff, "NET220", CourseStatus.OPEN));   // cancelled is final
    }

    @Test void capacityCannotDropBelowRegistrationsAndRaisingItPromotes() {
        var s = memory();
        User staff = user(s, "T2001");
        s.register(user(s, "S1002"), "S1002", "CS101");
        s.register(user(s, "S1003"), "S1003", "CS101");                      // waiting
        assertThrows(DomainException.class, () -> s.saveCourse(staff, "CS101", "Introduction to Programming", 1));
        s.saveCourse(staff, "CS101", "Introduction to Programming", 3);
        assertEquals(3, s.courseRows("CS101", null, null).get(0).registered());
    }

    @Test void searchAndSortStrategies() {
        var s = memory();
        assertEquals(List.of("CS101", "CS210", "DB201", "ENT301", "NET220"), s.courseRows(null, "code", null).stream().map(r -> r.course().code()).toList());
        assertEquals("Computer Networks", s.courseRows(null, "title", null).get(0).course().title());
        assertEquals("CS210", s.courseRows(null, "seats", null).get(0).course().code());   // 40 seats left
        assertEquals(List.of("CS210", "DB201"), s.courseRows("data", "code", null).stream().map(r -> r.course().code()).toList());
    }

    @Test void requestsAreOrderedByPriorityThenAgeAndFollowStatusRules() {
        var s = memory();
        User st = user(s, "S1002"), staff = user(s, "T2001");
        var low = s.submitRequest(st, "OTHER", Priority.LOW, "later");
        var urgent = s.submitRequest(st, "APPEAL", Priority.URGENT, "now");
        var normal = s.submitRequest(user(s, "S1003"), "TRANSCRIPT", Priority.NORMAL, "soon");
        assertEquals(List.of(urgent.requestId(), normal.requestId(), low.requestId()), s.requestsFor(staff).stream().map(ServiceRequest::requestId).toList());
        assertEquals(2, s.requestsFor(st).size());                           // students see only their own
        assertThrows(DomainException.class, () -> s.advanceRequest(staff, urgent.requestId(), RequestStatus.IN_PROGRESS));  // unassigned
        s.assignRequest(staff, urgent.requestId(), "T2001");
        assertThrows(DomainException.class, () -> s.advanceRequest(staff, urgent.requestId(), RequestStatus.RESOLVED));     // skipping a step
        s.advanceRequest(staff, urgent.requestId(), RequestStatus.IN_PROGRESS);
        s.advanceRequest(staff, urgent.requestId(), RequestStatus.RESOLVED);
        s.advanceRequest(staff, urgent.requestId(), RequestStatus.IN_PROGRESS);        // reopen allowed
        assertThrows(DomainException.class, () -> s.submitRequest(staff, "OTHER", Priority.LOW, "x"));
        assertThrows(DomainException.class, () -> s.submitRequest(st, "UNKNOWN", Priority.LOW, "x"));
    }

    @Test void serviceBoundaryReturnsUnmodifiableCopiesAndAuditsChanges() {
        var store = new InMemoryStore();
        var s = services(store);
        assertThrows(UnsupportedOperationException.class, () -> store.courses().add(null));
        assertThrows(UnsupportedOperationException.class, () -> s.courseRows(null, null, null).clear());
        s.register(user(s, "S1002"), "S1002", "CS210");
        var last = s.auditReport(1).get(0);
        assertEquals("REGISTER", last.action());
        assertEquals("S1002", last.actor());
    }

    @Test void loginUsesHashedPasswords() {
        var s = memory();
        assertTrue(s.login("S1001", SampleData.DEMO_PASSWORD).isPresent());
        assertTrue(s.login("S1001", "wrong").isEmpty());
        assertTrue(s.login("nobody", "x").isEmpty());
        assertNotEquals(Passwords.hash("a"), Passwords.hash("a"));           // random salt
    }

    @Test void announcementsAreFilteredByAudience() {
        var s = memory();
        s.publishAnnouncement(user(s, "T2001"), "Staff meeting", "Friday", Audience.STAFF);
        assertTrue(s.announcementsFor(user(s, "S1001")).stream().noneMatch(a -> a.title().equals("Staff meeting")));
        assertTrue(s.announcementsFor(user(s, "T2001")).stream().anyMatch(a -> a.title().equals("Staff meeting")));
    }
}
