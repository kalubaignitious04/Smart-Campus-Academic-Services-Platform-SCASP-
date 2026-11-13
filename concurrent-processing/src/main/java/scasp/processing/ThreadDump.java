package scasp.processing;

import java.util.Map;

/** Prints a simple thread dump (name, state, top frames) for the Stage 2 evidence. */
public final class ThreadDump {
    private ThreadDump() {}
    public static String capture() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            sb.append('"').append(e.getKey().getName()).append("\" ").append(e.getKey().getState()).append('\n');
            StackTraceElement[] st = e.getValue();
            for (int i = 0; i < Math.min(st.length, 6); i++) sb.append("    at ").append(st[i]).append('\n');
        }
        return sb.toString();
    }
    public static void main(String[] args) { System.out.print(capture()); }
}
