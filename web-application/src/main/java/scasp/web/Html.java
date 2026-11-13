package scasp.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** EL functions used by the JSP pages (declared in WEB-INF/scasp.tld). */
public final class Html {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm").withZone(ZoneId.of("Africa/Lusaka"));
    private Html() {}
    /** Escapes untrusted text for safe output in HTML. */
    public static String h(Object o) {
        if (o == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : o.toString().toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
    public static String time(Object instant) { return instant instanceof Instant i ? FMT.format(i) : ""; }
}
