package scasp.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import scasp.core.AcademicServices;
import scasp.core.DomainException;
import scasp.core.Model.*;
import scasp.processing.RequestProcessor;

/**
 * Stage 3: TCP gateway. One UTF-8 line per request, one line per response (see docs/protocols/gateway-protocol.md).
 * Connections run on virtual threads (cheap to hold open); every business action runs on the shared
 * RequestProcessor, so it uses the same bounded Stage 2 execution model as the web application.
 */
public class GatewayServer implements AutoCloseable {
    static final int MAX_LINE = 512;
    private final AcademicServices services;
    private final RequestProcessor processor;
    private final String bindHost;
    private final int requestedPort;
    private final int idleTimeoutMillis;
    private final AuthorisedFetcher fetcher;
    private final URI healthUri;
    private final Set<Socket> open = ConcurrentHashMap.newKeySet();
    private final ExecutorService connections = Executors.newVirtualThreadPerTaskExecutor();
    private ServerSocket server;
    private Thread acceptor;
    private volatile boolean running;

    public GatewayServer(AcademicServices services, RequestProcessor processor, String bindHost, int port,
                         int idleTimeoutMillis, URI healthUri) {
        this.services = services; this.processor = processor; this.bindHost = bindHost;
        this.requestedPort = port; this.idleTimeoutMillis = idleTimeoutMillis; this.healthUri = healthUri;
        this.fetcher = healthUri == null ? null : new AuthorisedFetcher(Set.of(healthUri.getHost() + ":" + healthUri.getPort()), 2000);
    }

    public int start() throws IOException {
        server = new ServerSocket(requestedPort, 50, InetAddress.getByName(bindHost));
        running = true;
        acceptor = new Thread(this::acceptLoop, "gateway-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        return server.getLocalPort();
    }

    public int port() { return server.getLocalPort(); }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = server.accept();
                open.add(s);
                connections.submit(() -> handle(s));
            } catch (IOException e) {
                if (running) System.err.println("Gateway accept failed: " + e.getMessage());
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            socket.setSoTimeout(idleTimeoutMillis);
            User[] user = new User[1];
            int[] failedLogins = new int[1];
            while (running) {
                String line = readLine(in);
                if (line == null) return;                              // client disconnected
                if (line.length() > MAX_LINE) { out.println("ERR|BAD_REQUEST|Line too long"); return; }
                String reply = dispatch(line, user, failedLogins);
                out.println(reply);
                if (reply.equals("OK|BYE") || failedLogins[0] >= 3) return;
            }
        } catch (SocketTimeoutException e) {
            // idle client: closed by try-with-resources
        } catch (IOException e) {
            // broken connection: nothing more to send
        } finally {
            open.remove(socket);
        }
    }

    /** Reads up to MAX_LINE+1 chars so a client cannot make us buffer an endless line. */
    static String readLine(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = in.read()) != -1) {
            if (ch == '\n') return sb.toString().stripTrailing();
            if (sb.length() > MAX_LINE) return sb.toString();
            sb.append((char) ch);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    String dispatch(String line, User[] user, int[] failedLogins) {
        String[] p = line.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty()) return "ERR|BAD_REQUEST|Empty command";
        try {
            switch (p[0].toUpperCase()) {
                case "PING": return "OK|PONG";
                case "QUIT": return "OK|BYE";
                case "INFO": {
                    InetAddress local = InetAddress.getLocalHost();
                    return "OK|host=" + local.getHostName() + ";address=" + local.getHostAddress() + ";port=" + port();
                }
                case "STATUS": {
                    if (fetcher == null) return "ERR|UNAVAILABLE|No web application configured";
                    try { return "OK|web=" + (fetcher.fetch(healthUri).strip()); }
                    catch (AuthorisedFetcher.FetchException e) { return "OK|web=DOWN"; }
                }
                case "COURSES": {
                    StringBuilder sb = new StringBuilder("OK|");
                    for (CourseRow r : services.courseRows(null, "code", null)) sb.append(row(r)).append(';');
                    return sb.toString();
                }
                case "COURSE": {
                    if (p.length != 2) return "ERR|BAD_REQUEST|Usage: COURSE <code>";
                    return "OK|" + services.courseRows(p[1], "code", null).stream()
                            .filter(r -> r.course().code().equalsIgnoreCase(p[1])).findFirst().map(GatewayServer::row)
                            .orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND, "Course not found"));
                }
                case "ANNOUNCEMENTS": {
                    if (user[0] == null) return "ERR|AUTH_REQUIRED|Please LOGIN first";
                    StringBuilder sb = new StringBuilder("OK|");
                    for (Announcement a : services.announcementsFor(user[0]))
                        sb.append(clean(a.title())).append(',').append(clean(a.body())).append(';');
                    return sb.toString();
                }
                case "LOGIN": {
                    if (p.length != 3) return "ERR|BAD_REQUEST|Usage: LOGIN <userId> <password>";
                    var u = services.login(p[1], p[2]);
                    if (u.isEmpty()) { failedLogins[0]++; return "ERR|AUTH_FAILED|Invalid credentials"; }
                    user[0] = u.get();
                    return "OK|" + clean(u.get().name()) + "," + u.get().role();
                }
                case "REGISTER": case "WITHDRAW": case "MYREG": {
                    if (user[0] == null) return "ERR|AUTH_REQUIRED|Please LOGIN first";
                    User me = user[0];
                    if (p[0].equalsIgnoreCase("MYREG")) {
                        StringBuilder sb = new StringBuilder("OK|");
                        for (CourseRow r : services.courseRows(null, "code", me.userId()))
                            if (!r.myState().isEmpty()) sb.append(r.course().code()).append(',').append(r.myState()).append(';');
                        return sb.toString();
                    }
                    if (p.length != 2) return "ERR|BAD_REQUEST|Usage: " + p[0].toUpperCase() + " <courseCode>";
                    String code = p[1].toUpperCase();
                    if (p[0].equalsIgnoreCase("REGISTER"))
                        return "OK|" + processor.call(() -> services.register(me, me.userId(), code), 5000);
                    Withdrawal w = processor.call(() -> services.withdraw(me, me.userId(), code), 5000);
                    return "OK|" + (w.fromWaitlist() ? "LEFT_WAITLIST" : "WITHDRAWN");
                }
                default: return "ERR|UNKNOWN_COMMAND|" + clean(p[0]);
            }
        } catch (DomainException e) {
            return "ERR|" + e.kind() + "|" + clean(e.getMessage());
        } catch (RequestProcessor.RejectedException e) {
            return "ERR|BUSY|" + clean(e.getMessage());
        } catch (RuntimeException | IOException e) {
            System.err.println("Gateway error: " + e);
            return "ERR|SERVER_ERROR|Something went wrong";
        }
    }

    private static String row(CourseRow r) {
        return clean(r.course().code()) + "," + clean(r.course().title()) + "," + r.course().capacity() + ","
                + r.registered() + "," + r.waiting() + "," + r.course().status();
    }
    static String clean(String s) { return s == null ? "" : s.replaceAll("[|;,\\r\\n]", " "); }

    @Override public void close() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        for (Socket s : open) { try { s.close(); } catch (IOException ignored) { } }
        connections.shutdown();
    }
}
