package scasp.network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import scasp.core.Model.Notification;

/**
 * Sends each notification as one small UDP datagram: "SCASP|TYPE|message".
 * UDP suits this: a lost notification is harmless because the data is also on the dashboard.
 */
public class UdpNotifier implements Consumer<Notification>, AutoCloseable {
    private final DatagramSocket socket;
    private final InetSocketAddress target;

    public UdpNotifier(String host, int port) throws SocketException {
        this.socket = new DatagramSocket();
        this.target = new InetSocketAddress(host, port);
    }

    @Override public void accept(Notification n) {
        byte[] data = ("SCASP|" + n.type() + "|" + n.message()).getBytes(StandardCharsets.UTF_8);
        try { socket.send(new DatagramPacket(data, Math.min(data.length, 512), target)); }
        catch (IOException e) { System.err.println("UDP notification failed: " + e.getMessage()); }
    }

    @Override public void close() { socket.close(); }

    /** Waits for one datagram (used by the demo listener and tests). Returns null on timeout. */
    public static String receiveOne(int port, int timeoutMillis) throws IOException {
        try (DatagramSocket s = new DatagramSocket(port)) { return receiveOne(s, timeoutMillis); }
    }
    public static String receiveOne(DatagramSocket s, int timeoutMillis) throws IOException {
        s.setSoTimeout(timeoutMillis);
        byte[] buf = new byte[512];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        try { s.receive(p); } catch (SocketTimeoutException e) { return null; }
        return new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
    }

    /** Demo listener: java -cp ... scasp.network.UdpNotifier [port] */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9091;
        try (DatagramSocket s = new DatagramSocket(port)) {
            System.out.println("Listening for SCASP notifications on UDP " + port);
            while (true) { String m = receiveOne(s, 0); System.out.println(m); }
        }
    }
}
