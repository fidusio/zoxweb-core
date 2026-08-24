package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The STARTTLS story driven by the pure-JSON definition {@code protocols/smtp-starttls.json}
 * (RFC 3207 shape): plaintext EHLO dialogue → {@code start_tls} upgrade against a server-mode
 * SSLSocket → post-upgrade EHLO validated over the encrypted link — plus the injection negative:
 * plaintext trailing the go-ahead fails the session before any handshake.
 */
public class SMTPStartTLSTest {

    private static final int WAIT_SEC = 10;
    private static final String DEFINITION = "src/test/resources/protocols/smtp-starttls.json";
    private static final String KEYSTORE = "src/test/resources/test.zoxweb.org.jks";
    private static final String KEYSTORE_PASSWORD = "password";

    private static String definitionJSON() throws Exception {
        return new String(Files.readAllBytes(Paths.get(DEFINITION)), StandardCharsets.UTF_8);
    }

    private static void expectLine(InputStream in, byte[] buf, String mustStartWith) throws IOException {
        int n = in.read(buf);
        String line = n > 0 ? new String(buf, 0, n) : "";
        if (!line.startsWith(mustStartWith))
            throw new IOException("expected " + mustStartWith + " got: " + line);
    }

    @Test
    public void definitionDrivenStartTLSUpgrade() throws Exception {
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();

        SSLContext serverSSLContext = SecUtil.initSSLContext(
                KEYSTORE, CryptoConst.PKCS12, KEYSTORE_PASSWORD.toCharArray(), null, null, null);
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPMetaProtocol validator = null;
        Thread serverThread = null;
        try {
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());

            // peer: SMTP greeting/EHLO/STARTTLS in plaintext, then server-side TLS + post-TLS EHLO
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    OutputStream out = accepted.getOutputStream();
                    InputStream in = accepted.getInputStream();
                    byte[] buf = new byte[512];

                    out.write(SharedStringUtil.getBytes("220 mx.test ESMTP\r\n"));
                    out.flush();
                    expectLine(in, buf, "EHLO probe.local");
                    out.write(SharedStringUtil.getBytes("250-mx.test\r\n250 STARTTLS\r\n"));
                    out.flush();
                    expectLine(in, buf, "STARTTLS");
                    out.write(SharedStringUtil.getBytes("220 Go\r\n"));
                    out.flush();

                    SSLSocket ssl = (SSLSocket) serverSSLContext.getSocketFactory()
                            .createSocket(accepted, null, accepted.getPort(), false);
                    ssl.setUseClientMode(false);
                    ssl.startHandshake();
                    expectLine(ssl.getInputStream(), buf, "EHLO probe.local");
                    ssl.getOutputStream().write(SharedStringUtil.getBytes("250 mx.test ready\r\n"));
                    ssl.getOutputStream().flush();
                    // hold until the client closes
                    ssl.getInputStream().read(buf);
                } catch (Exception e) {
                    serverFailure.set(e);
                }
            }, "smtp-starttls-server");
            serverThread.start();

            validator = ProtoConnect.createTCPValidator(remote, definitionJSON());
            nioSocket.addClientSocket(validator);

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "dialogue + upgrade + post-TLS EHLO must complete and self-close");
            assertNull(validator.getCloseCause(), "clean completion");
            assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
            assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
            String postTlsEhlo = (String) validator.getResults().getValue("post_tls_ehlo");
            assertNotNull(postTlsEhlo, "post-upgrade EHLO reply reported");
            assertTrue(postTlsEhlo.contains("250"), postTlsEhlo);
            assertNotNull(validator.getResults().getValue("tls_protocol"), "negotiated protocol recorded");
            assertNotNull(validator.getResults().getValue("tls_cipher"), "negotiated cipher recorded");
            assertNull(serverFailure.get(), "server side clean: " + serverFailure.get());
        } finally {
            SharedIOUtil.close(validator, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }

    @Test
    public void injectionResidueFailsBeforeHandshake() throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPMetaProtocol validator = null;
        Thread serverThread = null;
        try {
            InetSocketAddress remote = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());

            // hostile peer: the go-ahead and an injected plaintext command in one segment
            serverThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    accepted.getOutputStream().write(
                            SharedStringUtil.getBytes("220 Go\r\nEVIL INJECTED COMMAND\r\n"));
                    accepted.getOutputStream().flush();
                    // hold until the client closes
                    accepted.getInputStream().read(new byte[64]);
                } catch (Exception ignored) {
                    // the client slams the door — expected
                }
            }, "smtp-injection-server");
            serverThread.start();

            validator = ProtoConnect.createTCPValidator(remote,
                    "{ \"tls\": {\"mode\": \"on_demand\", \"cert_validation\": false},"
                            + " \"exchange\": ["
                            + "  {\"expect\": \"txt:220 \"},"
                            + "  {\"expect\": \"txt:\\r\\n\"},"
                            + "  {\"start_tls\": true}"
                            + " ] }");
            nioSocket.addClientSocket(validator);

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "injection must fail the session");
            assertNotNull(validator.getCloseCause());
            assertTrue(validator.getCloseCause().getMessage().contains("STARTTLS injection"),
                    "cause: " + validator.getCloseCause());
            assertEquals(Boolean.FALSE, validator.getResults().getValue("validated"));
            assertNull(validator.getResults().getNV("ready"), "a failed session is never ready");
            assertNull(validator.getResults().getNV("tls_protocol"), "no handshake on residue");
        } finally {
            SharedIOUtil.close(validator, server, nioSocket);
            if (serverThread != null)
                serverThread.join(TimeUnit.SECONDS.toMillis(WAIT_SEC));
        }
    }
}
