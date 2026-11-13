package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import scasp.core.DomainException;
import scasp.core.Model.*;

/** Register, withdraw and list a student's own registrations. Actions run on the concurrent RequestProcessor. */
@WebServlet("/registrations")
public class RegistrationServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User u = Web.user(req);
        req.setAttribute("rows", Web.services(req).courseRows(null, "code", u.userId()).stream().filter(r -> !r.myState().isEmpty()).toList());
        Web.render(req, resp, "registrations");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = Web.param(req, "action");
        String code = Web.param(req, "code").toUpperCase();
        String back = "mine".equals(Web.param(req, "from")) ? "/registrations" : "/courses";
        Web.post(req, resp, back, u -> {
            var s = Web.services(req);
            var processor = Web.app(req).processor();
            if (code.isEmpty()) throw new DomainException(DomainException.Kind.INVALID, "Course code is required");
            switch (action) {
                case "register" -> {
                    Outcome o = processor.call(() -> s.register(u, u.userId(), code), 5000);
                    return o == Outcome.REGISTERED ? "You are registered for " + code + "." : code + " is full. You were added to the waiting list.";
                }
                case "withdraw" -> {
                    Withdrawal w = processor.call(() -> s.withdraw(u, u.userId(), code), 5000);
                    return w.fromWaitlist() ? "You left the waiting list for " + code + "." : "You withdrew from " + code + ".";
                }
                default -> throw new DomainException(DomainException.Kind.INVALID, "Unknown action");
            }
        });
    }
}
