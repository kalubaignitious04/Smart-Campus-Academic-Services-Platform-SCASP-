package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import scasp.core.Model.Audience;

@WebServlet("/announcements")
public class AnnouncementServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("announcements", Web.services(req).announcementsFor(Web.user(req)));
        req.setAttribute("audiences", java.util.List.of(Audience.values()));
        Web.render(req, resp, "announcements");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.post(req, resp, "/announcements", u -> {
            Web.services(req).publishAnnouncement(u, Web.param(req, "title"), Web.param(req, "body"), Web.enumParam(req, "audience", Audience.class));
            return "Announcement published.";
        });
    }
}
