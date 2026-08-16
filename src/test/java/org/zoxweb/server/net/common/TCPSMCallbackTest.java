package org.zoxweb.server.net.common;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.StateMachine;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.sm.SMProtoUtil;
import org.zoxweb.server.net.common.sm.TCPSMCallback;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the TCPSMCallback session contract:
 * <ul>
 *   <li>one state machine per callback — a second binding is rejected</li>
 *   <li>teardown order — CLOSED is delivered to trigger consumers BEFORE the machine is closed,
 *   and the machine is closed by teardown (the machine is session-owned)</li>
 *   <li>CLOSED payload — the Throwable from the exception() path, null on clean close/EOF</li>
 *   <li>CLOSED exactly once, subsequent closes are no-ops</li>
 *   <li>full loopback lifecycle — CONNECTED (SelectionKey) → RAW_IN_DATA (detached ByteBuffer
 *   copy) → CLOSED (null) on peer EOF, in order</li>
 * </ul>
 * The loopback test opens a real 127.0.0.1 socket (same policy as the other server/net tests).
 */
public class TCPSMCallbackTest {

    private static final int WAIT_SEC = 10;

    @Test
    public void oneMachinePerCallback() {
        StateMachine<Void> sm = new StateMachine<Void>("one-to-one-fsm", (Executor) null);
        new TCPSMCallback(sm);
        assertThrows(IllegalArgumentException.class, () -> new TCPSMCallback(sm),
                "a state machine already bound to a session callback must be rejected");
    }

    @Test
    public void exceptionPathPublishesClosedWithThrowableThenClosesMachine() throws Exception {
        StateMachine<Void> sm = new StateMachine<Void>("error-fsm", (Executor) null);
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>();
        final AtomicInteger closedCount = new AtomicInteger();
        State<Object> session = new State<Object>("session");
        session.register((Consumer<Throwable>) t -> {
            closedPayload.set(t);
            closedCount.incrementAndGet();
        }, SMProtoUtil.BasicEvent.CLOSED);
        sm.register(session);

        TCPSMCallback callback = new TCPSMCallback(sm);
        IOException boom = new IOException("boom");
        callback.exception(boom);

        assertTrue(callback.isClosed());
        assertEquals(1, closedCount.get(), "CLOSED must be delivered exactly once");
        assertSame(boom, closedPayload.get(), "CLOSED payload must be the terminating Throwable");
        assertTrue(sm.isClosed(), "teardown must close the machine after CLOSED is delivered");

        // re-close is a no-op, CLOSED stays exactly once
        callback.close();
        assertEquals(1, closedCount.get());
    }

    @Test
    public void cleanClosePublishesClosedWithNullPayload() throws Exception {
        StateMachine<Void> sm = new StateMachine<Void>("clean-close-fsm", (Executor) null);
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>(new Exception("sentinel"));
        final AtomicInteger closedCount = new AtomicInteger();
        State<Object> session = new State<Object>("session");
        session.register((Consumer<Throwable>) t -> {
            closedPayload.set(t);
            closedCount.incrementAndGet();
        }, SMProtoUtil.BasicEvent.CLOSED);
        sm.register(session);

        TCPSMCallback callback = new TCPSMCallback(sm);
        callback.close();

        assertEquals(1, closedCount.get(), "CLOSED must be delivered on a clean close");
        assertNull(closedPayload.get(), "clean close must publish CLOSED with a null payload");
        assertTrue(callback.isClosed());
        assertTrue(sm.isClosed(), "teardown must close the machine");
    }

    @Test
    public void loopbackSessionLifecycle() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch connectedLatch = new CountDownLatch(1);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        final CountDownLatch closedLatch = new CountDownLatch(1);
        final AtomicReference<Object> connectedPayload = new AtomicReference<Object>();
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>(new Exception("sentinel"));

        StateMachine<Void> sm = new StateMachine<Void>("loopback-fsm", (Executor) null);
        State<Object> session = new State<Object>("session");
        session.register((Consumer<Object>) payload -> {
            order.add("CONNECTED");
            connectedPayload.set(payload);
            connectedLatch.countDown();
        }, SMProtoUtil.BasicEvent.CONNECTED);
        session.register((Consumer<ByteBuffer>) bb -> {
            order.add("RAW_IN_DATA");
            byte[] chunk = new byte[bb.remaining()];
            bb.get(chunk);
            received.write(chunk, 0, chunk.length);
            // the buffer is a consumer-owned detached copy, recache it when done
            ByteBufferUtil.cache(bb);
            dataLatch.countDown();
        }, SMProtoUtil.BasicEvent.IN_RAW_DATA);
        session.register((Consumer<Throwable>) t -> {
            order.add("CLOSED");
            closedPayload.set(t);
            closedLatch.countDown();
        }, SMProtoUtil.BasicEvent.CLOSED);
        sm.register(session);

        TCPSMCallback callback = new TCPSMCallback(sm);

        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                    callback, 5, null);
            accepted = server.accept();

            assertTrue(connectedLatch.await(WAIT_SEC, TimeUnit.SECONDS), "CONNECTED not published");
            assertTrue(connectedPayload.get() instanceof SelectionKey, "CONNECTED payload must be the SelectionKey");

            accepted.getOutputStream().write(SharedStringUtil.getBytes("hello"));
            accepted.getOutputStream().flush();
            assertTrue(dataLatch.await(WAIT_SEC, TimeUnit.SECONDS), "RAW_IN_DATA not published");

            // peer EOF must tear the session down and deliver CLOSED
            accepted.close();
            assertTrue(closedLatch.await(WAIT_SEC, TimeUnit.SECONDS), "CLOSED not published on clean EOF");

            assertEquals("hello", new String(received.toByteArray()));
            assertNull(closedPayload.get(), "clean EOF must publish CLOSED with a null payload");
            assertTrue(callback.isClosed());
            assertTrue(sm.isClosed(), "machine must be closed by session teardown");
            synchronized (order) {
                assertEquals("CONNECTED", order.get(0), "CONNECTED must be the first event");
                assertEquals("RAW_IN_DATA", order.get(1), "RAW_IN_DATA must follow CONNECTED");
                assertEquals("CLOSED", order.get(order.size() - 1), "CLOSED must be the last event");
            }
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }
}
