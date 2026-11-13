package scasp.processing;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Stage 2: runs business actions as tasks on a bounded pool.
 * - platform mode: fixed worker threads + bounded ArrayBlockingQueue; a full queue rejects (back-pressure)
 * - virtual mode: one virtual thread per task, bounded by a semaphore of the same total size
 * Statistics are thread-safe. shutdown() stops accepting work, waits, then interrupts what is left.
 */
public class RequestProcessor implements AutoCloseable {
    public record Stats(long submitted, long succeeded, long failed, long rejected, long timedOut, double avgMillis) {}

    /** Thrown when the processor is saturated or shut down. */
    public static class RejectedException extends RuntimeException {
        public RejectedException(String m) { super(m); }
    }

    private final ExecutorService executor;
    private final Semaphore permits;   // only used in virtual mode
    private final boolean virtual;
    private final LongAdder submitted = new LongAdder(), succeeded = new LongAdder(), failed = new LongAdder(),
            rejected = new LongAdder(), timedOut = new LongAdder(), nanos = new LongAdder();

    public RequestProcessor(int workers, int queueCapacity, boolean virtualThreads) {
        if (workers < 1 || queueCapacity < 1) throw new IllegalArgumentException("workers and queue must be positive");
        this.virtual = virtualThreads;
        if (virtualThreads) {
            this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("scasp-v-", 0).factory());
            this.permits = new Semaphore(workers + queueCapacity);
        } else {
            AtomicInteger n = new AtomicInteger();
            this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueCapacity),
                    r -> { Thread t = new Thread(r, "scasp-worker-" + n.incrementAndGet()); t.setDaemon(true); return t; },
                    new ThreadPoolExecutor.AbortPolicy());
            this.permits = null;
        }
    }

    public RequestProcessor(int workers, int queueCapacity) { this(workers, queueCapacity, false); }

    /** Submits a task. Throws RejectedException when saturated or shut down. */
    public <T> Future<T> submit(Callable<T> task) {
        submitted.increment();
        Callable<T> timed = () -> {
            long start = System.nanoTime();
            try {
                T result = task.call();
                succeeded.increment();
                return result;
            } catch (Exception | Error e) {
                failed.increment();
                throw e;
            } finally {
                nanos.add(System.nanoTime() - start);
                if (virtual) permits.release();
            }
        };
        try {
            if (virtual && !permits.tryAcquire()) throw new RejectedExecutionException("saturated");
            try { return executor.submit(timed); }
            catch (RejectedExecutionException e) { if (virtual) permits.release(); throw e; }
        } catch (RejectedExecutionException e) {
            rejected.increment();
            throw new RejectedException("Server is busy, please try again");
        }
    }

    /**
     * Submits and waits up to timeoutMillis. A DomainException (or any RuntimeException) from the task is rethrown
     * as is, so callers can map it to a user-safe message. Interruption restores the interrupt flag.
     */
    public <T> T call(Callable<T> task, long timeoutMillis) {
        Future<T> f = submit(task);
        try {
            return f.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            timedOut.increment();
            f.cancel(true);
            throw new RejectedException("The request took too long");
        } catch (InterruptedException e) {
            f.cancel(true);
            Thread.currentThread().interrupt();
            throw new RejectedException("Interrupted");
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof RuntimeException re) throw re;
            if (c instanceof Error err) throw err;
            throw new IllegalStateException(c);
        }
    }

    public Stats stats() {
        long done = succeeded.sum() + failed.sum();
        return new Stats(submitted.sum(), succeeded.sum(), failed.sum(), rejected.sum(), timedOut.sum(),
                done == 0 ? 0 : nanos.sum() / 1_000_000.0 / done);
    }

    /** Orderly shutdown: stop intake, wait for running tasks, then interrupt the rest. */
    public void shutdown(long waitMillis) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(waitMillis, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(waitMillis, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override public void close() { shutdown(2000); }
}
