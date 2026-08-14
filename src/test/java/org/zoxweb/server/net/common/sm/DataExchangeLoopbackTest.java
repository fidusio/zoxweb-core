package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A config-driven scripted send/expect dialogue over a real loopback socket: a plain server plays
 * an SMTP-greeting-style exchange, and a machine built entirely from JSON drives it to READY.
 */
public class DataExchangeLoopbackTest {

    private static final int WAIT_SEC = 10;

    @Test
    public void scriptedDialogueReachesReady() throws Exception {
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();
        final AtomicReference<String> gotEhlo = new AtomicReference<String>();

        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPSMCallback callback = null;
        Thread serverThread = null;
        try {
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());

            // peer: greet, expect EHLO, reply with a multi-line 250 ending in "250 ok"
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    OutputStream out = accepted.getOutputStream();
                    out.write(SharedStringUtil.getBytes("220 mx.test ESMTP\r\n"));
                    out.flush();
                    InputStream in = accepted.getInputStream();
                    byte[] buf = new byte[256];
                    int n = in.read(buf);
                    gotEhlo.set(n > 0 ? new String(buf, 0, n) : "");
                    out.write(SharedStringUtil.getBytes("250-mx.test\r\n250 ok\r\n"));
                    out.flush();
                    // hold the socket open until the client closes so READY is observed first
                    in.read(buf);
                } catch (Exception e) {
                    serverFailure.set(e);
                }
            }, "exchange-test-server");
            serverThread.start();

            String json = "{ \"name\": \"smtp-dialogue\","
                    + " \"protocol\": \"plain\","
                    + " \"exchange\": ["
                    + "   {\"expect\": \"txt:220 \"},"
                    + "   {\"send\":   \"txt:EHLO client.test\\r\\n\"},"
                    + "   {\"expect\": \"txt:250 \"}"
                    + " ] }";
            ClientConSM sm = ClientSMFactory.fromJSON(json);
            assertNotNull(sm.lookupState(DataExchangePhase.NAME), "exchange phase must be registered");

            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) o -> readyLatch.countDown(), ClientEvent.READY);
            sm.register(app);

            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, WAIT_SEC, null);

            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not reached by the scripted dialogue");
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());
            assertNotNull(gotEhlo.get());
            assertTrue(gotEhlo.get().startsWith("EHLO client.test"), "server did not receive the EHLO send: " + gotEhlo.get());
        } finally {
            SharedIOUtil.close(callback, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    /**
     * The generic-config path: the {@code send} literal carries a {@code ${helo}} placeholder — no
     * client identity is baked into the protocol description — and the caller injects the value via
     * {@code ctx.setVar} before connecting. The server must receive the substituted EHLO.
     */
    @Test
    public void callerInjectedVariableReachesThePeer() throws Exception {
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();
        final AtomicReference<String> gotEhlo = new AtomicReference<String>();

        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPSMCallback callback = null;
        Thread serverThread = null;
        try {
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());

            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    OutputStream out = accepted.getOutputStream();
                    out.write(SharedStringUtil.getBytes("220 mx.test ESMTP\r\n"));
                    out.flush();
                    InputStream in = accepted.getInputStream();
                    byte[] buf = new byte[256];
                    int n = in.read(buf);
                    gotEhlo.set(n > 0 ? new String(buf, 0, n) : "");
                    out.write(SharedStringUtil.getBytes("250 ok\r\n"));
                    out.flush();
                    in.read(buf);
                } catch (Exception e) {
                    serverFailure.set(e);
                }
            }, "exchange-var-server");
            serverThread.start();

            // generic config: the HELO name is a ${helo} placeholder, not a hardcoded literal
            String json = "{ \"name\": \"smtp-dialogue\","
                    + " \"protocol\": \"plain\","
                    + " \"exchange\": ["
                    + "   {\"expect\": \"txt:220 \"},"
                    + "   {\"send\":   \"txt:EHLO ${helo}\\r\\n\"},"
                    + "   {\"expect\": \"txt:250 \"}"
                    + " ] }";
            ClientConSM sm = ClientSMFactory.fromJSON(json);
            // the caller injects the environment-specific value at connection time
            sm.getContext().setVar("helo", "probe.example.org");

            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) o -> readyLatch.countDown(), ClientEvent.READY);
            sm.register(app);

            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, WAIT_SEC, null);

            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not reached with a var-driven send");
            assertNull(serverFailure.get(), "server error: " + serverFailure.get());
            assertEquals("EHLO probe.example.org\r\n", gotEhlo.get(),
                    "server must receive the caller-injected HELO, not the literal placeholder");
        } finally {
            SharedIOUtil.close(callback, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }
}
