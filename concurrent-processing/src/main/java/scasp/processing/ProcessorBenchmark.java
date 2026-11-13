package scasp.processing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Stage 2 comparison: the same blocking workload (simulated 20 ms I/O wait per task) on a platform-thread
 * pool versus virtual-thread-per-task. Optional argument: file to write a thread dump taken mid-run.
 */
public class ProcessorBenchmark {
    static final int TASKS = 2000, IO_MILLIS = 20;

    public static void main(String[] args) throws Exception {
        System.out.println("Workload: " + TASKS + " tasks, each waits " + IO_MILLIS + " ms (simulated database/network I/O)");
        System.out.println("platform pool, 16 workers : " + run(new RequestProcessor(16, TASKS, false), args.length > 0 ? Path.of(args[0]) : null) + " ms");
        System.out.println("virtual thread per task   : " + run(new RequestProcessor(16, TASKS, true), null) + " ms");
    }

    static long run(RequestProcessor p, Path dumpFile) throws Exception {
        long start = System.nanoTime();
        List<Future<Integer>> fs = new ArrayList<>();
        for (int i = 0; i < TASKS; i++) fs.add(p.submit(() -> { Thread.sleep(IO_MILLIS); return 1; }));
        if (dumpFile != null) {
            Thread.sleep(100);                                         // workers are busy now
            Files.writeString(dumpFile, ThreadDump.capture());
        }
        for (Future<Integer> f : fs) f.get();
        long ms = (System.nanoTime() - start) / 1_000_000;
        p.close();
        return ms;
    }
}
