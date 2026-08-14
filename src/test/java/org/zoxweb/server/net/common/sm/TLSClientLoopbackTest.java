package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLNIOSocketHandlerFactory;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The self-handshake integration test: the existing SSL server stack
 * ({@code SSLNIOSocketHandlerFactory} + echo callback, test keystore
 * {@code src/test/resources/test.zoxweb.org.jks}, PKCS12, password "password") terminates TLS
 * on the loopback; the client is a factory-built IMMEDIATE-mode {@link ClientConnectionSM}
 * with certificate validation disabled (self-signed cert).
 * <p>
 * Asserts SECURE → READY ordering, that the first application write after READY round-trips
 * encrypted (echo arrives as decrypted IN_DATA), and that teardown delivers CLOSED with the
 * machine closed last.
 */
public class TLSClientLoopbackTest {

    private static final int WAIT_SEC = 10;
    private static final String KEYSTORE = "src/test/resources/test.zoxweb.org.jks";
    private static final String KEYSTORE_PASSWORD = "password";

    /**
     * Server-side echo: decrypted app data (write-mode buffer) is written straight back through
     * the SSL output stream.
     */
    public static class EchoSSLCallback extends BaseSessionCallback<SSLSessionConfig> {
        @Override
        public void accept(ByteBuffer bb) {
            try {
                get().write(bb, true);
            } catch (IOException e) {
                exception(e);
            }
        }

        @Override
        public void exception(Throwable e) {
            SharedIOUtil.close(getConfig());
        }
    }

    @Test
    public void tlsImmediateLifecycle() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch secureLatch = new CountDownLatch(1);
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final CountDownLatch echoLatch = new CountDownLatch(1);
        final CountDownLatch closedLatch = new CountDownLatch(1);
        final AtomicReference<String> echoed = new AtomicReference<String>();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPSMCallback callback = null;
        try {
            // server: existing SSL stack + echo callback on an ephemeral loopback port
            SSLContextInfo serverCtx = new SSLContextInfo(SecUtil.initSSLContext(
                    KEYSTORE, CryptoConst.PKCS12, KEYSTORE_PASSWORD.toCharArray(), null, null, null));
            SelectionKey serverKey = nioSocket.addServerSocket(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 16,
                    new SSLNIOSocketHandlerFactory(serverCtx, EchoSSLCallback::new));
            int port = ((ServerSocketChannel) serverKey.channel()).socket().getLocalPort();
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);

