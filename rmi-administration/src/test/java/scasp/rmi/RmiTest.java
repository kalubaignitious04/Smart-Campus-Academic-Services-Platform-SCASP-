package scasp.rmi;

import static org.junit.jupiter.api.Assertions.*;
import static scasp.core.TestSupport.*;

import java.rmi.RemoteException;
import org.junit.jupiter.api.Test;
import scasp.core.*;

class RmiTest {
    @Test void clientReadsReportsChangesCourseWithTokenAndHandlesFailures() throws Exception {
        var s = memory();
        s.register(user(s, "S1002"), "S1002", "CS101");
        s.register(user(s, "S1003"), "S1003", "CS101");                           // waiting
        String token = AdminServer.newToken();
        int port;
        try (var server = new AdminServer(s, user(s, "A3001"), token)) {
            port = server.start(0);
            AdminService remote = AdminClient.connect("localhost", port);
            assertTrue(remote.ping().contains("running"));
            var cs101 = remote.capacityReport().stream().filter(c -> c.code().equals("CS101")).findFirst().orElseThrow();
            assertEquals(2, cs101.registered());
            assertEquals(1, cs101.waiting());
            assertEquals(1, remote.waitingListReport().size());
            assertEquals(0L, remote.requestWorkload().get("SUBMITTED"));
            assertFalse(remote.recentAudit(5).isEmpty());

            assertThrows(RemoteException.class, () -> remote.setCourseStatus("wrong", "CS101", "CLOSED"));          // bad token
            assertThrows(RemoteException.class, () -> remote.setCourseStatus(token, "CS101", "NOT_A_STATUS"));       // bad value
            remote.setCourseStatus(token, "CS101", "CLOSED");
            assertEquals("CLOSED", remote.capacityReport().stream().filter(c -> c.code().equals("CS101")).findFirst().orElseThrow().status());
        }
        final int closedPort = port;
        assertThrows(RemoteException.class, () -> AdminClient.connect("localhost", closedPort).ping());           // server gone
    }
}
