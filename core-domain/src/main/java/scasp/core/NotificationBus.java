package scasp.core;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import scasp.core.Model.Notification;

/** Simple publish/subscribe for registration and request events. Keeps the latest 50 for the UI. */
public class NotificationBus {
    private final List<Consumer<Notification>> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedDeque<Notification> recent = new ConcurrentLinkedDeque<>();

    public void subscribe(Consumer<Notification> l) { listeners.add(l); }

    public void publish(String type, String message) {
        Notification n = new Notification(type, message, Instant.now());
        recent.addFirst(n);
        while (recent.size() > 50) recent.pollLast();
        for (Consumer<Notification> l : listeners) {
            try { l.accept(n); } catch (RuntimeException ignored) { /* a bad listener must not break a business operation */ }
        }
    }

    public List<Notification> recent(int limit) { return recent.stream().limit(limit).toList(); }
}
