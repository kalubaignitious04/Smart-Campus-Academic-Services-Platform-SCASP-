package scasp.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import scasp.core.AcademicServices;
import scasp.core.DomainException;
import scasp.core.Model.*;

/** RMI server object. It only talks to AcademicServices, so persistence internals are never exposed. */
public class AdminServer implements AdminService, AutoCloseable {
    public static final String NAME = "SCASPAdmin";
    private final AcademicServices services;
    private final User systemAdmin;
    private final String token;
    private Registry registry;

    public AdminServer(AcademicServices services, User systemAdmin, String token) {
        this.services = services; this.systemAdmin = systemAdmin; this.token = token;
    }

    public static String newToken() {
        byte[] b = new byte[12];
        new SecureRandom().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** Starts a registry on the given port (0 = any free port) and binds this object. Returns the port. */
    public int start(int port) throws RemoteException {
        AdminService stub = (AdminService) UnicastRemoteObject.exportObject(this, 0);
        registryPort = port == 0 ? freePort() : port;
        registry = LocateRegistry.createRegistry(registryPort);
        registry.rebind(NAME, stub);
        return portOf();
    }
    private int portOf() { return registryPort; }
    private int registryPort;
    private int freePort() {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) { return s.getLocalPort(); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    @Override public String ping() { return "SCASP admin service is running"; }

    @Override public List<CourseLine> capacityReport() { return lines(services.capacityReport()); }
    @Override public List<CourseLine> waitingListReport() { return lines(services.waitingListReport()); }

    @Override public Map<String, Long> requestWorkload() {
        Map<String, Long> m = new HashMap<>();
        services.requestsByStatus().forEach((k, v) -> m.put(k.name(), v));
        return m;
    }

    @Override public List<AuditLine> recentAudit(int limit) {
        int n = Math.max(1, Math.min(limit, 100));
        return services.auditReport(n).stream()
                .map(e -> new AuditLine(e.eventId(), e.actor(), e.action(), e.entityType() + "/" + e.entityId(), e.occurredAt().toString()))
                .collect(Collectors.toList());
    }

    @Override public void setCourseStatus(String adminToken, String code, String status) throws RemoteException {
        if (adminToken == null || !MessageDigest.isEqual(adminToken.getBytes(), token.getBytes()))
            throw new RemoteException("Not authorised");
        try {
            services.setCourseStatus(systemAdmin, code, CourseStatus.valueOf(status.toUpperCase()));
        } catch (DomainException | IllegalArgumentException e) {
            throw new RemoteException("Rejected: " + e.getMessage());
        }
    }

    private static List<CourseLine> lines(List<CapacityLine> in) {
        return in.stream().map(c -> new CourseLine(c.code(), c.title(), c.capacity(), c.registered(), c.waiting(), c.status().name()))
                .collect(Collectors.toList());
    }

    @Override public void close() {
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) { }
        try { if (registry != null) UnicastRemoteObject.unexportObject(registry, true); } catch (Exception ignored) { }
    }
}
