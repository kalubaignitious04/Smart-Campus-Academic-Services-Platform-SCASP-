package scasp.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

/** First filter: UTF-8 everywhere and basic security headers. */
@WebFilter(urlPatterns = "/*", filterName = "A-encoding")
public class EncodingFilter implements Filter {
    @Override public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        if (resp instanceof jakarta.servlet.http.HttpServletResponse r) {
            r.setHeader("X-Content-Type-Options", "nosniff");
            r.setHeader("X-Frame-Options", "DENY");
            r.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(req, resp);
    }
}
