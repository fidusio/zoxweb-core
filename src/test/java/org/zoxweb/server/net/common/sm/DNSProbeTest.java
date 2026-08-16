package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVGenericMap;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The DNS probe recipe (META-SM-PROTO-DESIGN.md), <b>machine driven end to end</b>: the config dictates the
 * whole session — on {@code connected(SK)} the machine sends the canned DNS query (txn id 0x1234,
 * A example.com), NIOSocket triggers feed it the reply, the {@code expect} validates the txn-id
 * echo, and {@code close_on_ready} (the UDP-probe default) makes the machine close the session
 * itself. The test is a pure observer: it hands the callback to NIOSocket, awaits completion via
 * {@code SMProtoUtil.waitForClose} (the machine's native {@code MACHINE_CLOSED} signal), and reads
 * the outcome from the machine's properties — the {@code SMProtoUtil.results} bag and
 * {@code SMProtoUtil.closeCause}. Verified live against 8.8.8.8:53 on 2026-08-14; this test
 * pins the same flow hermetically.
 */
public class DNSProbeTest {

    private static final int WAIT_SEC = 5;

    /** The recipe config, verbatim from META-SM-PROTO-DESIGN.md (port hint irrelevant on loopback). */
    static final String DNS_PROBE_CONFIG =
            "{\"name\": \"dns-probe\", \"transport\": \"udp\", \"port\": 53, \"timeout_sec\": 3," +
            " \"exchange\": [" +
            "   {\"send\":   \"hex:1234 0100 0001 0000 0000 0000 07 6578616d706c65 03 636f6d 00 0001 0001\"}," +
            "   {\"expect\": \"hex:1234\"}" +
            " ]}";

    /**
     * Standalone probe runner against a real DNS endpoint:
     * <pre>
     *   DNSProbeTest &lt;ip|host&gt; [port]     (port defaults to the recipe's 53)
     * </pre>
     * Observer only — the machine dials, sends, validates and closes itself. Exit code: 0 the
     * endpoint answered the probe (clean close), 1 closed with a cause, 2 no answer within the
     * wait (lost datagram / nothing listening), 64 usage error.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DNSProbeTest <ip|host> [port]");
            System.exit(64);
            return;
        }
        ClientConSM sm = ClientSMFactory.fromJSON(DNS_PROBE_CONFIG);
        int port = args.length > 1 ? Integer.parseInt(args[1])
                : ClientSMFactory.port(sm.getContext().getSettings(), 53);
        InetSocketAddress remote = new InetSocketAddress(args[0], port);

        State<Object> observer = new State<Object>("observer");
        observer.register((Consumer<Object>) o -> System.out.println("CONNECTED " + remote),
                ClientEvent.CONNECTED);
        observer.register((Consumer<Object>) o -> System.out.println("READY"), ClientEvent.READY);
        observer.register((Consumer<Throwable>) t ->
                System.out.println("CLOSED" + (t != null ? " cause: " + t : " (clean)")),
                ClientEvent.CLOSED);
        sm.register(observer);

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPSMCallback cb = sm.newSessionCallback(remote);
        int exit;
        try {
            nioSocket.addDatagramSocket(new InetSocketAddress(0), cb);
            if (SMProtoUtil.waitForClose(sm, TimeUnit.SECONDS.toMillis(WAIT_SEC + WAIT_SEC)))
                exit = SMProtoUtil.closeCause(sm) != null ? 1 : 0;
            else
                exit = 2;
        } finally {
            SharedIOUtil.close(cb, nioSocket);
            TaskUtil.close();
        }
        System.out.println("results: " + SMProtoUtil.results(sm));
        System.out.println("dns-probe " + remote + " -> " + (exit == 0 ? "OK" : "FAILED (exit " + exit + ")"));
        System.exit(exit);
    }

    @Test
    public void dnsProbeMachineDrivesFullLifecycle() throws Exception {
        final AtomicReference<byte[]> query = new AtomicReference<byte[]>();
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        // DNS-shaped responder: validate the canned query, answer with a minimal response
        // that echoes the transaction id (what a real resolver does — verified against 8.8.8.8)
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
                // header: same txn id, QR=1 (response), rest zeroed — enough for the txn-id expect
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

        ClientConSM sm = ClientSMFactory.fromJSON(DNS_PROBE_CONFIG);
        assertEquals(ClientSessionContext.Transport.UDP, sm.getContext().getTransport());
        assertEquals(53, ClientSMFactory.port(sm.getContext().getSettings(), -1), "recipe's default-port hint");

        InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPSMCallback cb = sm.newSessionCallback(remote);
        try {
            nioSocket.addDatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), cb);

            // the machine closes the session itself (close_on_ready, UDP-probe default);
            // completion awaited on the machine's native MACHINE_CLOSED signal — no observer state
            assertTrue(SMProtoUtil.waitForClose(sm, TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "machine did not complete and close the session");
            assertTrue(cb.isClosed(), "session must be closed by the machine, not the test");
            assertTrue(sm.isClosed(), "machine must be closed by teardown");

            // outcome read from the machine's properties, as accumulated by the states
            NVGenericMap results = SMProtoUtil.results(sm);
            assertEquals(Boolean.TRUE, results.getValue("ready"), "results bag must record the completed pipeline");
            assertNull(SMProtoUtil.closeCause(sm), "clean close expected, cause: " + SMProtoUtil.closeCause(sm));

            byte[] q = query.get();
            assertNotNull(q, "responder never received the probe");
            assertEquals(0x12, q[0] & 0xFF, "txn id hi byte");
            assertEquals(0x34, q[1] & 0xFF, "txn id lo byte");
            assertEquals(29, q.length, "canned A example.com query is 29 bytes");
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());
        } finally {
            SharedIOUtil.close(cb, nioSocket);
            server.close();
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }
}
