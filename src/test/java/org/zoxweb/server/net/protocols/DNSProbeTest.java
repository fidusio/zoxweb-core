package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The DNS probe driven end to end by the pure-JSON definition
 * {@code protocols/dns-probe.json}: the validator dials, sends the canned query (txn id 0x1234,
 * A example.com), validates the txn-id echo, and closes itself ({@code close_on_ready}, the UDP
 * default). The test pins the flow hermetically against a DNS-shaped responder; {@link #main}
 * runs the same definition live against a real resolver.
 */
public class DNSProbeTest {

    private static final int WAIT_SEC = 5;
    private static final String DEFINITION = "src/test/resources/protocols/dns-probe.json";

    private static String definitionJSON() throws IOException {
        // resolve from the working directory (project root) or the classpath-relative test path
        Path p = Paths.get(DEFINITION);
        if (!Files.exists(p))
            p = Paths.get("..", DEFINITION);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    /**
     * Standalone probe runner against a real DNS endpoint:
     * <pre>
     *   DNSProbeTest &lt;ip|host&gt; [port]     (port defaults to the definition's hint, 53)
     * </pre>
     * Exit code: 0 the endpoint answered the probe (clean close, validated), 1 closed with a
     * cause, 2 no answer within the wait (lost datagram / nothing listening), 64 usage error.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DNSProbeTest <ip|host> [port]");
            System.exit(64);
            return;
        }
        // the endpoint string; a missing port falls back to the definition's port hint
        String endpoint = args.length > 1 ? args[0] + ":" + args[1] : args[0];
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPMetaProtocol probe = ProtoConnect.createUDPProtocol(endpoint, definitionJSON());
        int exit;
        try {
            System.out.println("CONNECTING " + endpoint);
            nioSocket.addDatagramSocket(new InetSocketAddress(0), probe);
            if (probe.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC * 2L)))
                exit = probe.getCloseCause() != null ? 1 : 0;
            else
                exit = 2;
            System.out.println("results: " + probe.getResults());
            if (probe.getCloseCause() != null)
                System.out.println("cause: " + probe.getCloseCause());
        } finally {
            SharedIOUtil.close(probe, nioSocket);
            TaskUtil.close();
        }
        System.out.println("dns-probe " + endpoint + " -> " + (exit == 0 ? "OK" : "FAILED (exit " + exit + ")"));
        System.exit(exit);
    }

    @Test
    public void dnsProbeDefinitionDrivesFullLifecycle() throws Exception {
        final AtomicReference<byte[]> query = new AtomicReference<byte[]>();
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        // DNS-shaped responder: capture the query, answer with a minimal response echoing the
        // transaction id (what a real resolver does — the live main() proves the same flow)
        DatagramSocket server = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        server.setSoTimeout((int) TimeUnit.SECONDS.toMillis(WAIT_SEC));
        Thread serverThread = new Thread(() -> {
            try {
                byte[] buf = new byte[512];
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                server.receive(in);
                byte[] q = new byte[in.getLength()];
                System.arraycopy(in.getData(), 0, q, 0, q.length);
                query.set(q);
                // header: same txn id, QR=1 (response), rest zeroed — enough for the expect
                byte[] response = new byte[12];
                response[0] = q[0];          // txn id hi = 0x12
                response[1] = q[1];          // txn id lo = 0x34
                response[2] = (byte) 0x80;   // QR bit
                server.send(new DatagramPacket(response, response.length, in.getAddress(), in.getPort()));
            } catch (Exception e) {
                serverFailure.set(e);
            }
        }, "fake-dns-server");
        serverThread.start();

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPMetaProtocol validator = null;
        try {
            validator = ProtoConnect.createUDPProtocol(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                    definitionJSON());
            assertTrue(validator.getScript().isUDP());
            assertEquals(53, validator.getScript().getPort(), "definition's default-port hint");

            nioSocket.addDatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), validator);

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "validator did not complete and close the session");
            assertTrue(validator.isClosed(), "session must be closed by the validator, not the test");
            assertNull(validator.getCloseCause(), "clean close expected, cause: " + validator.getCloseCause());
            assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
            assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
            assertNotNull(validator.getResults().getValue("dns"), "report key recorded");

            byte[] q = query.get();
            assertNotNull(q, "responder never received the probe");
            assertEquals(0x12, q[0] & 0xFF, "txn id hi byte");
            assertEquals(0x34, q[1] & 0xFF, "txn id lo byte");
            assertEquals(29, q.length, "canned A example.com query is 29 bytes");
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());
        } finally {
            SharedIOUtil.close(validator, nioSocket);
            server.close();
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }
}
