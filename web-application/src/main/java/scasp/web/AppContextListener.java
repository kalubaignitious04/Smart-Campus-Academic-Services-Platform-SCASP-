package scasp.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import scasp.core.AcademicServices;
import scasp.core.CampusStore;
import scasp.core.Model.Role;
import scasp.core.Model.User;
import scasp.core.NotificationBus;
import scasp.jdbc.Database;
import scasp.jdbc.JdbcStore;
import scasp.network.GatewayServer;
import scasp.network.UdpNotifier;
import scasp.processing.RequestProcessor;
import scasp.rmi.AdminServer;

/**
 * Starts the whole platform when the web application starts and stops it in order when it stops:
 * database pool -> services -> request processor -> TCP gateway + UDP notifier -> RMI admin server.
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent e) {
        List<AutoCloseable> closers = new ArrayList<>();
        jakarta.servlet.jsp.JspFactory.getDefaultFactory().getJspApplicationContext(e.getServletContext()).addELResolver(new RecordELResolver());
        try {
            BasicDataSource ds = Database.pooled(AppConfig.get("scasp.db.url", "jdbc:h2:file:./data/scasp;DB_CLOSE_ON_EXIT=FALSE"), 8);
            closers.add(ds);
            Database.init(ds, true);
            CampusStore store = new JdbcStore(ds);
            NotificationBus bus = new NotificationBus();
            AcademicServices services = new AcademicServices(store, bus);
            RequestProcessor processor = new RequestProcessor(AppConfig.getInt("scasp.workers", 4), AppConfig.getInt("scasp.queue", 100),
                    Boolean.parseBoolean(AppConfig.get("scasp.virtual", "false")));
            closers.add(0, processor);

            int udpPort = AppConfig.getInt("scasp.udp.port", 9091);
            UdpNotifier udp = new UdpNotifier(AppConfig.get("scasp.udp.host", "localhost"), udpPort);
            bus.subscribe(udp);
            closers.add(0, udp);

            int webPort = AppConfig.getInt("scasp.web.port", 8080);
            GatewayServer gateway = new GatewayServer(services, processor, AppConfig.get("scasp.bind", "127.0.0.1"),
                    AppConfig.getInt("scasp.gateway.port", 9090), 60_000, URI.create("http://localhost:" + webPort + "/health"));
            int gatewayPort = gateway.start();
            closers.add(0, gateway);

            String token = AppConfig.get("scasp.admin.token", AdminServer.newToken());
            User systemAdmin = store.findUser("A3001").orElse(new User("A3001", "Admin", Role.ADMIN, true));
            AdminServer rmi = new AdminServer(services, systemAdmin, token);
            int rmiPort = rmi.start(AppConfig.getInt("scasp.rmi.port", 1099));
            closers.add(0, rmi);

            e.getServletContext().setAttribute(AppContext.KEY, new AppContext(services, processor, () -> closeAll(closers),
                    gatewayPort, rmiPort, udpPort, token));
            System.out.println("SCASP started. TCP gateway " + gatewayPort + ", RMI registry " + rmiPort + ", UDP notifications " + udpPort);
            System.out.println("RMI admin token (for status changes): " + token);
        } catch (Exception ex) {
            closeAll(closers);
            throw new IllegalStateException("SCASP failed to start: " + ex.getMessage(), ex);
        }
    }

    @Override public void contextDestroyed(ServletContextEvent e) {
        AppContext ctx = AppContext.of(e.getServletContext());
        if (ctx != null) try { ctx.shutdownHook().close(); } catch (Exception ignored) { }
    }

    private static void closeAll(List<AutoCloseable> closers) {
        for (AutoCloseable c : closers) { try { c.close(); } catch (Exception ignored) { } }
        closers.clear();
    }
}
