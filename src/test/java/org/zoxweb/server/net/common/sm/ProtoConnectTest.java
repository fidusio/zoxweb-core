package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit coverage for the ProtoConnect CLI's endpoint parsing and byte rendering — the non-trivial,
 * network-free parts. The connect/event flow itself is the same pattern the loopback tests exercise.
 */
public class ProtoConnectTest {

    private static ClientConSM smWithPortHint(int port) {
        return ClientSMFactory.fromJSON("{ \"name\": \"x\", \"protocol\": \"plain\", \"port\": " + port + " }");
    }

    private static ClientConSM smNoHint() {
        return ClientSMFactory.fromJSON("{ \"name\": \"x\", \"protocol\": \"plain\" }");
    }

    @Test
    public void hostAndPortFromArg() {
        InetSocketAddress a = ProtoConnect.resolveRemote("mail.example.com:25", smNoHint());
        assertEquals("mail.example.com", a.getHostString());
        assertEquals(25, a.getPort());
    }

    @Test
    public void portFromArgWinsOverConfigHint() {
        InetSocketAddress a = ProtoConnect.resolveRemote("host:25", smWithPortHint(587));
        assertEquals(25, a.getPort(), "explicit port overrides the config default-port hint");
    }

    @Test
    public void portFallsBackToConfigHintWhenOmitted() {
        InetSocketAddress a = ProtoConnect.resolveRemote("host", smWithPortHint(587));
        assertEquals("host", a.getHostString());
        assertEquals(587, a.getPort(), "port omitted -> config port hint used");
    }

    @Test
    public void missingPortWithNoHintIsFatal() {
        assertThrows(IllegalArgumentException.class, () -> ProtoConnect.resolveRemote("host", smNoHint()));
    }

    @Test
    public void bracketedIPv6() {
        InetSocketAddress a = ProtoConnect.resolveRemote("[::1]:443", smNoHint());
        assertEquals(443, a.getPort());
        assertTrue(a.getHostString().contains(":"), "IPv6 host preserved: " + a.getHostString());
    }

    @Test
    public void bracketedIPv6PortFallsBackToHint() {
        InetSocketAddress a = ProtoConnect.resolveRemote("[::1]", smWithPortHint(22));
        assertEquals(22, a.getPort());
    }

    @Test
    public void printableKeepsAsciiDotsTheRest() {
        assertEquals("EHLO\r\n", ProtoConnect.printable("EHLO\r\n".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("..A.", ProtoConnect.printable(new byte[]{0x00, (byte) 0xFF, 'A', 0x1B}));
    }

    /**
     * End-to-end run() against a loopback peer: a plain scripted dialogue (greet, EHLO, 250) reaches
     * READY and the peer closes, so run() returns 0 (clean close). Exercises the full connect →
     * events → close path without main's System.exit / TaskUtil teardown.
     */
    @Test
    public void runDrivesPlainDialogueToCleanClose() throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Thread serverThread = null;
        try {
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    OutputStream out = accepted.getOutputStream();
                    out.write(SharedStringUtil.getBytes("220 mx.test ESMTP\r\n"));
                    out.flush();
                    accepted.getInputStream().read(new byte[256]); // consume EHLO
                    out.write(SharedStringUtil.getBytes("250 ok\r\n"));
                    out.flush();
                    // brief hold, then close so the client observes CLOSED
                    Thread.sleep(200);
                } catch (Exception ignore) {
                }
            }, "protoconnect-test-server");
            serverThread.start();

            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());
            String json = "{ \"name\": \"pc-plain\", \"protocol\": \"plain\","
                    + " \"exchange\": [ {\"expect\": \"txt:220 \"}, {\"send\": \"txt:EHLO ${helo}\\r\\n\"}, {\"expect\": \"txt:250 \"} ] }";
            ClientConSM sm = ClientSMFactory.fromJSON(json);
            sm.getContext().setVar("helo", "probe.test");

            int exit = ProtoConnect.run(remote, sm, nioSocket, 5, 10);
            assertEquals(0, exit, "clean close expected");
            assertTrue(sm.isClosed(), "machine closed by teardown");
        } finally {
            SharedIOUtil.close(server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