            // client: IMMEDIATE TLS machine, cert validation off (self-signed server cert)
            ClientConnectionSM sm = ClientConnectionSMBuilder.create("tls-loopback")
                    .phase(new SSLClientPhase(new SSLContextInfo(remote, false), SSLClientPhase.TLSMode.IMMEDIATE))
                    .build();
            final ClientSessionContext ctx = sm.getContext();

            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) sci -> {
                order.add("SECURE");
                secureLatch.countDown();
            }, ClientEvent.SECURE);
            app.register((Consumer<Object>) o -> {
                order.add("READY");
                // post-READY owner registers its IN_DATA consumer, then sends the first message
                app.register((Consumer<ByteBuffer>) bb -> {
                    order.add("IN_DATA");
                    byte[] chunk = new byte[bb.remaining()];
                    bb.get(chunk);
                    echoed.set(new String(chunk));
                    ByteBufferUtil.cache(bb);
                    echoLatch.countDown();
                }, ClientEvent.IN_DATA);
                readyLatch.countDown();
                try {
                    ctx.write(ByteBuffer.wrap(SharedStringUtil.getBytes("ping-over-tls")));
                } catch (IOException e) {
                    failure.set(e);
                }
            }, ClientEvent.READY);
            app.register((Consumer<Throwable>) t -> {
                order.add("CLOSED");
                closedLatch.countDown();
            }, TCPSMCallback.BasicEvent.CLOSED);
            sm.register(app);

            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, WAIT_SEC, null);

            assertTrue(secureLatch.await(WAIT_SEC, TimeUnit.SECONDS), "SECURE not published — handshake failed");
            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not published after SECURE");
            assertTrue(echoLatch.await(WAIT_SEC, TimeUnit.SECONDS), "echo not received as decrypted IN_DATA");
            assertNull(failure.get(), "post-READY write must succeed: " + failure.get());
            assertEquals("ping-over-tls", echoed.get());
            assertTrue(ctx.isSecure());

            callback.close();
            assertTrue(closedLatch.await(WAIT_SEC, TimeUnit.SECONDS), "CLOSED not published on session close");
            assertTrue(sm.isClosed(), "machine must be closed by session teardown");
            synchronized (order) {
                assertTrue(order.indexOf("SECURE") < order.indexOf("READY"), "SECURE must precede READY: " + order);
                assertTrue(order.indexOf("READY") < order.indexOf("IN_DATA"), "READY must precede echo data: " + order);
                assertEquals("CLOSED", order.get(order.size() - 1), "CLOSED must be last: " + order);
            }
        } finally {
            SharedIOUtil.close(callback, nioSocket);
        }
    }

    /**
     * The endpoint-free JSON path: a config carrying NO remote is built by the factory, and the
     * deferred TLS phase binds its SSLContextInfo to the address the caller hands
     * {@code addClientSocket} at connect time. Proves the handshake completes against that
     * connected endpoint (SNI/context resolved from the socket, not the config).
     */
    @Test
    public void deferredTlsFromEndpointFreeJson() throws Exception {
        final CountDownLatch secureLatch = new CountDownLatch(1);
        final CountDownLatch echoLatch = new CountDownLatch(1);
        final AtomicReference<String> echoed = new AtomicReference<String>();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPSMCallback callback = null;
        try {
            SSLContextInfo serverCtx = new SSLContextInfo(SecUtil.initSSLContext(
                    KEYSTORE, CryptoConst.PKCS12, KEYSTORE_PASSWORD.toCharArray(), null, null, null));
            SelectionKey serverKey = nioSocket.addServerSocket(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 16,
                    new SSLNIOSocketHandlerFactory(serverCtx, EchoSSLCallback::new));
            int port = ((ServerSocketChannel) serverKey.channel()).socket().getLocalPort();

            // config carries NO remote — only the protocol/tls behavior + a port hint
            ClientConnectionSM sm = ClientSMFactory.fromJSON(
                    "{ \"name\": \"tls-deferred\", \"port\": " + port + ","
                            + " \"protocol\": \"tls\", \"tls\": {\"mode\": \"immediate\", \"cert_validation\": false} }");
            final ClientSessionContext ctx = sm.getContext();

            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) sci -> secureLatch.countDown(), ClientEvent.SECURE);
            app.register((Consumer<Object>) o -> {
                app.register((Consumer<ByteBuffer>) bb -> {
                    byte[] chunk = new byte[bb.remaining()];
                    bb.get(chunk);
                    echoed.set(new String(chunk));
                    ByteBufferUtil.cache(bb);
                    echoLatch.countDown();
                }, ClientEvent.IN_DATA);
                try {
                    ctx.write(ByteBuffer.wrap(SharedStringUtil.getBytes("deferred-ping")));
                } catch (IOException e) {
                    failure.set(e);
                }
            }, ClientEvent.READY);
            sm.register(app);

            // the caller supplies the InetSocketAddress; the config never did
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    ClientSMFactory.port(ctx.getSettings(), 443));
            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, ClientSMFactory.timeoutSec(ctx.getSettings()), null);

            assertTrue(secureLatch.await(WAIT_SEC, TimeUnit.SECONDS), "SECURE not published — deferred handshake failed");
            assertTrue(echoLatch.await(WAIT_SEC, TimeUnit.SECONDS), "echo not received over the deferred-TLS session");
            assertNull(failure.get(), "post-READY write must succeed: " + failure.get());
            assertEquals("deferred-ping", echoed.get());
            assertTrue(ctx.isSecure());
        } finally {
            SharedIOUtil.close(callback, nioSocket);
        }
    }
}
