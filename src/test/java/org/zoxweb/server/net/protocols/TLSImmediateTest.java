package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLNIOSocketHandlerFactory;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Immediate-mode TLS end to end: the existing SSL server stack ({@code SSLNIOSocketHandlerFactory}
 * + echo callback, test keystore, PKCS12/"password") terminates TLS on the loopback; the validator
 * runs its whole script encrypted — handshake before the first send, echo validated, negotiated
 * {@code tls_protocol}/{@code tls_cipher} recorded, {@code close_on_ready} self-close.
 */
public class TLSImmediateTest {

    private static final int WAIT_SEC = 10;
    private static final String KEYSTORE = "src/test/resources/test.zoxweb.org.jks";
    private static final String KEYSTORE_PASSWORD = "password";

    /** Server-side echo: decrypted app data written straight back through the SSL output stream. */
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
    public void immediateTlsScriptedEcho() throws Exception {
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        TCPMetaProtocol validator = null;
        try {
            SSLContextInfo serverCtx = new SSLContextInfo(SecUtil.initSSLContext(
                    KEYSTORE, CryptoConst.PKCS12, KEYSTORE_PASSWORD.toCharArray(), null, null, null));
            SelectionKey serverKey = nioSocket.addServerSocket(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 16,
                    new SSLNIOSocketHandlerFactory(serverCtx, EchoSSLCallback::new));
            int port = ((ServerSocketChannel) serverKey.channel()).socket().getLocalPort();

            // cert_validation false: self-signed test certificate
            validator = ProtoConnect.createTCPProtocol(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                    "{ \"name\": \"tls-echo\", \"close_on_ready\": true,"
                            + " \"tls\": {\"mode\": \"immediate\", \"cert_validation\": false},"
                            + " \"exchange\": ["
                            + "  {\"send\": \"txt:ping-over-tls\"},"
                            + "  {\"expect\": \"txt:ping-over-tls\"},"
                            + "  {\"validate\": {\"exact\": \"txt:ping-over-tls\", \"report\": \"echo\"}}"
                            + " ] }");

            nioSocket.addClientSocket(validator);

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "validated echo must complete and self-close");
            assertNull(validator.getCloseCause(), "clean completion");
            assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
            assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
            assertEquals("ping-over-tls", validator.getResults().getValue("echo"));
            assertNotNull(validator.getResults().getValue("tls_protocol"), "negotiated protocol recorded");
            assertNotNull(validator.getResults().getValue("tls_cipher"), "negotiated cipher recorded");
        } finally {
            SharedIOUtil.close(validator, nioSocket);
        }
    }
}
