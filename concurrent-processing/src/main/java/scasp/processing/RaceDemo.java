package scasp.processing;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stage 2 evidence: a deliberate check-then-act race and its fix.
 * Run: java -cp out/classes scasp.processing.RaceDemo
 * Capacity is 5; 200 tasks try to take a seat. The unsafe version admits more than 5.
 */
public class RaceDemo {
    static final int CAPACITY = 5, TASKS = 200;

    public static void main(String[] args) throws Exception {
        System.out.println("UNSAFE (no lock):  seats taken = " + run(false) + " (capacity " + CAPACITY + ")");
        System.out.println("FIXED  (locked):   seats taken = " + run(true) + " (capacity " + CAPACITY + ")");
    }

    static int run(boolean locked) throws Exception {
        Set<Integer> seats = ConcurrentHashMap.newKeySet();   // thread-safe set, but check-then-act is not atomic
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (int i = 0; i < TASKS; i++) {
            final int id = i;
            pool.submit(() -> {
                try { gate.await(); } catch (InterruptedException e) { return; }
                if (locked) lock.lock();
                try {
                    if (seats.size() < CAPACITY) {          // check
                        try { Thread.sleep(2); } catch (InterruptedException e) { return; }   // widen the window so the race is visible
                        seats.add(id);                      // act
                    }
                } finally { if (locked) lock.unlock(); }
            });
        }
        gate.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        return seats.size();
    }
}
