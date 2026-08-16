package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.NVPairList;
import org.zoxweb.shared.util.NamedValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Catalog state {@code controller} (META-SM-PROTO-DESIGN.md §8): drives the linear
 * {@code exchange} step list against the message seams. It consumes
 * {@link ClientEvent#IN_MESSAGE} (plus {@code CONNECTED} to start and {@code SECURE} to resume
 * after an upgrade) and publishes {@link ClientEvent#OUT_MESSAGE} (→ responder),
 * {@link ClientEvent#VALIDATE} (→ validator) and {@link ClientEvent#START_TLS} (→ ssl state).
 * <p>
 * <b>Configuration is the state's properties bag</b>:
 * <ul>
 * <li>{@code exchange} — the step list: {@code send} / {@code expect} ({@link SMProtoUtil}
 * literals, {@code ${var}}s resolved at run time), {@code validate}
 * ({@code {prefix, contains, exact, report}} meta), {@code start_tls}. Compiled fail-fast at
 * construction — unknown op or malformed literal never surfaces mid-session.</li>
 * <li>{@code max_skip} — framed boundaries: total bytes of skipped non-matching messages
 * before an {@code expect} fails the session (default 65536).</li>
 * </ul>
 * Working memory (cursor, skip counter, inbox, current message, await-secure flag) lives in
 * the state's bag. The {@code ready_gate} bag flag marks this state as gating
 * {@link ClientEvent#READY} — the completion rule: the step list finishing is the pipeline
 * done. A run that completes with no {@code validate} step records
 * {@code validated=true, reason="script completed"} — every run yields a verdict.
 * <p>
 * <b>Hard limit preserved:</b> a linear script — no branching, no field parsing, no computing
 * a value from a reply. A richer controller is a future catalog state, not a growth of this
 * grammar.
 * <p>
 * Boundary interplay: on framed boundaries ({@code datagram}/{@code delimited}/
 * {@code length_prefixed}) an {@code expect} is a per-message contains-match — a non-matching
 * complete message is skipped (the SMTP {@code 250-} continuation idiom), bounded by
 * {@code max_skip}. On {@code stream} the message is the accumulation snapshot and the
 * controller consumes through the match via the {@link MessageAssemblerState.Assembly}
 * blackboard holder — byte-for-byte the v1 semantics. The {@code start_tls} residue check is
 * "assembler accumulation must be empty": unexpected plaintext before the upgrade fails the
 * session (STARTTLS injection mitigation — never clear-and-continue).
 */
public class ProtocolControllerState extends State<Object> {

    public static final String NAME = "controller";
    public static final int DEFAULT_MAX_SKIP = 65536;

    static final String OP_SEND = "send";
    static final String OP_EXPECT = "expect";
    static final String OP_VALIDATE = "validate";
    static final String OP_START_TLS = "start_tls";

    private static final byte[] EMPTY = new byte[0];

    // working-memory keys in the state bag
    private static final String STEPS = "steps";
    private static final String CURSOR = "cursor";
    private static final String SKIPPED = "skipped";
    private static final String DONE = "done";
    private static final String AWAIT_SECURE = "await_secure";
    private static final String INBOX = "inbox";
    private static final String CURRENT = "current";

    /**
     * One compiled step: a static literal is decoded at build time ({@code data} set); a
     * {@code ${var}}-bearing literal keeps its raw form ({@code literal}) and resolves at run
     * time; a {@code validate} step carries its match meta.
     */
    static final class Step {
        final String op;
        final byte[] data;
        final String literal;
        final NVGenericMap meta;

        Step(String op, byte[] data, String literal, NVGenericMap meta) {
            this.op = op;
            this.data = data;
            this.literal = literal;
            this.meta = meta;
        }
    }

    public ProtocolControllerState() {
        this(null);
    }

    /**
     * @param config the {@code config} block seeded into this state's properties bag
     *               (null = empty script: the pipeline completes on {@code CONNECTED})
     * @throws IllegalArgumentException on an unknown op or a literal that fails to decode
     */
    public ProtocolControllerState(NVGenericMap config) {
        super(NAME);
        SMProtoUtil.seed(this, config);
        NVGenericMap bag = getProperties();
        bag.add(new NVBoolean("ready_gate", true));
        bag.add(new NamedValue<List<Step>>(STEPS, compile(bag.getNV("exchange"))));
        bag.add(new NamedValue<byte[]>(INBOX, null));
        bag.add(new NamedValue<byte[]>(CURRENT, null));
        bag.build(new NVBoolean(DONE, false)).build(new NVBoolean(AWAIT_SECURE, false));
        bag.add(new NamedValue<int[]>(CURSOR, new int[]{0}));
        bag.add(new NamedValue<int[]>(SKIPPED, new int[]{0}));
        register(new Connected());
        register(new Secured());
        register(new InMessage());
    }

    /**
     * @return true if the compiled script contains a {@code start_tls} step (build-time
     * validation seam: it requires an ON_DEMAND ssl state on the machine)
     */
    public boolean hasStartTLS() {
        for (Step s : steps()) {
            if (OP_START_TLS.equals(s.op))
                return true;
        }
        return false;
    }

    /**
     * Compiles the {@code exchange} entry — either the JSON-parsed {@code NVPairList} (a
     * {@code validate} step's meta arrives as its JSON text and is re-parsed here) or a
     * programmatic {@code NamedValue<List<GetNameValue<?>>>} whose values are literals
     * (send/expect) or {@code NVGenericMap} meta (validate). Fail-fast on unknown ops and
     * malformed literals.
     */
    private static List<Step> compile(Object exchange) {
        List<Step> ret = new ArrayList<Step>();
        List<GetNameValue<?>> raw = new ArrayList<GetNameValue<?>>();
        if (exchange instanceof NVPairList) {
            NVPair[] all = ((NVPairList) exchange).values();
            if (all != null)
                raw.addAll(Arrays.asList(all));
        } else if (exchange instanceof NamedValue && ((NamedValue<?>) exchange).getValue() instanceof List) {
            for (Object o : (List<?>) ((NamedValue<?>) exchange).getValue())
                raw.add((GetNameValue<?>) o);
        } else if (exchange != null) {
            throw new IllegalArgumentException("unsupported exchange structure: " + exchange.getClass());
        }
        for (GetNameValue<?> step : raw) {
            String op = step.getName();
            Object value = step.getValue();
            if (OP_START_TLS.equals(op)) {
                ret.add(new Step(op, EMPTY, null, null));
            } else if (OP_VALIDATE.equals(op)) {
                NVGenericMap meta;
                if (value instanceof NVGenericMap)
                    meta = (NVGenericMap) value;
                else if (value instanceof String)
                    meta = GSONUtil.fromJSONDefault((String) value, NVGenericMap.class);
                else
                    throw new IllegalArgumentException("validate step without match meta");
                ret.add(new Step(op, null, null, meta));
            } else if (OP_SEND.equals(op) || OP_EXPECT.equals(op)) {
                String literal = (String) value;
                if (SMProtoUtil.hasVars(literal))
                    ret.add(new Step(op, null, literal, null)); // run-time var resolution
                else
                    ret.add(new Step(op, SMProtoUtil.STRING_TO_DATA.decode(literal), null, null));
            } else {
                throw new IllegalArgumentException("unknown exchange step: " + op);
            }
        }
        return ret;
    }

    // ---- working-memory accessors (all state lives in the bag) ----

    @SuppressWarnings("unchecked")
    private List<Step> steps() {
        return (List<Step>) ((NamedValue<?>) getProperties().getNV(STEPS)).getValue();
    }

    private int[] counter(String name) {
        return (int[]) ((NamedValue<?>) getProperties().getNV(name)).getValue();
    }

    private boolean flag(String name) {
        return Boolean.TRUE.equals(getProperties().getValue(name));
    }

    private void flag(String name, boolean value) {
        ((NVBoolean) getProperties().getNV(name)).setValue(value);
    }

    @SuppressWarnings("unchecked")
    private byte[] bytes(String name) {
        return ((NamedValue<byte[]>) getProperties().getNV(name)).getValue();
    }

    @SuppressWarnings("unchecked")
    private void bytes(String name, byte[] value) {
        ((NamedValue<byte[]>) getProperties().getNV(name)).setValue(value);
    }

    private ClientSessionContext ctx() {
        return (ClientSessionContext) getStateMachine().getConfig();
    }

    private void publish(ClientEvent canID, Object data) {
        getStateMachine().publishSync(canID, data);
    }

    /** Step bytes: build-time decode, or run-time variable-resolved decode. */
    private byte[] data(Step step, ClientSessionContext ctx) {
        return step.data != null ? step.data : SMProtoUtil.STRING_VARS_TO_DATA.decode(step.literal, ctx.getVars());
    }

    // ---- the driver ----

    /**
     * Runs forward through the steps until one must wait (an {@code expect}/{@code validate}
     * without a message, or a {@code start_tls} handshake) or the script completes. All
     * follow-up work happens by publishing events — responder, validator and ssl state react
     * in the same synchronous broadcast.
     */
    private void pump(ClientSessionContext ctx) {
        List<Step> steps = steps();
        int[] cursor = counter(CURSOR);
        while (!flag(DONE) && cursor[0] < steps.size() && !ctx.getStateMachine().isClosed()) {
            Step step = steps.get(cursor[0]);
            if (OP_SEND.equals(step.op)) {
                byte[] out;
                try {
                    out = data(step, ctx);
                } catch (IllegalArgumentException e) {
                    ctx.fail(e);
                    return;
                }
                publish(ClientEvent.OUT_MESSAGE, out); // the responder writes; a failure closes the machine
                cursor[0]++;
            } else if (OP_EXPECT.equals(step.op)) {
                if (!expect(ctx, step))
                    return; // wait for the next IN_MESSAGE
                cursor[0]++;
            } else if (OP_VALIDATE.equals(step.op)) {
                byte[] message = bytes(CURRENT);
                if (message == null) {
                    MessageAssemblerState.Assembly asm = SMProtoUtil.assembly(ctx.getStateMachine());
                    if (asm != null && asm.getBoundary() == MessageAssemblerState.Boundary.STREAM) {
                        if (asm.pending() == 0)
                            return; // wait for data
                        message = asm.snapshot(); // validate reads, never consumes, the stream
                    } else {
                        message = takeInbox();
                        if (message == null)
                            return; // wait for the next IN_MESSAGE
                    }
                    bytes(CURRENT, message);
                }
                publish(ClientEvent.VALIDATE,
                        new ProtocolTypeValidatorState.Validation(message, step.meta));
                if (ctx.getStateMachine().isClosed())
                    return; // verdict was false — the validator failed the session
                cursor[0]++;
            } else { // OP_START_TLS
                MessageAssemblerState.Assembly asm = SMProtoUtil.assembly(ctx.getStateMachine());
                int residue = (asm != null ? asm.pending() : 0) + (bytes(INBOX) != null ? bytes(INBOX).length : 0);
                if (residue > 0) {
                    ctx.fail(new IOException("STARTTLS injection: " + residue + " byte(s) of residue after the last expect"));
                    return;
                }
                if (ctx.getMode() != ClientSessionContext.Mode.PLAIN) {
                    ctx.fail(new IOException("start_tls: transport is not plaintext (mode " + ctx.getMode() + ")"));
                    return;
                }
                cursor[0]++;
                flag(AWAIT_SECURE, true);
                publish(ClientEvent.START_TLS, null);
                if (ctx.getMode() == ClientSessionContext.Mode.PLAIN && !ctx.getStateMachine().isClosed()) {
                    // no ON_DEMAND ssl state consumed START_TLS — fail instead of a silent
                    // forever-hang waiting for a SECURE that cannot come
                    ctx.fail(new IOException("start_tls: no ON_DEMAND ssl state on this machine"));
                }
                return; // resume on SECURE
            }
        }
        if (!flag(DONE) && cursor[0] >= steps.size())
            complete(ctx);
    }

    /**
     * One {@code expect} attempt against the available input; true = matched (cursor may
     * advance), false = must wait for more input.
     */
    private boolean expect(ClientSessionContext ctx, Step step) {
        byte[] want;
        try {
            want = data(step, ctx);
        } catch (IllegalArgumentException e) {
            ctx.fail(e);
            return false;
        }
        MessageAssemblerState.Assembly asm = SMProtoUtil.assembly(ctx.getStateMachine());
        if (asm != null && asm.getBoundary() == MessageAssemblerState.Boundary.STREAM) {
            // stream: contains-match on the unconsumed accumulation, consume through the match
            byte[] have = asm.snapshot();
            int at = SMProtoUtil.indexOf(have, want);
            if (at < 0)
                return false;
            asm.consume(at + want.length);
            bytes(CURRENT, Arrays.copyOfRange(have, 0, at + want.length));
            return true;
        }
        // framed: per-message contains-match; a non-matching complete message is skipped,
        // bounded by max_skip
        byte[] message = takeInbox();
        while (message != null) {
            if (SMProtoUtil.indexOf(message, want) >= 0) {
                bytes(CURRENT, message);
                return true;
            }
            int[] skipped = counter(SKIPPED);
            skipped[0] += message.length;
            if (skipped[0] > SMProtoUtil.intValue(getProperties(), "max_skip", DEFAULT_MAX_SKIP)) {
                ctx.fail(new IOException("expect skipped more than max_skip bytes without a match"));
                return false;
            }
            message = takeInbox();
        }
        return false;
    }

    private byte[] takeInbox() {
        byte[] message = bytes(INBOX);
        if (message != null)
            bytes(INBOX, null);
        return message;
    }

    /**
     * The completion rule: the step list finishing = pipeline done → the READY gate. Records
     * the default verdict when no {@code validate} ran, finishes the assembly (the assembler
     * goes dormant), completes the gate (→ {@code READY}), then drains any leftover
     * accumulation as a fresh {@code IN_DATA} for the post-{@code READY} owner — after the
     * READY broadcast, so a consumer registered from a READY handler receives it.
     */
    private void complete(ClientSessionContext ctx) {
        flag(DONE, true);
        NVGenericMap results = SMProtoUtil.results(ctx.getStateMachine());
        if (results.getNV("validated") == null)
            results.build(new NVBoolean("validated", true)).build("reason", "script completed");
        MessageAssemblerState.Assembly asm = SMProtoUtil.assembly(ctx.getStateMachine());
        if (asm != null)
            asm.finish();
        ctx.gateComplete(NAME);
        if (asm != null && asm.pending() > 0 && !ctx.getStateMachine().isClosed()) {
            byte[] leftover = asm.drain();
            publish(ClientEvent.IN_DATA,
                    ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, leftover, 0, leftover.length, true));
        }
    }

    // ---- consumers ----

    private class Connected extends TriggerConsumer<Object> {
        // payload-agnostic: CONNECTED carries a SelectionKey over TCP, the remote address over UDP
        Connected() {
            super(ClientEvent.CONNECTED);
        }

        @Override
        public void accept(Object payload) {
            if (flag(DONE))
                return;
            ClientSessionContext ctx = ctx();
            if (ctx.getMode() == ClientSessionContext.Mode.TLS_HANDSHAKING) {
                // an IMMEDIATE ssl state is upgrading (its auto-start ran earlier in this
                // broadcast): the script starts on SECURE so sends go out encrypted
                flag(AWAIT_SECURE, true);
                return;
            }
            pump(ctx);
        }
    }

    private class Secured extends TriggerConsumer<Object> {
        Secured() {
            super(ClientEvent.SECURE);
        }

        @Override
        public void accept(Object sci) {
            if (flag(AWAIT_SECURE)) {
                flag(AWAIT_SECURE, false);
                pump(ctx());
            }
        }
    }

    private class InMessage extends TriggerConsumer<byte[]> {
        InMessage() {
            super(ClientEvent.IN_MESSAGE);
        }

        @Override
        public void accept(byte[] message) {
            if (flag(DONE))
                return;
            MessageAssemblerState.Assembly asm = SMProtoUtil.assembly(getStateMachine());
            // stream: the assembly holder IS the input (the payload is its snapshot) — buffering
            // it in the inbox would double-count it as start_tls residue
            if (asm == null || asm.getBoundary() != MessageAssemblerState.Boundary.STREAM)
                bytes(INBOX, message);
            pump(ctx());
        }
    }
}
