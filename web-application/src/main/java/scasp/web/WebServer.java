package scasp.web;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/** Starts the SCASP web application on an embedded Tomcat. Run: scripts run-web (or java scasp.web.WebServer). */
public class WebServer implements AutoCloseable {
    private final Tomcat tomcat = new Tomcat();
    private final Path baseDir;

    public WebServer() throws Exception { baseDir = Files.createTempDirectory("scasp-tomcat"); }

    /** Starts the server; port 0 picks a free port. Returns the port in use. */
    public int start(int port, File webappDir, File classesDir) throws LifecycleException {
        tomcat.setBaseDir(baseDir.toString());
        tomcat.setPort(port);
        tomcat.setHostname("localhost");
        Connector c = tomcat.getConnector();
        c.setProperty("address", AppConfig.get("scasp.bind", "127.0.0.1"));
        Context ctx = tomcat.addWebapp("", webappDir.getAbsolutePath());
        ctx.setSessionTimeout(20);
        ctx.setUseHttpOnly(true);
        WebResourceRoot res = new StandardRoot(ctx);
        res.addPreResources(new DirResourceSet(res, "/WEB-INF/classes", classesDir.getAbsolutePath(), "/"));
        ctx.setResources(res);
        tomcat.start();
        return tomcat.getConnector().getLocalPort();
    }

    @Override public void close() {
        try { tomcat.stop(); tomcat.destroy(); } catch (LifecycleException ignored) { }
    }

    private static File resolveDir(String property, String relative) {
        String configured = AppConfig.get(property, "");
        if (!configured.isBlank()) {
            File f = new File(configured);
            if (f.exists()) return f.getAbsoluteFile();
        }

        File cwd = new File("").getAbsoluteFile();
        File candidate = new File(cwd, relative);
        if (candidate.exists()) return candidate.getAbsoluteFile();

        try {
            URI codeLocation = WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path classesDir = Path.of(codeLocation).toAbsolutePath();
            Path projectRoot = classesDir;
            if (classesDir.getFileName() != null && classesDir.getFileName().toString().equals("classes")) {
                projectRoot = classesDir.getParent().getParent();
            }
            File fallback = projectRoot.resolve(relative).toFile();
            if (fallback.exists()) return fallback.getAbsoluteFile();
        } catch (Exception ignored) {
            // Fall through to the relative path from the current working directory.
        }

        return candidate.getAbsoluteFile();
    }

    public static void main(String[] args) throws Exception {
        File webapp = resolveDir("scasp.webapp", "web-application/src/main/webapp");
        File classes = resolveDir("scasp.classes", "out/classes");
        WebServer server = new WebServer();
        int port = server.start(AppConfig.getInt("scasp.web.port", 8080), webapp, classes);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("SCASP web interface: http://localhost:" + port + "/   (demo password for every account: Demo#2026)");
        System.out.println("Demo users: S1001 (student), T2001 (academic staff), A3001 (admin)");
        server.tomcat.getServer().await();
    }
}
