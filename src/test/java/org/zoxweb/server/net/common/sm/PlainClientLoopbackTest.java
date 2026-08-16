package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain-mode smoke test over a real loopback socket (same policy as TCPSMCallbackTest):
 * a phase-less machine must publish READY right after CONNECTED, pass wire bytes through as
 * IN_DATA, and deliver CLOSED(null) on peer EOF with the machine closed by teardown.
 */
public class PlainClientLoopbackTest {

    private static final int WAIT_SEC = 10;

    @Test
    public void plainSessionLifecycle() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>(new Exception("sentinel"));

        ClientConSM sm = ClientConSMBuilder.create("plain-loopback").build();
        State<Object> app = new State<Object>("app");
        app.register((Consumer<Object>) payload -> {
            order.add("READY");
            readyLatch.countDown();
        }, ClientEvent.READY);
        app.register((Consumer<ByteBuffer>) bb -> {
            order.add("IN_DATA");
            byte[] chunk = new byte[bb.remaining()];
            bb.get(chunk);
            received.write(chunk, 0, chunk.length);
            ByteBufferUtil.cache(bb);
            dataLatch.countDown();
        }, ClientEvent.IN_DATA);
        app.register((Consumer<Throwable>) t -> {
            order.add("CLOSED");
            closedPayload.set(t);
        }, ClientEvent.CLOSED);
        sm.register(app);

        TCPSMCallback callback = sm.newSessionCallback();

        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                    callback, 5, null);
            accepted = server.accept();

            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not published for plain session");

            accepted.getOutputStream().write(SharedStringUtil.getBytes("plain-hello"));
            accepted.getOutputStream().flush();
            assertTrue(dataLatch.await(WAIT_SEC, TimeUnit.SECONDS), "IN_DATA not published");

            accepted.close();
            // completion via the machine's native signal — teardown closes the machine last,
            // so the CLOSED consumer above has already run when this returns
            assertTrue(SMProtoUtil.waitForClose(sm, TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "session did not close on peer EOF");

            assertEquals("plain-hello", new String(received.toByteArray()));
            assertNull(closedPayload.get(), "clean EOF must deliver CLOSED with null payload");
            assertTrue(callback.isClosed());
            assertTrue(sm.isClosed(), "machine must be closed by session teardown");
            synchronized (order) {
                assertEquals("READY", order.get(0), "READY must precede data");
                assertEquals("IN_DATA", order.get(1));
                assertEquals("CLOSED", order.get(order.size() - 1));
            }
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }
}
