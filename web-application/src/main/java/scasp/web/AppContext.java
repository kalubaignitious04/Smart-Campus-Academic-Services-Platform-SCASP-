package scasp.web;

import jakarta.servlet.ServletContext;
import java.io.Closeable;
import scasp.core.AcademicServices;
import scasp.processing.RequestProcessor;

/** Shared application objects, created once by AppContextListener and kept in the ServletContext. */
public record AppContext(AcademicServices services, RequestProcessor processor, Closeable shutdownHook,
                         int gatewayPort, int rmiPort, int udpPort, String adminToken) {
    static final String KEY = "scasp.app";
    public static AppContext of(ServletContext c) { return (AppContext) c.getAttribute(KEY); }
}
