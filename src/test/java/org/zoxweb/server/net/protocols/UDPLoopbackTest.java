package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The UDP validator over loopback datagrams: a definition-driven PING/PONG exchange — first
 * datagram out on {@code connected}, reply validated, {@code close_on_ready} (the UDP default)
 * self-close — plus the transport guard at construction.
 */
public class UDPLoopbackTest {

    private static final int WAIT_SEC = 10;

    @Test
    public void tcpDefinitionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProtoConnect.createUDPValidator(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 9), "{\"transport\": \"tcp\"}"));
    }

    @Test
    public void pingPongExchangeSelfCloses() throws Exception {
        final AtomicReference<String> request = new AtomicReference<String>();
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        DatagramSocket server = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        server.setSoTimeout((int) TimeUnit.SECONDS.toMillis(WAIT_SEC));
        Thread serverThread = new Thread(() -> {
            try {
                byte[] buf = new byte[512];
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                server.receive(in);
                request.set(new String(in.getData(), 0, in.getLength()));
                byte[] reply = SharedStringUtil.getBytes("PONG");
                server.send(new DatagramPacket(reply, reply.length, in.getAddress(), in.getPort()));
            } catch (Exception e) {
                serverFailure.set(e);
            }
        }, "udp-pong-server");
        serverThread.start();

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPMetaProtocol validator = null;
        try {
            validator = ProtoConnect.createUDPValidator(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                    "{ \"transport\": \"udp\","
                            + " \"exchange\": ["
                            + "  {\"send\": \"txt:PING\"},"
                            + "  {\"expect\": \"txt:PONG\"},"
                            + "  {\"validate\": {\"exact\": \"txt:PONG\", \"report\": \"pong\"}}"
                            + " ] }");

            nioSocket.addDatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), validator);

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "close_on_ready (UDP default) must close the session after the script completes");
            assertTrue(validator.isClosed());
            assertNull(validator.getCloseCause(), "clean completion");
            assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
            assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
            assertEquals("PONG", validator.getResults().getValue("pong"));
            assertEquals("PING", request.get(), "first datagram out on connected");
            assertNull(serverFailure.get(), "server side clean: " + serverFailure.get());
        } finally {
            SharedIOUtil.close(validator, nioSocket);
            server.close();
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }
}
