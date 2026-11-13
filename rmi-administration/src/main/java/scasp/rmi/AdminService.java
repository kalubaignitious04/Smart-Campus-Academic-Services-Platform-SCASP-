package scasp.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Stage 8: remote administration interface. Scope is deliberately small:
 * four read-only reports plus one administrative action that needs the admin token.
 * Arguments and results are serializable DTOs (copied by value); the server object itself is passed by remote reference.
 */
public interface AdminService extends Remote {
    record CourseLine(String code, String title, int capacity, int registered, int waiting, String status) implements Serializable {}
    record AuditLine(long id, String actor, String action, String entity, String at) implements Serializable {}

    String ping() throws RemoteException;
    List<CourseLine> capacityReport() throws RemoteException;
    List<CourseLine> waitingListReport() throws RemoteException;
    Map<String, Long> requestWorkload() throws RemoteException;          // HashMap: status -> count
    List<AuditLine> recentAudit(int limit) throws RemoteException;
    /** Opens or closes a course. Requires the admin token printed when the server starts. */
    void setCourseStatus(String adminToken, String courseCode, String status) throws RemoteException;
}
