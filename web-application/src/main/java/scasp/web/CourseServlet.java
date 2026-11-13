package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import scasp.core.Model.CourseStatus;

/** Course catalogue: everyone searches and sorts; staff create, update and open/close courses. */
@WebServlet("/courses")
public class CourseServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sort = Web.param(req, "sort");
        if (!sort.isEmpty() && !sort.equals("code") && !sort.equals("title") && !sort.equals("seats")) {
            Web.fail(req, resp, 400, "Unknown sort option.");
            return;
        }
        req.setAttribute("q", Web.param(req, "q"));
        req.setAttribute("sort", sort.isEmpty() ? "code" : sort);
        req.setAttribute("rows", Web.services(req).courseRows(Web.param(req, "q"), sort, Web.user(req).userId()));
        Web.render(req, resp, "courses");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = Web.param(req, "action");
        Web.post(req, resp, "/courses", u -> {
            var s = Web.services(req);
            switch (action) {
                case "save" -> { s.saveCourse(u, Web.param(req, "code"), Web.param(req, "title"), Web.intParam(req, "capacity")); return "Course saved."; }
                case "status" -> { s.setCourseStatus(u, Web.param(req, "code"), Web.enumParam(req, "status", CourseStatus.class)); return "Course status updated."; }
                default -> throw new scasp.core.DomainException(scasp.core.DomainException.Kind.INVALID, "Unknown action");
            }
        });
    }
}
