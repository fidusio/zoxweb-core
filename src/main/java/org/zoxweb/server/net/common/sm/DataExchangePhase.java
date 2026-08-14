package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.util.NVPair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A fixed scripted send/expect dialogue over the (plaintext or upgraded) link, built from the
 * {@code exchange} array of the JSON config. Each step is one of:
 * <ul>
 * <li>{@code send} — write the decoded {@link SMProtoUtil} literal to the peer, then continue;</li>
 * <li>{@code expect} — wait until the accumulated incoming bytes <b>contain</b> the decoded sequence
 * (substring match, not a full-line or exact match), consume through the match, then continue;</li>
 * <li>{@code start_tls} — publish {@link ClientEvent#START_TLS} to upgrade to TLS (needs an
 * {@code on_demand} TLS phase); the accumulation buffer must be empty at that point — any residue
 * past the last matched {@code expect} is attacker-controllable plaintext and is treated as a fatal
 * injection (STARTTLS injection class).</li>
 * </ul>
 * Steps are validated and their data literals decoded at construction — a malformed literal or an
 * unknown op fails fast with {@link IllegalArgumentException} at build time, never mid-session.
 * <p>
 * When the machine also has an {@code IMMEDIATE} TLS phase, the dialogue starts after
 * {@link ClientEvent#SECURE} (the transport is upgrading when {@code CONNECTED} reaches this
 * phase), so every {@code send} goes out encrypted.
 * <p>
 * Completing the last step reports the phase done and fires {@link ClientEvent#READY}; any bytes left
 * over after the final step are republished as a fresh {@code IN_DATA} for the post-{@code READY}
 * owner. A mismatch cannot be detected directly (an {@code expect} only ever waits) — a wrong reply
 * surfaces as the connection closing with the {@code expect} still pending, i.e. {@code CLOSED} with
 * a cause; an oversized non-matching accumulation ({@link #maxAccumulation}) fails the session.
 * <p>
 * This is a linear script, not a program: it cannot branch on a reply, parse fields out of one, or
 * compute a value to send. Those need a custom phase in code.
 */
public class DataExchangePhase implements ConnectionPhase {

    public static final String NAME = "data-exchange";
    /** Bound on unmatched accumulation before an {@code expect} fails the session. */
    public static final int DEFAULT_MAX_ACCUMULATION = 65536;

    static final String OP_SEND = "send";
    static final String OP_EXPECT = "expect";
    static final String OP_START_TLS = "start_tls";

    private static final byte[] EMPTY = new byte[0];

    /**
     * One validated step. A static literal is decoded once at build time ({@code data} set); a
     * literal carrying {@code ${var}} placeholders keeps its raw form ({@code literal} set) and is
     * resolved + decoded at send/expect time against the session's variable bag.
     */
    private static final class Step {
        final String op;
        final byte[] data;     // non-null for a static (build-time decoded) literal
        final String literal;  // non-null for a dynamic ${var}-bearing literal

        Step(String op, byte[] data, String literal) {
            this.op = op;
            this.data = data;
            this.literal = literal;
        }
    }

    private final List<Step> steps;
    private final int maxAccumulation;

    /**
     * @param steps ordered dialogue steps (each pair: name = {@code send}/{@code expect}/
     *              {@code start_tls}, value = the {@link SMProtoUtil} literal for send/expect)
     * @throws IllegalArgumentException on an unknown op or a literal that fails to decode
     */
    public DataExchangePhase(List<NVPair> steps) {
        this(steps, DEFAULT_MAX_ACCUMULATION);
    }

    /**
     * @throws IllegalArgumentException on an unknown op or a literal that fails to decode
     */
    public DataExchangePhase(List<NVPair> steps, int maxAccumulation) {
        this.steps = compile(steps);
        this.maxAccumulation = maxAccumulation;
    }

    /**
     * Validates ops and decodes every literal once — the fail-fast half of the factory contract:
     * a bad script must never surface as a mid-session wedge.
     */
    private static List<Step> compile(List<NVPair> raw) {
        List<Step> ret = new ArrayList<Step>(raw.size());
        for (NVPair step : raw) {
            String op = step.getName();
            if (!OP_SEND.equals(op) && !OP_EXPECT.equals(op) && !OP_START_TLS.equals(op))
                throw new IllegalArgumentException("unknown exchange step: " + op);
            if (OP_START_TLS.equals(op)) {
                ret.add(new Step(op, EMPTY, null));
            } else if (SMProtoUtil.hasVars(step.getValue())) {
                // dynamic: bytes depend on the caller's variable bag — decode deferred to run time
                ret.add(new Step(op, null, step.getValue()));
            } else {
                // static: decode once now (fail-fast on a malformed literal)
                ret.add(new Step(op, SMProtoUtil.STRING_TO_DATA.decode(step.getValue()), null));
            }
        }
        return ret;
    }

    /**
     * @return true if the script contains a {@code start_tls} step (used by the factory to
     * validate that an {@code on_demand} TLS phase is present)
     */
    public boolean hasStartTLS() {
        for (Step s : steps) {
            if (OP_START_TLS.equals(s.op))
                return true;
        }
        return false;
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
    public void contribute(ClientConnectionSM sm) {
        State<Object> state = new State<Object>(NAME);
        Driver driver = new Driver();
        state.register(driver.connected);
        state.register(driver.inData);
        state.register(driver.secure);
        sm.register(state);
    }

    /**
     * Holds the per-session dialogue cursor and accumulation, shared by the three consumers.
     */
    private class Driver {
        private int index = 0;
        private boolean done = false;
        private boolean waitingForSecure = false;
        private final ByteArrayOutputStream acc = new ByteArrayOutputStream(64);

        final TriggerConsumer<SelectionKey> connected = new TriggerConsumer<SelectionKey>(TCPSMCallback.BasicEvent.CONNECTED) {
            @Override
            public void accept(SelectionKey key) {
                ClientSessionContext ctx = ctx();
                if (ctx.getMode() == ClientSessionContext.Mode.TLS_HANDSHAKING) {
                    // IMMEDIATE TLS phase upgrading (its AutoStart ran earlier in this
                    // broadcast): the dialogue starts on SECURE so sends go out encrypted
                    waitingForSecure = true;
                    return;
                }
                pump(ctx);
            }
        };

        final TriggerConsumer<Object> secure = new TriggerConsumer<Object>(ClientEvent.SECURE) {
            @Override
            public void accept(Object sci) {
                if (waitingForSecure) {
                    waitingForSecure = false;
                    pump(ctx());
                }
            }
        };

        final TriggerConsumer<ByteBuffer> inData = new TriggerConsumer<ByteBuffer>(ClientEvent.IN_DATA) {
            @Override
            public void accept(ByteBuffer bb) {
                if (done)
                    return; // dialogue finished; the post-READY owner owns the buffer now
                ClientSessionContext ctx = ctx();
                byte[] chunk = new byte[bb.remaining()];
                bb.get(chunk);
                ByteBufferUtil.cache(bb);
                acc.write(chunk, 0, chunk.length);
                if (acc.size() > maxAccumulation) {
                    ctx.fail(new IOException("exchange accumulation exceeded " + maxAccumulation + " bytes without a match"));
                    return;
                }
                matchAndPump(ctx);
            }
        };

        private ClientSessionContext ctx() {
            return (ClientSessionContext) connected.getStateMachine().getConfig();
        }

        /**
         * Bytes for a step: the build-time decode for a static literal, or a run-time
         * variable-resolved decode for a {@code ${var}}-bearing one.
         *
         * @throws IllegalArgumentException on an unresolved variable or a body that fails to decode
         */
        private byte[] data(Step step, ClientSessionContext ctx) {
            return step.data != null ? step.data : SMProtoUtil.STRING_VARS_TO_DATA.decode(step.literal, ctx.getVars());
        }

        /**
         * Runs forward through {@code send} / {@code start_tls} steps until it must wait (an
         * {@code expect}, or a {@code start_tls} handshake) or the script completes.
         */
        private void pump(ClientSessionContext ctx) {
            while (!done && index < steps.size()) {
                Step step = steps.get(index);
                if (OP_EXPECT.equals(step.op)) {
                    return; // wait for IN_DATA
                } else if (OP_SEND.equals(step.op)) {
                    try {
                        ctx.write(ByteBuffer.wrap(data(step, ctx)));
                    } catch (IOException | IllegalArgumentException e) {
                        ctx.fail(e);
                        return;
                    }
                    index++;
                } else { // OP_START_TLS — the only remaining op after compile()
                    if (acc.size() > 0) {
                        ctx.fail(new IOException("STARTTLS injection: " + acc.size() + " byte(s) of residue after the last expect"));
                        return;
                    }
                    if (ctx.getMode() != ClientSessionContext.Mode.PLAIN) {
                        ctx.fail(new IOException("start_tls: transport is not plaintext (mode " + ctx.getMode() + ")"));
                        return;
                    }
                    index++;
                    waitingForSecure = true;
                    connected.publishSync(ClientEvent.START_TLS, null);
                    if (ctx.getMode() == ClientSessionContext.Mode.PLAIN) {
                        // no ON_DEMAND TLS phase consumed START_TLS — fail instead of a
                        // silent forever-hang waiting for a SECURE that cannot come
                        ctx.fail(new IOException("start_tls: no ON_DEMAND TLS phase on this machine"));
                    }
                    return; // resume on SECURE
                }
            }
            if (!done && index >= steps.size())
                complete(ctx);
        }

        /**
         * Satisfies as many consecutive {@code expect} steps as the accumulation allows, consuming
         * through each match, then resumes {@link #pump}.
         */
        private void matchAndPump(ClientSessionContext ctx) {
            while (!done && index < steps.size() && OP_EXPECT.equals(steps.get(index).op)) {
                byte[] want;
                try {
                    want = data(steps.get(index), ctx);
                } catch (IllegalArgumentException e) {
                    ctx.fail(e);
                    return;
                }
                byte[] have = acc.toByteArray();
                int at = indexOf(have, want);
                if (at < 0)
                    return; // need more bytes
                // consume through the match; keep the remainder
                int consumed = at + want.length;
                acc.reset();
                if (consumed < have.length)
                    acc.write(have, consumed, have.length - consumed);
                index++;
            }
            pump(ctx);
        }

        private void complete(ClientSessionContext ctx) {
            done = true;
            byte[] leftover = acc.toByteArray();
            acc.reset();
            ctx.phaseComplete(NAME);
            // machine may have closed inside the READY dispatch — publishing on it would throw
            if (leftover.length > 0 && !ctx.getStateMachine().isClosed())
                connected.publishSync(ClientEvent.IN_DATA,
                        ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, leftover, 0, leftover.length, true));
        }
    }

    /**
     * @return the first index of {@code needle} in {@code haystack}, or -1; an empty needle matches at 0
     */
    static int indexOf(byte[] haystack, byte[] needle) {
        if (needle.length == 0)
            return 0;
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j])
                    continue outer;
            }
            return i;
        }
        return -1;
    }

    /**
     * Reads the {@code exchange} steps from the config's {@code NVPairList} (or null if absent/empty).
     */
    static List<NVPair> stepsFrom(org.zoxweb.shared.util.NVGenericMap cfg) {
        Object nv = cfg.getNV("exchange");
        if (!(nv instanceof org.zoxweb.shared.util.NVPairList))
            return null;
        NVPair[] all = ((org.zoxweb.shared.util.NVPairList) nv).values();
        return all != null && all.length > 0 ? Arrays.asList(all) : null;
    }
}
