package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Staff reports. The role check is repeated here because the filter is only the first gate. */
@WebServlet("/reports")
public class ReportServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!Web.user(req).role().isStaff()) { Web.fail(req, resp, 403, "You do not have access to this page."); return; }
        var s = Web.services(req);
        req.setAttribute("capacity", s.capacityReport());
        req.setAttribute("waiting", s.waitingListReport());
        req.setAttribute("byStatus", s.requestsByStatus());
        req.setAttribute("byAssignee", s.openRequestsByAssignee());
        req.setAttribute("students", s.studentReport());
        req.setAttribute("audit", s.auditReport(15));
        req.setAttribute("stats", Web.app(req).processor().stats());
        req.setAttribute("app", Web.app(req));
        Web.render(req, resp, "reports");
    }
}
