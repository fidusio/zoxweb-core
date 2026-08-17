package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSH banner flow over a real loopback socket: the peer writes an identification line, the
 * machine validates it and reports READY, and peer EOF tears the session down with CLOSED(null).
 */
public class SSHBannerLoopbackTest {

    private static final int WAIT_SEC = 10;

    @Test
    public void bannerLifecycle() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>(new Exception("sentinel"));

        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"ssh-loopback\", \"protocol\": \"ssh\", \"ssh\": {\"banner_contains\": \"OpenSSH\"} }");
        State<Object> app = new State<Object>("app");
        app.register((Consumer<Object>) o -> {
            order.add("READY");
            readyLatch.countDown();
        }, CommonTrigger.READY);
        app.register((Consumer<Throwable>) t -> {
            order.add("CLOSED");
            closedPayload.set(t);
        }, CommonTrigger.CLOSED);
        sm.register(app);

        TCPSMCallback callback = sm.newSessionCallback();

        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                    callback, 5, null);
            accepted = server.accept();

            accepted.getOutputStream().write(SharedStringUtil.getBytes("SSH-2.0-OpenSSH_9.6\r\n"));
            accepted.getOutputStream().flush();
            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not published after banner");
            // BANNER_RECEIVED is retired — the validated banner is read from the report
            assertEquals("SSH-2.0-OpenSSH_9.6", SMProtoUtil.results(sm).getValue("banner"));

            accepted.close();
            // completion via the machine's native signal — CLOSED was delivered before the
            // machine closed (teardown order), so order/payload above are final here
            assertTrue(SMProtoUtil.waitForClose(sm, TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "session did not close on peer EOF");

            assertNull(closedPayload.get(), "clean EOF must deliver CLOSED with null payload");
            assertTrue(callback.isClosed());
            assertTrue(sm.isClosed(), "machine must be closed by session teardown");
            synchronized (order) {
                assertEquals("READY", order.get(0));
                assertEquals("CLOSED", order.get(order.size() - 1));
            }
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }
}
