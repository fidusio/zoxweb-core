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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The TCP validator over a real loopback socket: a scripted send/expect/validate dialogue driven
 * entirely by the JSON definition — greeting, variable-injected request, validated reply,
 * {@code close_on_ready} self-close, verdict in the results — and the clean peer-EOF ending.
 */
public class PlainLoopbackTest {

    private static final int WAIT_SEC = 10;

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

    @Test
    public void scriptedDialogueToReady() throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        TCPMetaProtocol validator = ProtoConnect.createTCPValidator(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                "{ \"name\": \"greeting-check\", \"close_on_ready\": true,"
                        + " \"vars\": {\"who\": \"default\"},"
                        + " \"exchange\": ["
                        + "  {\"expect\": \"txt:220 ready\\r\\n\"},"
                        + "  {\"send\": \"txt:HELLO ${who}\\r\\n\"},"
                        + "  {\"expect\": \"txt:250 ok\\r\\n\"},"
                        + "  {\"validate\": {\"prefix\": \"txt:250\", \"report\": \"reply\"}}"
                        + " ] }");
        validator.getScript().setVar("who", "probe");

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(validator);
            accepted = server.accept();

            accepted.getOutputStream().write(SharedStringUtil.getBytes("220 ready\r\n"));
            accepted.getOutputStream().flush();

            String request = readLine(accepted.getInputStream());
            assertEquals("HELLO probe\r\n", request, "send-time var resolution on the wire");

            accepted.getOutputStream().write(SharedStringUtil.getBytes("250 ok\r\n"));
            accepted.getOutputStream().flush();

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "close_on_ready must close the session after the script completes");
            assertTrue(validator.isClosed());
            assertNull(validator.getCloseCause(), "a completed run closes cleanly");
            assertEquals(Boolean.TRUE, validator.getResults().getValue("validated"));
            assertEquals(Boolean.TRUE, validator.getResults().getValue("ready"));
            assertEquals("250 ok\r\n", validator.getResults().getValue("reply"), "report through the match");
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }

    @Test
    public void peerEofClosesCleanly() throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        TCPMetaProtocol validator = ProtoConnect.createTCPValidator(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                "{ \"exchange\": [ {\"expect\": \"txt:NEVER\"} ] }");

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(validator);
            accepted = server.accept();

            accepted.getOutputStream().write(SharedStringUtil.getBytes("unmatched noise"));
            accepted.getOutputStream().flush();
            accepted.close();

            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "session must close on peer EOF");
            assertTrue(validator.isClosed());
            assertNull(validator.getCloseCause(), "clean EOF carries no cause");
            assertNull(validator.getResults().getNV("validated"),
                    "an incomplete run records no verdict — the missing 'ready' tells the story");
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }
}
