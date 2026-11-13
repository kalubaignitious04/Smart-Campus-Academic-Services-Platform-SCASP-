package scasp.network;

import static org.junit.jupiter.api.Assertions.*;
import static scasp.core.TestSupport.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import scasp.core.*;
import scasp.core.Model.*;
import scasp.processing.RequestProcessor;

class GatewayTest {
    private InMemoryStore store;
    private AcademicServices s;
    private RequestProcessor processor;
    private GatewayServer server;
    private int port;

    @BeforeEach void start() throws Exception {
        store = new InMemoryStore();
        s = services(store);
        processor = new RequestProcessor(4, 50);
        server = new GatewayServer(s, processor, "127.0.0.1", 0, 600, null);
        port = server.start();
    }
    @AfterEach void stop() { server.close(); processor.close(); }

    private GatewayClient client() throws IOException { return new GatewayClient("127.0.0.1", port, 3000); }

    @Test void basicCommandsAndInfo() throws Exception {
        try (var c = client()) {
            assertEquals("OK|PONG", c.send("PING"));
            assertTrue(c.send("INFO").startsWith("OK|host="));
            assertTrue(c.send("COURSES").contains("CS101,Introduction to Programming,2,1,0,OPEN"));
            assertTrue(c.send("COURSE cs210").startsWith("OK|CS210,Data Structures,40"));
            assertEquals("OK|BYE", c.send("QUIT"));
        }
    }

    @Test void loginRegisterWithdrawAndErrors() throws Exception {
        try (var c = client()) {
            assertEquals("ERR|AUTH_REQUIRED|Please LOGIN first", c.send("REGISTER CS210"));
            assertEquals("ERR|AUTH_FAILED|Invalid credentials", c.send("LOGIN S1002 nope"));
            assertEquals("OK|Mwansa Demo,STUDENT", c.send("LOGIN S1002 Demo#2026"));
            assertEquals("OK|REGISTERED", c.send("REGISTER CS210"));
            assertTrue(c.send("REGISTER CS210").startsWith("ERR|CONFLICT"));
            assertTrue(c.send("REGISTER NOPE").startsWith("ERR|NOT_FOUND"));
            assertEquals("OK|CS210,REGISTERED;", c.send("MYREG"));
            assertEquals("OK|WITHDRAWN", c.send("WITHDRAW CS210"));
            assertTrue(c.send("COURSE").startsWith("ERR|BAD_REQUEST"));
        }
    }

    @Test void malformedMessagesGetErrorsNotCrashes() throws Exception {
        try (var c = client()) {
            assertTrue(c.send("DROP TABLE users").startsWith("ERR|UNKNOWN_COMMAND"));
            assertTrue(c.send("   ").startsWith("ERR|BAD_REQUEST"));
            assertTrue(c.send("LOGIN onlyone").startsWith("ERR|BAD_REQUEST"));
            assertEquals("OK|PONG", c.send("PING"));                              // connection still healthy
        }
        try (var c = client()) {
            assertTrue(c.send("X".repeat(2000)).startsWith("ERR|BAD_REQUEST"));  // oversized line rejected
        }
    }

    @Test void threeFailedLoginsCloseTheConnection() throws Exception {
        try (var c = client()) {
            for (int i = 0; i < 3; i++) assertTrue(c.send("LOGIN S1001 bad").startsWith("ERR|AUTH_FAILED"));
            assertNull(c.send("PING"));
        }
    }

    @Test void abruptDisconnectDoesNotAffectOtherClients() throws Exception {
        Socket rude = new Socket("127.0.0.1", port);
        rude.getOutputStream().write("PIN".getBytes());                           // half a command, then vanish
        rude.setSoLinger(true, 0);
        rude.close();
        try (var c = client()) { assertEquals("OK|PONG", c.send("PING")); }
    }

    @Test void idleClientsAreDisconnectedByTimeout() throws Exception {
        try (var c = client()) {
            Thread.sleep(1000);                                                   // server idle timeout is 600 ms
            assertNull(c.send("PING"));
        }
    }

    @Test void shutdownClosesOpenConnections() throws Exception {
        try (var c = client()) {
            assertEquals("OK|PONG", c.send("PING"));
            server.close();
            assertNull(c.send("PING"));
        }
        assertThrows(IOException.class, () -> new GatewayClient("127.0.0.1", port, 500));
    }

    @Test void manyClientsRegisterAtOnceAndCapacityHolds() throws Exception {
        addStudents(store, 20);
        s.saveCourse(user(s, "T2001"), "TCP1", "TCP race", 4);
        for (int i = 1; i <= 20; i++) store.saveUser(new User(String.format("P%02d", i), "P", Role.STUDENT, true), Passwords.hash("pw"));
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<String>> fs = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String id = String.format("P%02d", i);
            fs.add(pool.submit(() -> { try (var c = client()) { c.send("LOGIN " + id + " pw"); gate.await(); return c.send("REGISTER TCP1"); } }));
        }
        gate.countDown();
        int registered = 0;
        for (var f : fs) if (f.get(20, TimeUnit.SECONDS).equals("OK|REGISTERED")) registered++;
        pool.shutdown();
        assertEquals(4, registered);
        assertEquals(4, store.registrationCount("TCP1"));
    }

    @Test void udpNotificationReachesAListener() throws Exception {
        try (DatagramSocket listener = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             UdpNotifier notifier = new UdpNotifier("127.0.0.1", listener.getLocalPort())) {
            s.notifications().subscribe(notifier);
            s.register(user(s, "S1002"), "S1002", "CS210");
            String msg = UdpNotifier.receiveOne(listener, 3000);
            assertNotNull(msg);
            assertTrue(msg.startsWith("SCASP|REGISTERED|"));
        }
    }

    @Test void fetcherOnlyReadsAuthorisedResourcesAndHandlesFailure() throws Exception {
        var f = new AuthorisedFetcher(Set.of("127.0.0.1:" + port), 500);
        assertThrows(AuthorisedFetcher.FetchException.class, () -> f.fetch(URI.create("http://example.com:80/x")));        // not allow-listed
        assertThrows(AuthorisedFetcher.FetchException.class, () -> f.fetch(URI.create("file:///etc/passwd")));           // wrong scheme
        int closed; try (ServerSocket ss = new ServerSocket(0)) { closed = ss.getLocalPort(); }
        var g = new AuthorisedFetcher(Set.of("127.0.0.1:" + closed), 500);
        assertThrows(AuthorisedFetcher.FetchException.class, () -> g.fetch(URI.create("http://127.0.0.1:" + closed + "/health")));  // unavailable
    }
}
