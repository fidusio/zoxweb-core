package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.ByteArrayOutputStream;
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
 * The SSH checks as pure JSON definitions: {@code protocols/ssh-banner.json} (banner-only
 * reachability + version validation) and {@code protocols/ssh-banner-kex.json} (banner, then the
 * mid-script {@code boundary} switch to RFC 4253 binary packets, the {@code extract} capture of
 * the server's key-exchange name-list from {@code SSH_MSG_KEXINIT}, and the {@code optional}
 * post-quantum kex probe) — plus the banner-mismatch negative (SSH-1.x fails before the client
 * ident is ever sent).
 */
public class SSHBannerTest {

    private static final int WAIT_SEC = 10;
    private static final String DEFINITION_BANNER = "src/test/resources/protocols/ssh-banner.json";
    private static final String DEFINITION_KEX = "src/test/resources/protocols/ssh-banner-kex.json";
    private static final String KEX_LIST_PQ = "sntrup761x25519-sha512@openssh.com,curve25519-sha256,ecdh-sha2-nistp256";
    private static final String KEX_LIST_CLASSIC = "curve25519-sha256,ecdh-sha2-nistp256";

    private static String definitionJSON(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /**
     * Manual runner against a real SSH server, driving the full {@code ssh-banner-kex.json}
     * definition (banner validation + kex name-list capture + post-quantum probe):
     * <pre>
     *   SSHBannerTest &lt;host[:port]&gt;   (port defaults to the definition's 22 hint)
     * </pre>
     * Delegates to the {@code ProtoConnect} CLI, so the report and exit codes are the
     * operational ones: 0 validated, 1 failed, 2 no completion, 64 usage. Run from the repo
     * root — the definition path is repo-relative.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: SSHBannerTest <host[:port]>");
            System.exit(64);
        }
        System.exit(ProtoConnect.run(new String[]{DEFINITION_KEX, args[0]}));
    }

    /**
     * A KEXINIT-shaped RFC 4253 binary packet: uint32 packet_length, padding_length, type 20,
     * 16-byte cookie, uint32 name-list length, the kex name-list. Enough structure for the
     * definition's length_prefixed framing and the fixed-offset extract.
     */
    private static byte[] syntheticKexInit(String kexList) throws Exception {
        byte[] names = SharedStringUtil.getBytes(kexList);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(0);                        // padding_length
        payload.write(20);                       // SSH_MSG_KEXINIT
        payload.write(new byte[16]);             // cookie
        payload.write((names.length >>> 24) & 0xFF);
        payload.write((names.length >>> 16) & 0xFF);
        payload.write((names.length >>> 8) & 0xFF);
        payload.write(names.length & 0xFF);
        payload.write(names);
        byte[] body = payload.toByteArray();
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write((body.length >>> 24) & 0xFF);
        frame.write((body.length >>> 16) & 0xFF);
        frame.write((body.length >>> 8) & 0xFF);
        frame.write(body.length & 0xFF);
        frame.write(body);
        return frame.toByteArray();
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

    /**
     * Drives a definition against a loopback banner server; {@code kexInit} non-null makes the
     * server wait for the client ident and answer with the synthetic KEXINIT (the GitHub shape).
     */
    private TCPMetaProtocol probe(String definitionPath, String banner, byte[] kexInit) throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        TCPMetaProtocol validator = ProtoConnect.createTCPProtocol(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                definitionJSON(definitionPath));
        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        Socket accepted = null;
        try {
            nioSocket.addClientSocket(validator);
            accepted = server.accept();
            accepted.getOutputStream().write(SharedStringUtil.getBytes(banner));
            accepted.getOutputStream().flush();
            if (kexInit != null) {
                String clientIdent = readLine(accepted.getInputStream());
                assertEquals("SSH-2.0-generic_probe\r\n", clientIdent, "scripted client ident on the wire");
                accepted.getOutputStream().write(kexInit);
                accepted.getOutputStream().flush();
            }
            assertTrue(validator.waitForClose(TimeUnit.SECONDS.toMillis(WAIT_SEC)),
                    "verdict must close the session");
            return validator;
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }

    @Test
    public void bannerOnlyDefinitionCompletes() throws Exception {
        TCPMetaProtocol v = probe(DEFINITION_BANNER, "SSH-2.0-OpenSSH_9.6 zoxweb-test\r\n", null);
        assertNull(v.getCloseCause(), "clean completion, cause: " + v.getCloseCause());
        assertEquals(Boolean.TRUE, v.getResults().getValue("validated"));
        assertEquals(Boolean.TRUE, v.getResults().getValue("ready"));
        assertEquals("SSH-2.0-OpenSSH_9.6 zoxweb-test", v.getResults().getValue("banner"),
                "banner reported with terminator and CR stripped");
    }

    @Test
    public void bannerAndKexListCapturedWithPQProbe() throws Exception {
        TCPMetaProtocol v = probe(DEFINITION_KEX, "SSH-2.0-OpenSSH_9.6 zoxweb-test\r\n", syntheticKexInit(KEX_LIST_PQ));
        assertNull(v.getCloseCause(), "clean completion, cause: " + v.getCloseCause());
        assertEquals(Boolean.TRUE, v.getResults().getValue("validated"));
        assertEquals(Boolean.TRUE, v.getResults().getValue("ready"));
        assertEquals("SSH-2.0-OpenSSH_9.6 zoxweb-test", v.getResults().getValue("banner"));
        assertEquals(KEX_LIST_PQ, v.getResults().getValue("kex_algorithms"),
                "kex name-list extracted from the KEXINIT frame");
        assertEquals(Boolean.TRUE, v.getResults().getValue("pq_kex"), "sntrup761 present → probe true");
    }

    @Test
    public void pqProbeReportsFalseWithoutFailing() throws Exception {
        TCPMetaProtocol v = probe(DEFINITION_KEX, "SSH-2.0-OpenSSH_8.9\r\n", syntheticKexInit(KEX_LIST_CLASSIC));
        assertNull(v.getCloseCause(), "an optional probe miss never fails the session");
        assertEquals(Boolean.TRUE, v.getResults().getValue("validated"));
        assertEquals(Boolean.TRUE, v.getResults().getValue("ready"));
        assertEquals(KEX_LIST_CLASSIC, v.getResults().getValue("kex_algorithms"));
        assertEquals(Boolean.FALSE, v.getResults().getValue("pq_kex"), "sntrup761 absent → probe false");
    }

    @Test
    public void banner1xFailsPrefixValidation() throws Exception {
        TCPMetaProtocol v = probe(DEFINITION_KEX, "SSH-1.99-legacy\r\n", null);
        assertNotNull(v.getCloseCause(), "mismatch must fail the session");
        assertEquals(Boolean.FALSE, v.getResults().getValue("validated"));
        String reason = (String) v.getResults().getValue("reason");
        assertTrue(reason.contains("prefix mismatch"), reason);
        assertNull(v.getResults().getNV("ready"), "a failed session is never ready");
        assertNull(v.getResults().getNV("kex_algorithms"), "no kex capture on a failed banner");
        assertNull(v.getResults().getNV("pq_kex"), "the probe never ran on a failed banner");
    }
}
