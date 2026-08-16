package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The UDP transport path of the JSON-defined machine: a factory-built {@code transport: udp}
 * config runs its exchange dialogue over a real loopback datagram exchange (send PING, expect
 * PONG), fires READY, and tears down cleanly. Also pins the UDP fail-fast matrix (factory and
 * builder) and the transport/callback type guards.
 */
public class UDPClientLoopbackTest {

    private static final int WAIT_SEC = 5;

    @Test
    public void udpExchangeDialogueFiresReady() throws Exception {
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final CountDownLatch closedLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> closeCause = new AtomicReference<Throwable>(new Exception("sentinel"));
        final AtomicReference<String> received = new AtomicReference<String>();
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        // scripted UDP peer: expect "PING", answer "PONG"
        DatagramSocket server = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        server.setSoTimeout((int) TimeUnit.SECONDS.toMillis(WAIT_SEC));
        Thread serverThread = new Thread(() -> {
            try {
                byte[] buf = new byte[64];
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                server.receive(in);
                received.set(new String(in.getData(), 0, in.getLength(), StandardCharsets.US_ASCII));
                byte[] reply = "PONG".getBytes(StandardCharsets.US_ASCII);
                server.send(new DatagramPacket(reply, reply.length, in.getAddress(), in.getPort()));
            } catch (Exception e) {
                serverFailure.set(e);
            }
        }, "udp-script-server");
        serverThread.start();

        ClientConSM sm = ClientSMFactory.fromJSON(
                "{\"name\": \"udp-ping\", \"transport\": \"udp\", \"protocol\": \"plain\"," +
                " \"exchange\": [ {\"send\": \"txt:PING\"}, {\"expect\": \"txt:PONG\"} ]}");
        assertEquals(ClientSessionContext.Transport.UDP, sm.getContext().getTransport());

        State<Object> app = new State<Object>("app");
        app.register((Consumer<Object>) o -> readyLatch.countDown(), ClientEvent.READY);
        app.register((Consumer<Throwable>) t -> {
            closeCause.set(t);
            closedLatch.countDown();
        }, SMProtoUtil.BasicEvent.CLOSED);
        sm.register(app);

        InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPSMCallback cb = sm.newSessionCallback(remote);
        try {
            nioSocket.addDatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), cb);

            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "exchange did not complete (no READY)");
            assertEquals("PING", received.get(), "peer must receive the scripted send");
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());

            cb.close();
            assertTrue(closedLatch.await(WAIT_SEC, TimeUnit.SECONDS), "CLOSED not published on close");
            assertNull(closeCause.get(), "clean close must deliver CLOSED with null payload");
            assertTrue(sm.isClosed(), "machine must be closed by teardown");
        } finally {
            SharedIOUtil.close(cb, nioSocket);
            server.close();
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    @Test
    public void udpPostReadyDataFlowsAsInData() throws Exception {
        // no exchange: READY at CONNECTED, machine sends the first datagram from the app's
        // CONNECTED consumer, the reply routes DATAGRAM -> IN_DATA through the UDP transport state
        final CountDownLatch dataLatch = new CountDownLatch(1);
        final AtomicReference<String> echoed = new AtomicReference<String>();
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        DatagramSocket server = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        server.setSoTimeout((int) TimeUnit.SECONDS.toMillis(WAIT_SEC));
        Thread serverThread = new Thread(() -> {
            try {
                byte[] buf = new byte[64];
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                server.receive(in);
                server.send(new DatagramPacket(in.getData(), in.getLength(), in.getAddress(), in.getPort()));
            } catch (Exception e) {
                serverFailure.set(e);
            }
        }, "udp-echo-server");
        serverThread.start();

        ClientConSM sm = ClientSMFactory.fromJSON("{\"name\": \"udp-echo\", \"transport\": \"udp\"}");
        State<Object> app = new State<Object>("app");
        app.register((Consumer<Object>) o -> {
            try {
                sm.getContext().write(ByteBuffer.wrap("udp-hello".getBytes(StandardCharsets.US_ASCII)));
            } catch (Exception e) {
                sm.getContext().fail(e);
            }
        }, SMProtoUtil.BasicEvent.CONNECTED);
        app.register((Consumer<ByteBuffer>) bb -> {
            byte[] chunk = new byte[bb.remaining()];
            bb.get(chunk);
            ByteBufferUtil.cache(bb);
            echoed.set(new String(chunk, StandardCharsets.US_ASCII));
            dataLatch.countDown();
        }, ClientEvent.IN_DATA);
        sm.register(app);

        InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPSMCallback cb = sm.newSessionCallback(remote);
        try {
            nioSocket.addDatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), cb);
            assertTrue(dataLatch.await(WAIT_SEC, TimeUnit.SECONDS), "echo not routed as IN_DATA");
            assertEquals("udp-hello", echoed.get());
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());
        } finally {
            SharedIOUtil.close(cb, nioSocket);
            server.close();
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    @Test
    public void factoryRejectsStreamOnlyFeaturesOverUDP() {
        // TLS and SSH require a stream
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{\"transport\": \"udp\", \"protocol\": \"tls\"}"));
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{\"transport\": \"udp\", \"protocol\": \"ssh\"}"));
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{\"transport\": \"udp\", \"tls\": {\"mode\": \"on_demand\"}}"));
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{\"transport\": \"bogus\"}"));
    }

    @Test
    public void builderRejectsStreamOnlyPhasesOverUDP() {
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("udp-tls")
                .transport(ClientSessionContext.Transport.UDP)
                .phase(new SSLClientPhase(SSLClientPhase.TLSMode.IMMEDIATE, true))
                .build());
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("udp-ssh")
                .transport(ClientSessionContext.Transport.UDP)
                .phase(new SSHBannerPhase())
                .build());
    }

    @Test
    public void transportGuardsCallbackType() {
        InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), 9);
        ClientConSM udpSM = ClientConSMBuilder.create("udp-machine")
                .transport(ClientSessionContext.Transport.UDP).build();
        assertNotNull(udpSM.lookupState(UDPClientTransportState.NAME), "UDP transport state must be registered");
        assertThrows(IllegalStateException.class, udpSM::newSessionCallback,
                "TCP callback on a UDP machine must be rejected");

        ClientConSM tcpSM = ClientConSMBuilder.create("tcp-machine").build();
        assertThrows(IllegalStateException.class, () -> tcpSM.newSessionCallback(remote),
                "UDP callback on a TCP machine must be rejected");
    }
}
