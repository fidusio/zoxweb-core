package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.util.SUS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Connection phase that validates the peer's SSH identification line (RFC 4253 §4.2) after
 * connect: accumulates {@link ClientEvent#IN_DATA} until the {@code SSH-} line arrives
 * (skipping optional pre-banner lines, bounded), validates it against the configured
 * expectations, then publishes {@link ClientEvent#BANNER_RECEIVED} with the banner string and
 * completes the phase (→ {@link ClientEvent#READY}). Any bytes following the banner line
 * (start of key exchange — legitimate for SSH) are republished as a fresh {@code IN_DATA}
 * after {@code READY}, so an application consumer registered from its {@code READY} handler
 * receives them.
 * <p>
 * Validation failure, an oversize identification line, or breach of the pre-banner cap is
 * fatal: the session fails with an {@link IOException} and teardown publishes {@code CLOSED}
 * with it.
 * <p>
 * One phase instance per machine — the accumulation state lives in the contributed consumer.
 */
public class SSHBannerPhase implements ConnectionPhase {

    public static final String NAME = "ssh-banner";
    /** RFC 4253 §4.2: identification line at most 255 bytes including CRLF. */
    public static final int DEFAULT_MAX_LINE = 255;
    /** Bound on skippable pre-banner data before the SSH- line must appear. */
    public static final int DEFAULT_PRE_BANNER_CAP = 4096;
    private static final String IDENT_LEAD = "SSH-";

    private final String bannerPrefix;
    private final String bannerContains;
    private final String bannerExact;
    private final int maxLine;
    private final int preBannerCap;

    public SSHBannerPhase() {
        this("SSH-2.0-", null);
    }

    public SSHBannerPhase(String bannerPrefix, String bannerContains) {
        this(bannerPrefix, bannerContains, null, DEFAULT_MAX_LINE, DEFAULT_PRE_BANNER_CAP);
    }

    /**
     * @param bannerPrefix   required identification-line prefix (e.g. {@code "SSH-2.0-"})
     * @param bannerContains optional substring the banner must contain, null to skip
     * @param bannerExact    optional exact banner match, null to skip
     * @param maxLine        maximum identification-line length in bytes including CRLF
     * @param preBannerCap   maximum bytes of skippable pre-banner lines
     */
    public SSHBannerPhase(String bannerPrefix, String bannerContains, String bannerExact,
                          int maxLine, int preBannerCap) {
        SUS.checkIfNull("bannerPrefix null", bannerPrefix);
        this.bannerPrefix = bannerPrefix;
        this.bannerContains = bannerContains;
        this.bannerExact = bannerExact;
        this.maxLine = maxLine;
        this.preBannerCap = preBannerCap;
    }

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
        state.register(new BannerConsumer());
        sm.register(state);
    }

    private class BannerConsumer extends TriggerConsumer<ByteBuffer> {

        private final ByteArrayOutputStream line = new ByteArrayOutputStream(64);
        private boolean done = false;
        private boolean sawCR = false;
        private int preBannerBytes = 0;

        BannerConsumer() {
            super(ClientEvent.IN_DATA);
        }

        @Override
        public void accept(ByteBuffer bb) {
            if (done)
                return; // another owner has the buffer now, do not touch it

            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            try {
                while (bb.hasRemaining() && !done) {
                    byte b = bb.get();
                    if (b == '\n') {
                        if (!processLine(ctx, bb))
                            return; // fatal — session already failed
                    } else if (b == '\r') {
                        sawCR = true; // stripped; tolerate bare LF too
                    } else {
                        if (sawCR) {
                            // CR not followed by LF — treat as line content
                            line.write('\r');
                            sawCR = false;
                        }
                        line.write(b);
                        if (line.size() + 2 > maxLine) {
                            ctx.fail(new IOException("SSH identification line exceeds " + maxLine + " bytes"));
                            return;
                        }
                    }
                }
            } finally {
                ByteBufferUtil.cache(bb);
            }
        }

        /**
         * Handles one complete line; returns false on fatal failure. On banner success,
         * publishes BANNER_RECEIVED, completes the phase (→ READY), then republishes any bytes
         * remaining in the current packet as a fresh IN_DATA for the post-READY owner.
         */
        private boolean processLine(ClientSessionContext ctx, ByteBuffer bb) {
            sawCR = false;
            String text = new String(line.toByteArray(), StandardCharsets.US_ASCII);
            line.reset();

            if (text.startsWith(IDENT_LEAD)) {
                if (!text.startsWith(bannerPrefix)) {
                    ctx.fail(new IOException("SSH banner prefix mismatch: " + text));
                    return false;
                }
                if (bannerExact != null && !text.equals(bannerExact)) {
                    ctx.fail(new IOException("SSH banner mismatch: " + text));
                    return false;
                }
                if (bannerContains != null && !text.contains(bannerContains)) {
                    ctx.fail(new IOException("SSH banner does not contain '" + bannerContains + "': " + text));
                    return false;
                }
                done = true;
                SMProtoUtil.results(ctx.getStateMachine()).build("banner", text);
                publishSync(ClientEvent.BANNER_RECEIVED, text);
                ctx.phaseComplete(NAME);
                // a BANNER_RECEIVED/READY consumer may have failed the session inline —
                // publishing on the closed machine would throw instead of closing cleanly
                if (bb.hasRemaining() && !ctx.getStateMachine().isClosed()) {
                    // start of key exchange — hand it to the post-READY owner as a fresh packet
                    byte[] rest = new byte[bb.remaining()];
                    bb.get(rest);
                    publishSync(ClientEvent.IN_DATA,
                            ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, rest, 0, rest.length, true));
                }
                return true;
            }

            // pre-banner line — skip, bounded
            preBannerBytes += text.length() + 2;
            if (preBannerBytes > preBannerCap) {
                ctx.fail(new IOException("SSH pre-banner data exceeds " + preBannerCap + " bytes"));
                return false;
            }
            return true;
        }
    }
}
