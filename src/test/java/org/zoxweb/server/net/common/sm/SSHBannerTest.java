package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOBuffers;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the SSH banner flow — the {@code protocol: "ssh"} catalog sugar (delimited assembler +
 * validating controller) — without sockets: CONNECTED and RAW_IN_DATA are published directly
 * (the transport router passes wire bytes through as IN_DATA in PLAIN mode), and the fail paths
 * use an unconnected TCPSMCallback whose teardown publishes CLOSED without a channel.
 */
public class SSHBannerTest {

    private static final String DEFAULT_JSON = "{ \"name\": \"ssh-test\", \"protocol\": \"ssh\" }";
    private static final String OPENSSH_JSON =
            "{ \"name\": \"ssh-test\", \"protocol\": \"ssh\", \"ssh\": {\"banner_contains\": \"OpenSSH\"} }";

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
                    postReadyData.append(new String(chunk));
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

        void feed(String wire) {
            byte[] bytes = SharedStringUtil.getBytes(wire);
            // IN_RAW_DATA carries a DataPacket wrapping the borrowed pair (the router copies,
            // never recaches); no channel here, and the peer address is a placeholder
            sm.publishSync(CommonTrigger.IN_RAW_DATA, new DataPacket<Long>(
                    packetCounter.incrementAndGet(), null,
                    InetSocketAddress.createUnresolved("test.local", 22),
                    new IOBuffers().setInBuffer(ByteBufferUtil.allocateByteBuffer(
                            ByteBufferUtil.BufferType.HEAP, bytes, 0, bytes.length, true))));
        }

        /** The validated banner is read from the report ({@code results.banner}). */
        String banner() {
            return SMProtoUtil.results(sm).getValue("banner");
        }
    }

    @Test
    public void bannerSinglePacket() {
        Harness h = new Harness(DEFAULT_JSON);
        h.feed("SSH-2.0-OpenSSH_9.6\r\n");

        assertEquals("SSH-2.0-OpenSSH_9.6", h.banner());
        assertEquals(Boolean.TRUE, SMProtoUtil.results(h.sm).getValue("validated"));
        assertEquals(1, h.readyCount.get(), "READY must be published once");
        assertEquals(0, h.closedCount.get());
    }

    @Test
    public void bannerSplitAcrossPackets() {
        Harness h = new Harness(DEFAULT_JSON);
        h.feed("SSH-2.0-Op");
        assertNull(h.banner(), "incomplete banner must not publish");
        h.feed("enSSH_9.6");
        h.feed("\r\n");

        assertEquals("SSH-2.0-OpenSSH_9.6", h.banner());
        assertEquals(1, h.readyCount.get());
    }

    @Test
    public void bareLineFeedTolerated() {
        Harness h = new Harness(DEFAULT_JSON);
        h.feed("SSH-2.0-Whatever\n");

        assertEquals("SSH-2.0-Whatever", h.banner());
        assertEquals(1, h.readyCount.get());
    }

    @Test
    public void preBannerLinesSkipped() {
        Harness h = new Harness(DEFAULT_JSON);
        h.feed("welcome to the jungle\r\nplease behave\r\nSSH-2.0-OpenSSH_9.6\r\n");

        assertEquals("SSH-2.0-OpenSSH_9.6", h.banner());
        assertEquals(1, h.readyCount.get());
        assertEquals(0, h.closedCount.get());
    }

    @Test
    public void bannerContainsMismatchIsFatal() {
        Harness h = new Harness(OPENSSH_JSON);
        h.feed("SSH-2.0-Dropbear\r\n");

        assertNull(h.banner());
        assertEquals(Boolean.FALSE, SMProtoUtil.results(h.sm).getValue("validated"),
                "the report must carry the false verdict");
        assertNotNull(SMProtoUtil.results(h.sm).getValue("reason"), "the report must carry the mismatch reason");
        assertEquals(0, h.readyCount.get(), "READY must not fire on validation failure");
        assertEquals(1, h.closedCount.get(), "fatal validation must tear the session down");
        assertTrue(h.closedPayload.get() instanceof IOException,
                "CLOSED payload must be the IOException cause");
        assertTrue(h.sm.isClosed(), "machine must be closed by teardown");
    }

    @Test
    public void oversizeIdentificationLineIsFatal() {
        Harness h = new Harness(DEFAULT_JSON);
        StringBuilder big = new StringBuilder("SSH-2.0-");
        for (int i = 0; i < 300; i++)
            big.append('A');
        h.feed(big.toString());

        assertEquals(1, h.closedCount.get(), "oversize line must tear the session down");
        assertTrue(h.closedPayload.get() instanceof IOException);
        assertEquals(0, h.readyCount.get());
    }

    @Test
    public void leftoverAfterBannerRepublishedPostReady() {
        Harness h = new Harness(DEFAULT_JSON);
        h.feed("SSH-2.0-OpenSSH_9.6\r\nKEXDATA");

        assertEquals("SSH-2.0-OpenSSH_9.6", h.banner());
        assertEquals(1, h.readyCount.get());
        assertEquals("KEXDATA", h.postReadyData.toString(),
                "bytes after the banner line must reach the post-READY IN_DATA owner");
    }

    /**
     * Command-line runner: dials a real SSH endpoint and prints the banner + KEXINIT probe
     * lifecycle via {@link ProtoConnect#run} — the report carries {@code kex_algorithms} and
     * the {@code pq_kex} post-quantum readiness verdict.
     * <pre>
     *   SSHBannerTest host [port]     (port defaults to 22)
     * </pre>
     * The config sets {@code close_on_ready} so the machine records the banner, captures the
     * KEXINIT and closes the session itself — this runner, like every application, only
     * observes. Exit code: 0 clean close, 1 closed with a cause, 2 timed out, 64 usage error.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("usage: SSHBannerTest host [port]");
            System.exit(64);
            return;
        }
        String host = args[0];
        int port = args.length == 2 ? Integer.parseInt(args[1]) : 22;

        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"ssh-banner\", \"protocol\": \"ssh\", \"close_on_ready\": true," +
                " \"ssh\": {\"kex_check\": true} }");
        int timeoutSec = ClientSMFactory.timeoutSec(sm.getContext().getSettings());

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        int exit;
        try {
            exit = ProtoConnect.run(new InetSocketAddress(host, port), sm, nioSocket,
                    timeoutSec, timeoutSec + 30);
        } finally {
            SharedIOUtil.close(nioSocket);
            TaskUtil.close();
        }
        System.exit(exit);
    }
}
