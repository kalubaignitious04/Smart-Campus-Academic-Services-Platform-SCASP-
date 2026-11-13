package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Optional;
import scasp.core.Model.User;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.render(req, resp, "login");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Optional<User> user = Web.services(req).login(Web.param(req, "userId"), req.getParameter("password"));
        if (user.isEmpty()) {
            req.setAttribute("loginError", "Wrong user ID or password.");
            req.setAttribute("userId", Web.param(req, "userId"));
            resp.setStatus(401);
            Web.render(req, resp, "login");
            return;
        }
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();                       // new session after login (prevents session fixation)
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(20 * 60);
        session.setAttribute("user", user.get());
        session.setAttribute("csrf", AuthFilter.newToken());
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }
}
