package scasp.web;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.net.CookieManager;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import org.junit.jupiter.api.*;

/** Starts the real application (Tomcat, JSP, JDBC on in-memory H2, gateway, RMI) and drives it over HTTP. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebTest {
    private WebServer server;
    private String base;

    @BeforeAll void start() throws Exception {
        System.setProperty("scasp.db.url", "jdbc:h2:mem:webtest;DB_CLOSE_DELAY=-1");
        System.setProperty("scasp.gateway.port", "0");
        System.setProperty("scasp.rmi.port", "0");
        server = new WebServer();
        base = "http://localhost:" + server.start(0, new File("web-application/src/main/webapp"), new File("out/classes"));
    }
    @AfterAll void stop() { server.close(); }

    /** A browser-like client: keeps cookies, never follows redirects so we can see them. */
    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private HttpResponse<String> get(HttpClient c, String path) throws Exception {
        return c.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(HttpClient c, String path, String... kv) throws Exception {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) b.append(i > 0 ? "&" : "").append(kv[i]).append('=').append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        return c.send(HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(b.toString())).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient login(String id) throws Exception {
        HttpClient c = browser();
        assertEquals(302, post(c, "/login", "userId", id, "password", "Demo#2026").statusCode());
        return c;
    }
    private String csrf(HttpClient c) throws Exception {
        Matcher m = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(get(c, "/courses").body());
        assertTrue(m.find());
        return m.group(1);
    }

    @Test void healthIsPublicAndProtectedPagesRedirectToLogin() throws Exception {
        assertEquals("UP", get(browser(), "/health").body());
        var r = get(browser(), "/courses");
        assertEquals(302, r.statusCode());
        assertTrue(r.headers().firstValue("Location").orElse("").endsWith("/login"));
        assertEquals(302, get(browser(), "/").statusCode());
    }

    @Test void wrongPasswordGives401AndNoSession() throws Exception {
        HttpClient c = browser();
        var r = post(c, "/login", "userId", "S1001", "password", "bad");
        assertEquals(401, r.statusCode());
        assertTrue(r.body().contains("Wrong user ID or password"));
        assertEquals(302, get(c, "/courses").statusCode());
    }

    @Test void studentRegistersJoinsWaitingListAndSeesIt() throws Exception {
        HttpClient a = login("S1002"), b = login("S1003");
        assertEquals(302, post(a, "/registrations", "csrf", csrf(a), "action", "register", "code", "CS101").statusCode());
        assertTrue(get(a, "/registrations").body().contains("You are registered for CS101"));
        assertEquals(302, post(b, "/registrations", "csrf", csrf(b), "action", "register", "code", "CS101").statusCode());
        String page = get(b, "/registrations").body();
        assertTrue(page.contains("added to the waiting list"));
        assertTrue(page.contains("Waiting list, position 1"));
        // duplicate registration is a business conflict shown on the page, not a crash
        post(b, "/registrations", "csrf", csrf(b), "action", "register", "code", "CS101");
        assertTrue(get(b, "/courses").body().contains("Already registered or on the waiting list"));
    }

    @Test void securityControlsCsrfRolesInputAndMissingResources() throws Exception {
        HttpClient s = login("S1004");
        assertEquals(403, post(s, "/registrations", "action", "register", "code", "CS210").statusCode());           // no CSRF token
        assertEquals(403, get(s, "/reports").statusCode());                                                          // student, staff page
        assertEquals(403, post(s, "/courses", "csrf", csrf(s), "action", "save", "code", "X", "title", "X", "capacity", "5").statusCode());
        assertEquals(400, get(s, "/courses?sort=evil").statusCode());
        assertEquals(400, post(s, "/registrations", "csrf", csrf(s), "action", "register", "code", "").statusCode());
        assertEquals(404, post(s, "/registrations", "csrf", csrf(s), "action", "register", "code", "NOPE").statusCode());
        assertEquals(404, get(s, "/does-not-exist").statusCode());
        HttpClient t = login("T2001");
        assertEquals(400, post(t, "/courses", "csrf", csrf(t), "action", "save", "code", "N1", "title", "T", "capacity", "abc").statusCode());
        assertEquals(200, get(t, "/reports").statusCode());
    }

    @Test void untrustedTextIsEscapedInPages() throws Exception {
        HttpClient s = login("S1005");
        post(s, "/requests", "csrf", csrf(s), "action", "submit", "category", "OTHER", "priority", "LOW", "details", "<script>alert(1)</script>");
        String page = get(s, "/requests").body();
        assertTrue(page.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(page.contains("<script>alert(1)"));
    }

    @Test void staffManagesRequestsAnnouncementsAndSeesReports() throws Exception {
        HttpClient st = login("S1001"), t = login("T2001");
        post(st, "/requests", "csrf", csrf(st), "action", "submit", "category", "TRANSCRIPT", "priority", "HIGH", "details", "Need a transcript");
        Matcher id = Pattern.compile("<td>(\\d+)</td><td>S1001</td>").matcher(get(t, "/requests").body());
        assertTrue(id.find());
        post(t, "/requests", "csrf", csrf(t), "action", "assign", "id", id.group(1), "assignee", "T2001");
        post(t, "/requests", "csrf", csrf(t), "action", "advance", "id", id.group(1), "status", "IN_PROGRESS");
        assertTrue(get(t, "/requests").body().contains("IN_PROGRESS"));
        post(t, "/announcements", "csrf", csrf(t), "title", "Exam timetable", "body", "Published today", "audience", "ALL");
        assertTrue(get(st, "/announcements").body().contains("Exam timetable"));
        String report = get(t, "/reports").body();
        assertTrue(report.contains("Course capacity") && report.contains("Audit trail") && report.contains("ASSIGN_REQUEST"));
    }

    @Test void logoutEndsTheSession() throws Exception {
        HttpClient c = login("S1001");
        assertEquals(200, get(c, "/dashboard").statusCode());
        assertEquals(302, post(c, "/logout", "csrf", csrf(c)).statusCode());
        assertEquals(302, get(c, "/dashboard").statusCode());
    }

    @Test void jsonApiForTheMobileClient() throws Exception {
        HttpClient c = browser();
        assertEquals(401, get(c, "/api/courses").statusCode());
        assertEquals(401, post(c, "/api/login", "userId", "S1001", "password", "bad").statusCode());
        assertEquals(200, post(c, "/api/login", "userId", "S1001", "password", "Demo#2026").statusCode());
        var courses = get(c, "/api/courses");
        assertEquals(200, courses.statusCode());
        assertTrue(courses.body().contains("\"code\":\"CS101\""));
        assertEquals(409, post(c, "/api/register", "code", "CS101").statusCode());                      // already registered
        assertTrue(get(c, "/api/campus-services").body().contains("Library"));
        assertEquals(404, get(c, "/api/unknown").statusCode());
    }
}
