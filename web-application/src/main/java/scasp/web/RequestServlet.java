package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import scasp.core.AcademicServices;
import scasp.core.DomainException;
import scasp.core.Model.*;

/** Academic service requests: students submit and track; staff assign and move them along. */
@WebServlet("/requests")
public class RequestServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var s = Web.services(req);
        User u = Web.user(req);
        req.setAttribute("requests", s.requestsFor(u));
        req.setAttribute("categories", AcademicServices.REQUEST_CATEGORIES);
        req.setAttribute("priorities", java.util.List.of(Priority.values()));
        req.setAttribute("statuses", java.util.List.of(RequestStatus.values()));
        req.setAttribute("staff", s.staff());
        Web.render(req, resp, "requests");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = Web.param(req, "action");
        Web.post(req, resp, "/requests", u -> {
            var s = Web.services(req);
            switch (action) {
                case "submit" -> {
                    ServiceRequest r = s.submitRequest(u, Web.param(req, "category"), Web.enumParam(req, "priority", Priority.class), Web.param(req, "details"));
                    return "Request #" + r.requestId() + " submitted.";
                }
                case "assign" -> { s.assignRequest(u, longParam(req, "id"), Web.param(req, "assignee")); return "Request assigned."; }
                case "advance" -> { s.advanceRequest(u, longParam(req, "id"), Web.enumParam(req, "status", RequestStatus.class)); return "Request updated."; }
                default -> throw new DomainException(DomainException.Kind.INVALID, "Unknown action");
            }
        });
    }

    private static long longParam(HttpServletRequest r, String n) {
        try { return Long.parseLong(Web.param(r, n)); }
        catch (NumberFormatException e) { throw new DomainException(DomainException.Kind.INVALID, "Invalid request number"); }
    }
}
