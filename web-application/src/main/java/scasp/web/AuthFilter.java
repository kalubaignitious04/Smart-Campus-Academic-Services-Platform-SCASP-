package scasp.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import scasp.core.Model.User;

/**
 * Authentication, role checks, CSRF check and request logging for every URL.
 * Public: /login, /health, /assets/*, /api/login. Staff only: /reports and the POST actions staff perform
 * (their servlets also check the role again, so the filter is a first gate, not the only one).
 */
@WebFilter(urlPatterns = "/*", filterName = "B-auth")
public class AuthFilter implements Filter {
    private static final Set<String> PUBLIC = Set.of("/login", "/health", "/api/login");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        long start = System.nanoTime();
        try {
            if (path.equals("/")) { resp.sendRedirect(req.getContextPath() + "/dashboard"); return; }
            boolean isPublic = PUBLIC.contains(path) || path.startsWith("/assets/");
            HttpSession session = req.getSession(false);
            User user = session == null ? null : (User) session.getAttribute("user");
            if (!isPublic) {
                if (user == null) {
                    if (path.startsWith("/api/")) { json(resp, 401, "{\"error\":\"login required\"}"); }
                    else resp.sendRedirect(req.getContextPath() + "/login");
                    return;
                }
                if (path.startsWith("/reports") && !user.role().isStaff()) {
                    if (path.startsWith("/api/")) json(resp, 403, "{\"error\":\"forbidden\"}");
                    else { req.setAttribute("errorStatus", 403); req.setAttribute("errorMessage", "You do not have access to this page.");
                        resp.setStatus(403); req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp); }
                    return;
                }
                if (req.getMethod().equals("POST") && !path.startsWith("/api/") && !validCsrf(req, session)) {
                    resp.setStatus(403);
                    req.setAttribute("errorStatus", 403);
                    req.setAttribute("errorMessage", "Your session form expired. Go back, refresh and try again.");
                    req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
                    return;
                }
                if (session.getAttribute("csrf") == null) session.setAttribute("csrf", newToken());
            }
            chain.doFilter(req, resp);
        } finally {
            req.getServletContext().log(req.getMethod() + " " + path + " -> " + resp.getStatus() + " ("
                    + (System.nanoTime() - start) / 1_000_000 + " ms)");
        }
    }

    static String newToken() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static boolean validCsrf(HttpServletRequest req, HttpSession session) {
        Object expected = session.getAttribute("csrf");
        String given = req.getParameter("csrf");
        return expected != null && given != null && MessageDigest.isEqual(expected.toString().getBytes(), given.getBytes());
    }

    private static void json(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().write(body);
    }
}
