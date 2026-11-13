package scasp.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Small TCP client. Interactive use: java -cp ... scasp.network.GatewayClient [host] [port] */
public class GatewayClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public GatewayClient(String host, int port, int timeoutMillis) throws IOException {
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), timeoutMillis);
        socket.setSoTimeout(timeoutMillis);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** Sends one command line and returns the one-line reply (null if the server closed the connection). */
    public String send(String command) throws IOException {
        out.println(command);
        return in.readLine();
    }

    @Override public void close() throws IOException { socket.close(); }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        try (GatewayClient c = new GatewayClient(host, port, 10_000);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Connected. Commands: PING INFO STATUS COURSES COURSE <c> LOGIN <id> <pw> REGISTER <c> WITHDRAW <c> MYREG QUIT");
            String line;
            while ((line = console.readLine()) != null) {
                String reply = c.send(line);
                System.out.println(reply);
                if (reply == null || reply.equals("OK|BYE")) break;
            }
        } catch (IOException e) {
            System.err.println("Could not talk to the gateway: " + e.getMessage());
        }
    }
}
