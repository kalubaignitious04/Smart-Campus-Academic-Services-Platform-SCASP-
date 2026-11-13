package scasp.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Command-line administration client.
 * Usage: AdminClient <host> <port> <ping|capacity|waiting|workload|audit|status> [args]
 *   status <token> <courseCode> <OPEN|CLOSED|CANCELLED>
 */
public class AdminClient {
    public static AdminService connect(String host, int port) throws RemoteException, NotBoundException {
        Registry r = LocateRegistry.getRegistry(host, port);
        return (AdminService) r.lookup(AdminServer.NAME);
    }

    public static void main(String[] a) {
        if (a.length < 3) { System.out.println("Usage: AdminClient <host> <port> <ping|capacity|waiting|workload|audit|status> [args]"); return; }
        try {
            AdminService s = connect(a[0], Integer.parseInt(a[1]));
            switch (a[2]) {
                case "ping" -> System.out.println(s.ping());
                case "capacity" -> s.capacityReport().forEach(c -> System.out.printf("%-8s %-30s %d/%d  waiting %d  %s%n", c.code(), c.title(), c.registered(), c.capacity(), c.waiting(), c.status()));
                case "waiting" -> s.waitingListReport().forEach(c -> System.out.printf("%-8s waiting %d%n", c.code(), c.waiting()));
                case "workload" -> s.requestWorkload().forEach((k, v) -> System.out.println(k + ": " + v));
                case "audit" -> s.recentAudit(10).forEach(e -> System.out.println(e.at() + " " + e.actor() + " " + e.action() + " " + e.entity()));
                case "status" -> { s.setCourseStatus(a[3], a[4], a[5]); System.out.println("Done"); }
                default -> System.out.println("Unknown command " + a[2]);
            }
        } catch (NotBoundException e) {
            System.err.println("The server is running but the admin service is not registered.");
        } catch (RemoteException e) {
            System.err.println("Cannot reach or use the admin server: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.err.println("Check the command arguments.");
        }
    }
}
