package scasp.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import scasp.core.DomainException;
import scasp.core.Model.*;

/**
 * Small JSON API for the mobile client (see docs/protocols/http-api.md). Uses the same session login as the web pages.
 * GET /api/courses, /api/my-registrations, /api/campus-services; POST /api/login, /api/register, /api/withdraw.
 */
@WebServlet("/api/*")
public class ApiServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = Web.user(req);
        var s = Web.services(req);
        switch (path(req)) {
            case "/courses" -> send(resp, 200, array(s.courseRows(Web.param(req, "q"), Web.param(req, "sort"), u.userId()).stream()
                    .map(r -> obj("code", r.course().code(), "title", r.course().title(), "capacity", r.course().capacity(),
                            "registered", r.registered(), "waiting", r.waiting(), "status", r.course().status().name(), "myState", r.myState())).toList()));
            case "/my-registrations" -> send(resp, 200, array(s.courseRows(null, "code", u.userId()).stream().filter(r -> !r.myState().isEmpty())
                    .map(r -> obj("code", r.course().code(), "title", r.course().title(), "state", r.myState(), "position", r.myPosition())).toList()));
            case "/campus-services" -> send(resp, 200, array(s.campusServices().stream()
                    .map(c -> obj("name", c.name(), "description", c.description(), "latitude", c.latitude(), "longitude", c.longitude())).toList()));
            default -> send(resp, 404, obj("error", "not found"));
        }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var s = Web.services(req);
        String p = path(req);
        if (p.equals("/login")) {
            var user = s.login(Web.param(req, "userId"), req.getParameter("password"));
            if (user.isEmpty()) { send(resp, 401, obj("error", "wrong user id or password")); return; }
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user.get());
            send(resp, 200, obj("userId", user.get().userId(), "name", user.get().name(), "role", user.get().role().name()));
            return;
        }
        User u = Web.user(req);
        try {
            String code = Web.param(req, "code").toUpperCase();
            switch (p) {
                case "/register" -> send(resp, 200, obj("result", Web.app(req).processor().call(() -> s.register(u, u.userId(), code), 5000).name()));
                case "/withdraw" -> { Web.app(req).processor().call(() -> s.withdraw(u, u.userId(), code), 5000); send(resp, 200, obj("result", "WITHDRAWN")); }
                default -> send(resp, 404, obj("error", "not found"));
            }
        } catch (DomainException e) {
            send(resp, Web.statusFor(e), obj("error", e.getMessage()));
        } catch (scasp.processing.RequestProcessor.RejectedException e) {
            send(resp, 503, obj("error", e.getMessage()));
        }
    }

    private static String path(HttpServletRequest r) { return r.getPathInfo() == null ? "" : r.getPathInfo(); }
    private static void send(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().write(json);
    }

    // ---- minimal JSON writer ----
    private static String array(List<String> items) { return "[" + String.join(",", items) + "]"; }
    private static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(kv[i]).append("\":");
            Object v = kv[i + 1];
            if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append('"').append(String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\r\\n\\t]", " ")).append('"');
        }
        return sb.append('}').toString();
    }
}
