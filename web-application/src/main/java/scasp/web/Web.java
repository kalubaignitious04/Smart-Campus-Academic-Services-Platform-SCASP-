package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import scasp.core.AcademicServices;
import scasp.core.DomainException;
import scasp.core.Model.User;
import scasp.processing.RequestProcessor;

/** Small helpers shared by the servlets (not a servlet itself). */
final class Web {
    private Web() {}
    static AppContext app(HttpServletRequest r) { return AppContext.of(r.getServletContext()); }
    static AcademicServices services(HttpServletRequest r) { return app(r).services(); }
    static User user(HttpServletRequest r) { return (User) r.getSession().getAttribute("user"); }

    static void render(HttpServletRequest req, HttpServletResponse resp, String view) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {                                  // show one-time messages once, then drop them
            for (String k : new String[]{"flash", "flashError"}) {
                Object v = session.getAttribute(k);
                if (v != null) { req.setAttribute(k, v); session.removeAttribute(k); }
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /** Shows the error page with a status code and a message that is safe for users. */
    static void fail(HttpServletRequest req, HttpServletResponse resp, int status, String message) throws ServletException, IOException {
        resp.setStatus(status);
        req.setAttribute("errorStatus", status);
        req.setAttribute("errorMessage", message);
        render(req, resp, "error");
    }

    static int statusFor(DomainException e) {
        return switch (e.kind()) { case INVALID -> 400; case FORBIDDEN -> 403; case NOT_FOUND -> 404; case CONFLICT -> 409; };
    }

    /** Runs a state-changing action through the processor, then redirects (Post/Redirect/Get) with a flash message. */
    interface Action { String run(User u) throws Exception; }
    static void post(HttpServletRequest req, HttpServletResponse resp, String redirectTo, Action action) throws ServletException, IOException {
        try {
            String msg = action.run(user(req));
            req.getSession().setAttribute("flash", msg);
            resp.sendRedirect(req.getContextPath() + redirectTo);
        } catch (DomainException e) {
            // Business-rule problems are shown on the same page after redirect, so refresh does not resubmit.
            if (e.kind() == DomainException.Kind.FORBIDDEN) fail(req, resp, 403, e.getMessage());
            else if (e.kind() == DomainException.Kind.INVALID) fail(req, resp, 400, e.getMessage());
            else if (e.kind() == DomainException.Kind.NOT_FOUND) fail(req, resp, 404, e.getMessage());
            else { req.getSession().setAttribute("flashError", e.getMessage()); resp.sendRedirect(req.getContextPath() + redirectTo); }
        } catch (RequestProcessor.RejectedException e) {
            fail(req, resp, 503, e.getMessage());
        } catch (Exception e) {
            req.getServletContext().log("Unexpected error", e);
            fail(req, resp, 500, "Something went wrong. Please try again.");
        }
    }

    static String param(HttpServletRequest r, String name) {
        String v = r.getParameter(name);
        return v == null ? "" : v.trim();
    }
    static int intParam(HttpServletRequest r, String name) {
        try { return Integer.parseInt(param(r, name)); }
        catch (NumberFormatException e) { throw new DomainException(DomainException.Kind.INVALID, "Please enter a valid number for " + name); }
    }
    static <E extends Enum<E>> E enumParam(HttpServletRequest r, String name, Class<E> type) {
        try { return Enum.valueOf(type, param(r, name).toUpperCase()); }
        catch (IllegalArgumentException e) { throw new DomainException(DomainException.Kind.INVALID, "Invalid value for " + name); }
    }
}
