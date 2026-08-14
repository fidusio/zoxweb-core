package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The STARTTLS upgrade seam, without an SMTP negotiator: a minimal test phase consumes IN_DATA
 * in plaintext, and on the go-ahead line performs the residue check before publishing START_TLS
 * (residue after the go-ahead is attacker-controllable plaintext — fatal, never
 * clear-and-continue). The SSL phase runs in ON_DEMAND mode.
 * <ul>
 * <li>Positive: plain go-ahead → START_TLS → handshake against a server-mode SSLSocket →
 * SECURE → READY → encrypted echo round-trip.</li>
 * <li>Negative: go-ahead with trailing residue → CLOSED with the injection IOException, no
 * handshake attempted.</li>
 * </ul>
 */
public class StartTLSUpgradeSeamTest {

    private static final int WAIT_SEC = 10;
    private static final String KEYSTORE = "src/test/resources/test.zoxweb.org.jks";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String GO_AHEAD = "220 Go";

    /**
     * Minimal STARTTLS negotiator: waits for the go-ahead line, enforces the residue-is-fatal
     * rule, publishes START_TLS, and completes on SECURE.
     */
    static class GoAheadPhase implements ConnectionPhase {
        static final String NAME = "starttls-test";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean gatesReady() {
            return true;
        }

        @Override
        public void contribute(ClientConSM sm) {
            State<Object> state = new State<Object>(NAME);
            state.register(new GoAhead());
            state.register(new Secured());
            sm.register(state);
        }

        static class GoAhead extends TriggerConsumer<ByteBuffer> {
            private final ByteArrayOutputStream line = new ByteArrayOutputStream(32);
            private boolean done = false;

            GoAhead() {
                super(ClientEvent.IN_DATA);
            }

            @Override
            public void accept(ByteBuffer bb) {
                if (done)
                    return; // post-upgrade data belongs to the application
                ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
                try {
                    while (bb.hasRemaining()) {
                        byte b = bb.get();
                        if (b == '\n') {
                            String text = new String(line.toByteArray()).trim();
                            line.reset();
                            if (GO_AHEAD.equals(text)) {
                                // THE injection check: any byte after the go-ahead line was put
                                // there by whoever controls the wire, not the authenticated peer
                                if (bb.hasRemaining()) {
                                    ctx.fail(new IOException("STARTTLS injection: residue after go-ahead"));
                                    return;
                                }
                                done = true;
                                publishSync(ClientEvent.START_TLS, null);
                                return;
                            }
                        } else {
                            line.write(b);
                        }
                    }
                } finally {
                    ByteBufferUtil.cache(bb);
                }
            }
        }

        static class Secured extends TriggerConsumer<Object> {
            Secured() {
                super(ClientEvent.SECURE);
            }

            @Override
            public void accept(Object sci) {
                ((ClientSessionContext) getStateMachine().getConfig()).phaseComplete(NAME);
            }
        }
    }

    private ClientConSM buildMachine(InetSocketAddress remote) throws Exception {
        return ClientConSMBuilder.create("starttls-seam")
                .phase(new GoAheadPhase())
                .phase(new SSLClientPhase(new SSLContextInfo(remote, false), SSLClientPhase.TLSMode.ON_DEMAND))
                .build();
    }

    @Test
    public void goAheadUpgradesAndEchoesEncrypted() throws Exception {
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final CountDownLatch echoLatch = new CountDownLatch(1);
        final AtomicReference<String> echoed = new AtomicReference<String>();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        SSLContext serverSSLContext = SecUtil.initSSLContext(
                KEYSTORE, CryptoConst.PKCS12, KEYSTORE_PASSWORD.toCharArray(), null, null, null);
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPSMCallback callback = null;
        Thread serverThread = null;
        try {
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());

            // peer: plain go-ahead, then server-side TLS over the same socket, then echo once
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    accepted.getOutputStream().write(SharedStringUtil.getBytes(GO_AHEAD + "\r\n"));
                    accepted.getOutputStream().flush();
                    SSLSocket ssl = (SSLSocket) serverSSLContext.getSocketFactory()
                            .createSocket(accepted, null, accepted.getPort(), false);
                    ssl.setUseClientMode(false);
                    ssl.startHandshake();
                    byte[] buf = new byte[1024];
                    int n = ssl.getInputStream().read(buf);
                    if (n > 0) {
                        ssl.getOutputStream().write(buf, 0, n);
                        ssl.getOutputStream().flush();
                    }
                } catch (Exception e) {
                    failure.set(e);
                }
            }, "starttls-test-server");
            serverThread.start();

            ClientConSM sm = buildMachine(remote);
            final ClientSessionContext ctx = sm.getContext();
            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) o -> {
                app.register((Consumer<ByteBuffer>) bb -> {
                    byte[] chunk = new byte[bb.remaining()];
                    bb.get(chunk);
                    echoed.set(new String(chunk));
                    ByteBufferUtil.cache(bb);
                    echoLatch.countDown();
                }, ClientEvent.IN_DATA);
                readyLatch.countDown();
                try {
                    ctx.write(ByteBuffer.wrap(SharedStringUtil.getBytes("ping-after-upgrade")));
                } catch (IOException e) {
                    failure.set(e);
                }
            }, ClientEvent.READY);
            sm.register(app);

            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, WAIT_SEC, null);

            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not published after upgrade");
            assertTrue(ctx.isSecure(), "session must be secure after the upgrade");
            assertTrue(echoLatch.await(WAIT_SEC, TimeUnit.SECONDS), "encrypted echo not received");
            assertEquals("ping-after-upgrade", echoed.get());
            assertNull(failure.get(), "no failure expected: " + failure.get());
        } finally {
            SharedIOUtil.close(callback, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    @Test
    public void residueAfterGoAheadIsFatalAndNoHandshakeAttempted() throws Exception {
        final CountDownLatch closedLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>();
        final AtomicBoolean secureFired = new AtomicBoolean(false);
        final AtomicBoolean peerSawEOF = new AtomicBoolean(false);

        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPSMCallback callback = null;
        Thread serverThread = null;
        try {
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());

            // malicious peer: go-ahead with injected plaintext trailing it, then wait for EOF
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    accepted.getOutputStream().write(SharedStringUtil.getBytes(GO_AHEAD + "\r\nEVIL-INJECTED"));
                    accepted.getOutputStream().flush();
                    peerSawEOF.set(accepted.getInputStream().read() == -1);
                } catch (Exception ignored) {
                    // socket reset by the failing client also proves the drop
                    peerSawEOF.set(true);
                }
            }, "starttls-injection-server");
            serverThread.start();

            ClientConSM sm = buildMachine(remote);
            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) o -> secureFired.set(true), ClientEvent.SECURE);
            app.register((Consumer<Throwable>) t -> {
                closedPayload.set(t);
                closedLatch.countDown();
            }, SMProtoUtil.BasicEvent.CLOSED);
            sm.register(app);

            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, WAIT_SEC, null);

            assertTrue(closedLatch.await(WAIT_SEC, TimeUnit.SECONDS), "residue must tear the session down");
            assertTrue(closedPayload.get() instanceof IOException, "CLOSED payload must be the injection IOException");
            assertTrue(closedPayload.get().getMessage().contains("injection"),
                    "cause must name the injection: " + closedPayload.get());
            assertFalse(secureFired.get(), "no handshake may be attempted on residue");
            assertTrue(sm.isClosed(), "machine must be closed by teardown");
            serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
            assertTrue(peerSawEOF.get(), "the connection must be dropped, not continued");
        } finally {
            SharedIOUtil.close(callback, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }
}
