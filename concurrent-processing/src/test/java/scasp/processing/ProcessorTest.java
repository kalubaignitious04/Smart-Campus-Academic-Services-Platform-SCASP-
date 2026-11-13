package scasp.processing;

import static org.junit.jupiter.api.Assertions.*;
import static scasp.core.TestSupport.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import scasp.core.*;
import scasp.core.Model.*;

/** Deterministic: latches release all tasks at once; no sleep-based timing for correctness. */
class ProcessorTest {
    private void raceForSeats(boolean virtual) throws Exception {
        var store = new InMemoryStore();
        var s = services(store);
        addStudents(store, 40);
        s.saveCourse(user(s, "T2001"), "RACE", "Race course", 5);
        try (var p = new RequestProcessor(8, 100, virtual)) {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<Outcome>> fs = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                String id = String.format("P%02d", i);
                fs.add(p.submit(() -> { gate.await(); return s.register(user(s, id), id, "RACE"); }));
            }
            gate.countDown();
            int registered = 0, waiting = 0;
            for (var f : fs) { if (f.get(10, TimeUnit.SECONDS) == Outcome.REGISTERED) registered++; else waiting++; }
            assertEquals(5, registered);
            assertEquals(35, waiting);
            assertEquals(5, store.registrationCount("RACE"));
            assertEquals(35, store.waitlist("RACE").size());
            assertEquals(40, p.stats().succeeded());
            assertEquals(0, p.stats().failed());
        }
    }

    @Test void capacityHoldsUnderConcurrencyPlatformThreads() throws Exception { raceForSeats(false); }
    @Test void capacityHoldsUnderConcurrencyVirtualThreads() throws Exception { raceForSeats(true); }

    @Test void concurrentWithdrawalsPromoteExactlyOncePerSeat() throws Exception {
        var store = new InMemoryStore();
        var s = services(store);
        addStudents(store, 12);
        s.saveCourse(user(s, "T2001"), "W1", "Withdraw race", 4);
        for (int i = 1; i <= 12; i++) { String id = String.format("P%02d", i); s.register(user(s, id), id, "W1"); }
        try (var p = new RequestProcessor(4, 50)) {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<Withdrawal>> fs = new ArrayList<>();
            for (int i = 1; i <= 4; i++) { String id = String.format("P%02d", i); fs.add(p.submit(() -> { gate.await(); return s.withdraw(user(s, id), id, "W1"); })); }
            gate.countDown();
            for (var f : fs) f.get(10, TimeUnit.SECONDS);
        }
        assertEquals(4, store.registrationCount("W1"));
        assertEquals(4, store.waitlist("W1").size());
    }

    @Test void fullQueueRejectsWorkAndStatsCountIt() throws Exception {
        try (var p = new RequestProcessor(1, 1)) {
            CountDownLatch hold = new CountDownLatch(1), started = new CountDownLatch(1);
            Future<?> running = p.submit(() -> { started.countDown(); hold.await(); return 1; });
            started.await();
            Future<?> queued = p.submit(() -> 2);
            assertThrows(RequestProcessor.RejectedException.class, () -> p.submit(() -> 3));
            hold.countDown();
            running.get(5, TimeUnit.SECONDS); queued.get(5, TimeUnit.SECONDS);
            assertEquals(1, p.stats().rejected());
        }
    }

    @Test void timeoutCancelsTheTaskAndReportsIt() throws Exception {
        try (var p = new RequestProcessor(1, 1)) {
            CountDownLatch interrupted = new CountDownLatch(1);
            assertThrows(RequestProcessor.RejectedException.class, () -> p.call(() -> {
                try { new CountDownLatch(1).await(); } catch (InterruptedException e) { interrupted.countDown(); throw e; }
                return 1;
            }, 100));
            assertTrue(interrupted.await(5, TimeUnit.SECONDS), "the running task must be interrupted");
            assertEquals(1, p.stats().timedOut());
        }
    }

    @Test void businessExceptionsPassThroughUnchanged() {
        try (var p = new RequestProcessor(1, 1)) {
            var ex = assertThrows(DomainException.class, () -> p.call(() -> { throw new DomainException(DomainException.Kind.CONFLICT, "full"); }, 1000));
            assertEquals("full", ex.getMessage());
            assertEquals(1, p.stats().failed());
        }
    }

    @Test void orderlyShutdownFinishesRunningWorkThenRefusesNewWork() throws Exception {
        var p = new RequestProcessor(2, 2);
        CountDownLatch started = new CountDownLatch(1);
        Future<String> f = p.submit(() -> { started.countDown(); Thread.sleep(150); return "done"; });
        started.await();
        p.shutdown(5000);
        assertEquals("done", f.get());
        assertThrows(RequestProcessor.RejectedException.class, () -> p.submit(() -> 1));
    }

    @Test void threadDumpListsThreads() { assertTrue(ThreadDump.capture().contains("main")); }
}
