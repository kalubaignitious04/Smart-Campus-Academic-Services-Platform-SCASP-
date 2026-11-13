package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import scasp.core.AcademicServices;
import scasp.core.Model.*;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AcademicServices s = Web.services(req);
        User u = Web.user(req);
        var rows = s.courseRows(null, "code", u.userId());
        req.setAttribute("courseCount", rows.size());
        req.setAttribute("myCount", rows.stream().filter(r -> !r.myState().isEmpty()).count());
        req.setAttribute("requestCount", s.requestsFor(u).stream().filter(r -> r.status() != RequestStatus.CLOSED).count());
        req.setAttribute("announcements", s.announcementsFor(u).stream().limit(3).toList());
        req.setAttribute("notifications", u.role().isStaff() ? s.notifications().recent(6) : java.util.List.of());
        Web.render(req, resp, "dashboard");
    }
}
