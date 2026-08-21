package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOBuffers;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the {@code ssh_kex} catalog state — the SSH KEXINIT post-quantum readiness check —
 * without sockets: CONNECTED and IN_RAW_DATA are published directly (banner bytes flow through
 * the delimited assembler + banner controller; once the script completes the kex state owns the
 * wire bytes). Synthetic RFC 4253 binary packets exercise framing, skipping, verdicts, and the
 * fail paths (an unconnected TCPSMCallback's teardown publishes CLOSED without a channel).
 */
public class SSHKexTest {

    // send_ident false: these harnesses have no channel — the client-identification send (on by
    // default with the kex check, see SSHKexLoopback below) would fail the write
    private static final String CHECK_JSON =
            "{ \"name\": \"ssh-kex-test\", \"protocol\": \"ssh\"," +
            " \"ssh\": {\"kex_check\": true, \"send_ident\": false} }";
    private static final String REQUIRED_JSON =
            "{ \"name\": \"ssh-kex-test\", \"protocol\": \"ssh\"," +
            " \"ssh\": {\"pq_required\": true, \"send_ident\": false} }";

    private static final String BANNER = "SSH-2.0-OpenSSH_9.6\r\n";
    private static final String CLASSIC_KEX = "curve25519-sha256,ecdh-sha2-nistp256,diffie-hellman-group14-sha256";
    private static final String PQ_KEX = "sntrup761x25519-sha512@openssh.com,mlkem768x25519-sha256," + CLASSIC_KEX;

    /** One SSH binary packet (RFC 4253 §6): uint32 length, byte padding, payload, padding. */
    static byte[] packet(int type, byte[] body) {
        int payloadLen = 1 + body.length;
        int padding = 4;
        ByteBuffer out = ByteBuffer.allocate(4 + payloadLen + padding + 1);
        out.putInt(payloadLen + padding + 1);
        out.put((byte) padding);
        out.put((byte) type);
        out.put(body);
        out.put(new byte[padding]);
        return out.array();
    }

    /** A KEXINIT truncated after the first name-list — all the parser reads. */
    static byte[] kexinit(String kexAlgorithms) {
        byte[] names = kexAlgorithms.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer body = ByteBuffer.allocate(16 + 4 + names.length);
        body.put(new byte[16]); // cookie
        body.putInt(names.length);
        body.put(names);
        return packet(SSHKexState.SSH_MSG_KEXINIT, body.array());
    }

    static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts)
            total += p.length;
        byte[] ret = new byte[total];
        int at = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, ret, at, p.length);
            at += p.length;
        }
        return ret;
    }

    private static class Harness {
        final ClientConSM sm;
        final TCPSMCallback callback;
        final AtomicInteger readyCount = new AtomicInteger();
        final AtomicReference<Throwable> closedPayload = new AtomicReference<Throwable>();
        final AtomicInteger closedCount = new AtomicInteger();
        final AtomicLong packetCounter = new AtomicLong();
        final StringBuilder postReadyData = new StringBuilder();

        Harness(String json) {
            sm = ClientSMFactory.fromJSON(json);
            State<Object> app = new State<Object>("app");
            app.register((Consumer<Object>) o -> {
                readyCount.incrementAndGet();
                // the post-READY owner registers its IN_DATA consumer from the READY handler
                app.register((Consumer<ByteBuffer>) bb -> {
                    byte[] chunk = new byte[bb.remaining()];
                    bb.get(chunk);
                    postReadyData.append(new String(chunk, StandardCharsets.US_ASCII));
                    ByteBufferUtil.cache(bb);
                }, CommonTrigger.IN_DATA);
            }, CommonTrigger.READY);
            app.register((Consumer<Throwable>) t -> {
                closedPayload.set(t);
                closedCount.incrementAndGet();
            }, CommonTrigger.CLOSED);
            sm.register(app);
            callback = sm.newSessionCallback();
            sm.publishSync(CommonTrigger.CONNECTED, null);
        }

        void feed(byte[] wire) {
            // IN_RAW_DATA carries a DataPacket wrapping the borrowed pair (the router copies,
            // never recaches); no channel here, and the peer address is a placeholder
            sm.publishSync(CommonTrigger.IN_RAW_DATA, new DataPacket<Long>(
                    packetCounter.incrementAndGet(), null,
                    InetSocketAddress.createUnresolved("test.local", 22),
                    new IOBuffers().setInBuffer(ByteBufferUtil.allocateByteBuffer(
                            ByteBufferUtil.BufferType.HEAP, wire, 0, wire.length, true))));
        }

        void feed(String wire) {
            feed(SharedStringUtil.getBytes(wire));
        }

        Object result(String key) {
            return SMProtoUtil.results(sm).getValue(key);
        }
    }

    @Test
    public void pqOfferedRecorded() {
        Harness h = new Harness(CHECK_JSON);
        h.feed(BANNER);
        assertEquals(0, h.readyCount.get(), "ssh_kex gates READY: the banner alone must not complete the pipeline");
        assertEquals("SSH-2.0-OpenSSH_9.6", h.result("banner"));

        h.feed(kexinit(PQ_KEX));
        assertEquals(PQ_KEX, h.result("kex_algorithms"));
        assertEquals(Boolean.TRUE, h.result("pq_kex"));
        assertEquals("sntrup761x25519-sha512@openssh.com,mlkem768x25519-sha256", h.result("pq_kex_algorithms"));
        assertEquals(Boolean.TRUE, h.result("validated"));
        assertEquals(1, h.readyCount.get());
        assertEquals(0, h.closedCount.get());
    }

    @Test
    public void pqAbsentRecordedNotFatal() {
        Harness h = new Harness(CHECK_JSON);
        h.feed(BANNER);
        h.feed(kexinit(CLASSIC_KEX));

        assertEquals(CLASSIC_KEX, h.result("kex_algorithms"));
        assertEquals(Boolean.FALSE, h.result("pq_kex"));
        assertNull(h.result("pq_kex_algorithms"));
        assertEquals(Boolean.TRUE, h.result("validated"), "kex_check is record-only");
        assertEquals(1, h.readyCount.get());
        assertEquals(0, h.closedCount.get());
    }

    @Test
    public void pqRequiredFailsWithoutPQ() {
        Harness h = new Harness(REQUIRED_JSON);
        h.feed(BANNER);
        h.feed(kexinit(CLASSIC_KEX));

        assertEquals(Boolean.FALSE, h.result("pq_kex"));
        assertEquals(Boolean.FALSE, h.result("validated"));
        assertNotNull(h.result("reason"), "the report must carry the failure reason");
        assertEquals(0, h.readyCount.get(), "READY must not fire on a failed required check");
        assertEquals(1, h.closedCount.get(), "a failed required check must tear the session down");
        assertTrue(h.closedPayload.get() instanceof IOException);
        assertTrue(h.sm.isClosed());
    }

    @Test
    public void pqRequiredPasses() {
        Harness h = new Harness(REQUIRED_JSON);
        h.feed(concat(SharedStringUtil.getBytes(BANNER), kexinit(PQ_KEX)));

        assertEquals(Boolean.TRUE, h.result("pq_kex"));
        assertEquals(Boolean.TRUE, h.result("validated"));
        assertEquals(1, h.readyCount.get());
        assertEquals(0, h.closedCount.get());
    }

    @Test
    public void kexinitSplitAcrossReads() {
        Harness h = new Harness(CHECK_JSON);
        byte[] whole = kexinit(PQ_KEX);
        // banner and the first half of the KEXINIT in one read (leftover drain path), rest after
        h.feed(concat(SharedStringUtil.getBytes(BANNER), Arrays.copyOfRange(whole, 0, 10)));
        assertNull(h.result("kex_algorithms"), "incomplete frame must wait");
        assertEquals(0, h.readyCount.get());

        h.feed(Arrays.copyOfRange(whole, 10, whole.length));
        assertEquals(Boolean.TRUE, h.result("pq_kex"));
        assertEquals(1, h.readyCount.get());
    }

    @Test
    public void preKexinitChatterSkipped() {
        Harness h = new Harness(CHECK_JSON);
        h.feed(BANNER);
        // SSH_MSG_IGNORE (2) frame ahead of the KEXINIT must be skipped, not break the parse
        h.feed(concat(packet(2, new byte[]{0, 0, 0, 0}), kexinit(PQ_KEX)));

        assertEquals(Boolean.TRUE, h.result("pq_kex"));
        assertEquals(1, h.readyCount.get());
    }

    @Test
    public void disconnectBeforeKexinitIsFatal() {
        Harness h = new Harness(CHECK_JSON);
        h.feed(BANNER);
        h.feed(packet(SSHKexState.SSH_MSG_DISCONNECT, new byte[]{0, 0, 0, 11}));

        assertEquals(1, h.closedCount.get());
        assertTrue(h.closedPayload.get() instanceof IOException);
        assertNull(h.result("kex_algorithms"));
    }

    @Test
    public void residueAfterKexinitReachesPostReadyOwner() {
        Harness h = new Harness(CHECK_JSON);
        h.feed(BANNER);
        h.feed(concat(kexinit(PQ_KEX), SharedStringUtil.getBytes("NEXTPKT")));

        assertEquals(1, h.readyCount.get());
        assertEquals("NEXTPKT", h.postReadyData.toString(),
                "bytes after the KEXINIT frame must reach the post-READY IN_DATA owner");
    }

    @Test
    public void closedBeforeKexinitFailsRequiredCheck() throws Exception {
        Harness h = new Harness(REQUIRED_JSON);
        h.feed(BANNER);
        // remote EOF before the KEXINIT (transport-level close, no channel in this harness)
        SharedIOUtil.close(h.callback);

        assertEquals(1, h.closedCount.get());
        assertEquals(Boolean.FALSE, h.result("validated"));
        assertEquals("session closed before SSH KEXINIT", h.result("reason"));
        assertTrue(h.sm.isClosed());
    }

    @Test
    public void identSentAndKexCapturedOverLoopback() throws Exception {
        // real sockets: with the kex check on, the probe identifies itself (send_ident default
        // true) — GitHub-style servers wait for the client line before sending their KEXINIT
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"ssh-kex-loopback\", \"protocol\": \"ssh\", \"close_on_ready\": true," +
                " \"ssh\": {\"kex_check\": true} }");
        TCPSMCallback callback = sm.newSessionCallback();

        java.net.ServerSocket server = new java.net.ServerSocket(0, 1,
                java.net.InetAddress.getLoopbackAddress());
        org.zoxweb.server.net.NIOSocket nioSocket = new org.zoxweb.server.net.NIOSocket(
                org.zoxweb.server.task.TaskUtil.defaultTaskProcessor(),
                org.zoxweb.server.task.TaskUtil.defaultTaskScheduler());
        java.net.Socket accepted = null;
        try {
            nioSocket.addClientSocket(new InetSocketAddress(java.net.InetAddress.getLoopbackAddress(),
                    server.getLocalPort()), callback, 5, null);
            accepted = server.accept();

            // the peer waits for the client identification line before proceeding to KEX
            java.io.InputStream in = accepted.getInputStream();
            StringBuilder ident = new StringBuilder();
            int c;
            while ((c = in.read()) != -1 && c != '\n')
                ident.append((char) c);
            assertTrue(ident.toString().startsWith("SSH-2.0-zoxweb_probe"),
                    "probe must identify itself: " + ident);

            accepted.getOutputStream().write(concat(SharedStringUtil.getBytes(BANNER), kexinit(PQ_KEX)));
            accepted.getOutputStream().flush();

            // close_on_ready: the machine captures the KEXINIT and ends the session itself
            assertTrue(SMProtoUtil.waitForClose(sm, 10000), "machine must close itself after the capture");
            assertEquals(Boolean.TRUE, SMProtoUtil.results(sm).getValue("pq_kex"));
            assertEquals(PQ_KEX, SMProtoUtil.results(sm).getValue("kex_algorithms"));
            assertNull(SMProtoUtil.closeCause(sm), "clean machine-driven close expected");
        } finally {
            SharedIOUtil.close(accepted, server, nioSocket);
        }
    }

    @Test
    public void pqAlgorithmsOverride() {
        Harness h = new Harness(
                "{ \"name\": \"ssh-kex-test\", \"protocol\": \"ssh\", \"ssh\": {\"kex_check\": true," +
                " \"send_ident\": false, \"pq_algorithms\": \"my-pq-kex@example.com\"} }");
        h.feed(BANNER);
        h.feed(kexinit("curve25519-sha256,my-pq-kex@example.com"));

        assertEquals(Boolean.TRUE, h.result("pq_kex"));
        assertEquals("my-pq-kex@example.com", h.result("pq_kex_algorithms"));
    }
}
