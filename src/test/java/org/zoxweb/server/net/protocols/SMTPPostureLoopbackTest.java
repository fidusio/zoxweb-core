package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The guarded-linear-script story over a real loopback socket, driven entirely by the pure-JSON
 * definition {@code protocols/smtp-posture.json}: one definition, two server postures. A server
 * that advertises STARTTLS takes the main path; one that answers plain {@code 250} routes through
 * the expect's {@code alt} to the {@code plaintext_only} label. Both runs complete with
 * {@code validated: true} — the branch verdict lives in the {@code starttls_offered} record key.
 */
public class SMTPPostureLoopbackTest {

    private static final int WAIT_SEC = 10;
    private static final String DEFINITION = "src/test/resources/protocols/smtp-posture.json";

    private static String definitionJSON() throws Exception {
        return new String(Files.readAllBytes(Paths.get(DEFINITION)), StandardCharsets.UTF_8);
    }

    private static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char) c);
            if (c == '\n')
                break;
        }
        return sb.toString();
    }

    private TCPMetaProtocol runAgainst(String ehloReply) throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        TCPMetaProtocol validator = ProtoConnect.createTCPProtocol(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                definitionJSON());
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(validator);
            accepted = server.accept();

            accepted.getOutputStream().write(SharedStringUtil.getBytes("220 smtp.local ready\r\n"));
            accepted.getOutputStream().flush();

            String ehlo = readLine(accepted.getInputStream());
            assertTrue(ehlo.startsWith("EHLO "), "definition identifies itself: " + ehlo);

            accepted.getOutputStream().write(SharedStringUtil.getBytes(ehloReply));
            accepted.getOutputStream().flush();

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "close_on_ready must close the session after the script completes");
            return validator;
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }

    @Test
    public void starttlsOfferedTakesTheMainPath() throws Exception {
        TCPMetaProtocol validator = runAgainst("250-smtp.local\r\n250-STARTTLS\r\n250 ok\r\n");
        assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
        assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
        assertEquals(Boolean.TRUE, validator.getResults().getValue("starttls_offered"));
    }

    @Test
    public void plainOnlyRoutesThroughTheAlt() throws Exception {
        TCPMetaProtocol validator = runAgainst("250-smtp.local\r\n250 ok\r\n");
        assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
        assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
        assertEquals(Boolean.FALSE, validator.getResults().getValue("starttls_offered"));
    }
}
