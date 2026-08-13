package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * The full SMProto story in one test: a JSON config with an {@code exchange} script negotiates a
 * plaintext STARTTLS-style dialogue, {@code start_tls} upgrades the same connection to TLS against
 * a server-mode SSLSocket, and the post-READY application traffic round-trips encrypted.
 */
public class DataExchangeStartTLSTest {

    private static final int WAIT_SEC = 10;
    private static final String KEYSTORE = "src/test/resources/test.zoxweb.org.jks";
    private static final String KEYSTORE_PASSWORD = "password";

    @Test
    public void configDrivenStartTLSUpgrade() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
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

            // peer: SMTP-style greeting/EHLO/STARTTLS in plaintext, then server-side TLS + one echo
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    OutputStream out = accepted.getOutputStream();
                    InputStream in = accepted.getInputStream();
                    byte[] buf = new byte[512];

                    out.write(SharedStringUtil.getBytes("220 mx.test ESMTP\r\n"));
                    out.flush();
                    expectLine(in, buf, "EHLO");
                    out.write(SharedStringUtil.getBytes("250-mx.test\r\n250 ok\r\n"));
                    out.flush();
                    expectLine(in, buf, "STARTTLS");
                    out.write(SharedStringUtil.getBytes("220 Go\r\n"));
                    out.flush();

                    SSLSocket ssl = (SSLSocket) serverSSLContext.getSocketFactory()
                            .createSocket(accepted, null, accepted.getPort(), false);
                    ssl.setUseClientMode(false);
                    ssl.startHandshake();
                    int n = ssl.getInputStream().read(buf);
                    if (n > 0) {
                        ssl.getOutputStream().write(buf, 0, n);
                        ssl.getOutputStream().flush();
                    }
                    // hold until the client closes
                    ssl.getInputStream().read(buf);
                } catch (Exception e) {
                    failure.set(e);
                }
            }, "exchange-starttls-server");
            serverThread.start();

            String json = "{ \"name\": \"smtp-starttls\","
                    + " \"protocol\": \"plain\","
                    + " \"remote\": {\"host\": \"" + remote.getHostString() + "\", \"port\": " + remote.getPort() + "},"
                    + " \"tls\": {\"mode\": \"on_demand\", \"cert_validation\": false},"
                    + " \"exchange\": ["
                    + "   {\"expect\":    \"txt:220 \"},"
                    + "   {\"send\":      \"txt:EHLO client.test\\r\\n\"},"
                    + "   {\"expect\":    \"txt:250 \"},"
                    + "   {\"send\":      \"txt:STARTTLS\\r\\n\"},"
                    + "   {\"expect\":    \"txt:220 \"},"
                    + "   {\"expect\":    \"txt:\\r\\n\"},"
                    + "   {\"start_tls\": true}"
                    + " ] }";
            ClientConnectionSM sm = ClientSMFactory.fromJSON(json);
            final ClientSessionContext ctx = sm.getContext();

            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) sci -> order.add("SECURE"), ClientEvent.SECURE);
            app.register((Consumer<Object>) o -> {
                order.add("READY");
                app.register((Consumer<ByteBuffer>) bb -> {
                    byte[] chunk = new byte[bb.remaining()];
                    bb.get(chunk);
                    echoed.set(new String(chunk));
                    ByteBufferUtil.cache(bb);
                    echoLatch.countDown();
                }, ClientEvent.IN_DATA);
                readyLatch.countDown();
                try {
                    ctx.write(ByteBuffer.wrap(SharedStringUtil.getBytes("secured-hello")));
                } catch (IOException e) {
                    failure.set(e);
                }
            }, ClientEvent.READY);
            sm.register(app);

            callback = sm.newSessionCallback();
            nioSocket.addClientSocket(remote, callback, WAIT_SEC, null);

            assertTrue(readyLatch.await(WAIT_SEC, TimeUnit.SECONDS), "READY not reached through dialogue + upgrade");
            assertTrue(ctx.isSecure(), "session must be secure after start_tls");
            assertTrue(echoLatch.await(WAIT_SEC, TimeUnit.SECONDS), "encrypted echo not received");
            assertEquals("secured-hello", echoed.get());
            assertNull(failure.get(), "no failure expected: " + failure.get());
            // note: callback order between SECURE and READY is registration-order broadcast — the
            // exchange completes inside the SECURE dispatch, so a late-registered app consumer may
            // observe READY first; both must have fired, and the session was secure before READY
            synchronized (order) {
                assertTrue(order.contains("SECURE"), "SECURE must be observed: " + order);
                assertTrue(order.contains("READY"), "READY must be observed: " + order);
            }
        } finally {
            SharedIOUtil.close(callback, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    private static void expectLine(InputStream in, byte[] buf, String mustStartWith) throws IOException {
        int n = in.read(buf);
        String line = n > 0 ? new String(buf, 0, n) : "";
        if (!line.startsWith(mustStartWith))
            throw new IOException("expected " + mustStartWith + " got: " + line);
    }
}
