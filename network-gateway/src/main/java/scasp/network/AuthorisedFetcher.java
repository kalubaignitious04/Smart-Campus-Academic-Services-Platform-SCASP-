package scasp.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/** Fetches a small text resource, but only from an allow-list of host:port pairs, with timeouts. */
public class AuthorisedFetcher {
    public static class FetchException extends Exception {
        public FetchException(String m, Throwable c) { super(m, c); }
    }
    private final Set<String> allowed;
    private final HttpClient client;
    private final Duration timeout;

    public AuthorisedFetcher(Set<String> allowedHostPorts, int timeoutMillis) {
        this.allowed = allowedHostPorts;
        this.timeout = Duration.ofMillis(timeoutMillis);
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public String fetch(URI uri) throws FetchException {
        String hostPort = uri.getHost() + ":" + uri.getPort();
        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()) || !allowed.contains(hostPort))
            throw new FetchException("Resource is not authorised: " + hostPort, null);
        try {
            HttpResponse<String> r = client.send(HttpRequest.newBuilder(uri).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) throw new FetchException("HTTP " + r.statusCode(), null);
            String body = r.body();
            return body.length() > 2000 ? body.substring(0, 2000) : body;
        } catch (IOException e) {
            throw new FetchException("Resource unavailable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("Interrupted", e);
        }
    }
}
