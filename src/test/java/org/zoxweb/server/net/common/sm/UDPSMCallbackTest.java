package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.StateMachine;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UDPSMCallback as a client over a real loopback datagram exchange: the machine sends the first
 * datagram on CONNECTED, the plain echo server replies, the reply arrives as DATAGRAM, and teardown
 * publishes CLOSED once. Also pins the one-machine-per-callback rule.
 */
public class UDPSMCallbackTest {

    private static final int WAIT_SEC = 5;

    @Test
    public void clientSendsOnConnectedAndReceivesEcho() throws Exception {
        final CountDownLatch dataLatch = new CountDownLatch(1);
        final AtomicReference<String> echoed = new AtomicReference<String>();
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>(new Exception("sentinel"));
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        // plain UDP echo server on an ephemeral loopback port
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

        InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());
        StateMachine<Object> machine = new StateMachine<Object>("udp-client", (Executor) null);
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        UDPSMCallback cb = new UDPSMCallback(remote, machine);
        try {
            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) o -> {
                // client kickoff: send the first datagram to the remote
                try {
                    cb.send(ByteBuffer.wrap(SharedStringUtil.getBytes("udp-ping")), false);
                } catch (IOException e) {
                    cb.exception(e);
                }
            }, CommonTrigger.CONNECTED);
            app.register((Consumer<Object>) o -> {
                DataPacket<?> dp = (DataPacket<?>) o;
                ByteBuffer bb = dp.getIOBuffers().getInBuffer();
                byte[] chunk = new byte[bb.remaining()];
                bb.get(chunk);
                // the packet buffer is a detached consumer-owned copy — recache when done
                ByteBufferUtil.cache(bb);
                echoed.set(new String(chunk));
                dataLatch.countDown();
            }, CommonTrigger.DATAGRAM);
            app.register((Consumer<Throwable>) t -> closedPayload.set(t), CommonTrigger.CLOSED);
            machine.register(app);

            // ephemeral local bind; NIOSocket invokes connected(SK) after registration — the
            // machine kickoff that publishes CONNECTED and sends the first datagram
            nioSocket.addDatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), cb);

            assertTrue(dataLatch.await(WAIT_SEC, TimeUnit.SECONDS), "echo datagram not received as DATAGRAM");
            assertEquals("udp-ping", echoed.get(), "client must receive the echoed payload");
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());

            cb.close();
            // teardown closes the machine last — CLOSED was delivered when this returns
            assertTrue(SMProtoUtil.waitForClose(machine, TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "machine not closed by teardown");
            assertNull(closedPayload.get(), "clean close must deliver CLOSED with null payload");
            assertTrue(cb.isClosed());
            assertTrue(machine.isClosed(), "machine must be closed by teardown");
        } finally {
            SharedIOUtil.close(cb, nioSocket);
            server.close();
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    @Test
    public void oneMachinePerCallback() {
        StateMachine<Object> machine = new StateMachine<Object>("udp-solo", (Executor) null);
        InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), 9);
        new UDPSMCallback(remote, machine);
        assertThrows(IllegalArgumentException.class, () -> new UDPSMCallback(remote, machine),
                "a state machine may back only one UDP callback");
    }
}
