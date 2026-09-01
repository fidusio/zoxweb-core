package org.zoxweb.server.net.protocols;

import org.zoxweb.server.fsm.MonoStateMachine;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The transport-agnostic protocol-validation engine (META-PROTOCOL.md §3): compiles a JSON
 * definition fail-fast, frames incoming bytes with the configured boundary strategy, runs the
 * linear {@code exchange} step script, and owns the results. It talks to its transport host only
 * through the {@link Host} seam — the engine never touches a socket, stream, or buffer pool.
 * <p>
 * Threading: everything runs inline on the host's read worker; NIOSocket serializes per-session
 * dispatches, so the engine needs no locks. The results bag is safely readable by an observer
 * thread after the host's close latch is released.
 * <p>
 * <b>Hard limit:</b> the script is a <i>guarded linear script</i> — a forward-only cursor over
 * the step list. Labeled steps and outcome routes ({@code jump}, an expect block's {@code alt},
 * a validate's {@code on_mismatch}) may only jump forward, enforced at compile, so loops are
 * impossible by construction; reserved targets are {@code done} and {@code fail}. A regex
 * capture <i>reads</i> a value into the report, but the script can never compute with it — no
 * captured value ever feeds a send. Anything richer (loops, computed values, multi-connection
 * logic) is purpose-written code, not a growth of this grammar. (Declarative framing and
 * fixed-offset field extraction are configuration, not computation.)
 * <p>
 * Dispatch is the inherited {@link MonoStateMachine} op→handler table — the
 * {@code CustomSSLStateMachine} pattern: publish is the loop, unsynchronized because the host
 * serializes per-session dispatches. The inherited {@code register}/{@code publish} surface is
 * an implementation detail; external calls are unsupported.
 */
public class ExchangeScript extends MonoStateMachine<String, ExchangeScript.Step> implements GetResults {

    /**
     * The engine's only coupling to transport (META-PROTOCOL.md §3.4), implemented by
     * {@code TCPMetaProtocol} and {@code UDPMetaProtocol}.
     */
    public interface Host {
        /** Writes fully to the session (plain, or encrypted once the session is secure). */
        void write(byte[] data) throws IOException;

        /** Starts the mid-session TLS upgrade; never called on a definition compiled without one. */
        void startTLS() throws IOException;

        /** Fails the session with {@code cause}: stash the close cause and close. */
        void fail(Throwable cause);

        /** The script completed: honor {@code close_on_ready}. */
        void complete();
    }

    /** The message-framing strategies (META-PROTOCOL.md §3.1). */
    public enum Boundary {
        /** One datagram = one message; pass-through, no accumulation. UDP default. */
        DATAGRAM,
        /** A message ends at the configured terminator sequence. */
        DELIMITED,
        /** A header field carries the payload length; the whole frame is the message. */
        LENGTH_PREFIXED,
        /** The unconsumed accumulation snapshot is the current message. TCP default. */
        STREAM,
    }

    /** The definition's TLS behavior (META-PROTOCOL.md §4). */
    public enum TLSMode {
        /** No {@code tls} block: the session is plaintext for its whole life. */
        NONE,
        /** Handshake right after connect; the whole script runs encrypted. */
        IMMEDIATE,
        /** Plaintext until the script's {@code start_tls} step upgrades. */
        ON_DEMAND,
    }

    public static final String DEFAULT_NAME = "protocol-validator";
    public static final int DEFAULT_MAX_MESSAGE = 65536;
    public static final int DEFAULT_MAX_SKIP = 65536;
    public static final String DEFAULT_TERMINATOR = "txt:\r\n";
    public static final int DEFAULT_TIMEOUT_SEC = 5;

    static final String OP_SEND = "send";
    static final String OP_EXPECT = "expect";
    static final String OP_VALIDATE = "validate";
    static final String OP_START_TLS = "start_tls";
    static final String OP_BOUNDARY = "boundary";
    static final String OP_LABEL = "label";
    static final String OP_JUMP = "jump";
    static final String OP_RECORD = "record";

    // routing sentinels: WAIT ends the publish chain until more input arrives; TARGET_FAIL and
    // "done" (= steps.size()) are the reserved route targets, resolved at compile
    private static final int WAIT = -1;
    private static final int TARGET_FAIL = -2;
    private static final int UNRESOLVED = Integer.MIN_VALUE;
    private static final String TARGET_DONE_NAME = "done";
    private static final String TARGET_FAIL_NAME = "fail";
    // results keys a record step may never write — verdict integrity
    private static final String[] RESERVED_RESULT_KEYS = {"validated", "reason", "ready", "latency_ms"};

    private static final byte[] EMPTY = new byte[0];

    /**
     * One framing configuration — the definition's {@code assembler} block, or a {@code boundary}
     * step's block for a mid-script switch. Parsed and validated fail-fast at compile.
     */
    private static final class Framing {
        final Boundary boundary;
        final int maxMessage;
        final int maxSkip;
        final byte[] terminator;
        final boolean stripCR;
        final int lengthOffset;
        final int lengthSize;
        final boolean lengthBigEndian;
        final int lengthAdjust;

        Framing(NVGenericMap block, boolean udp) {
            String configured = ProtoUtil.stringValue(block, "boundary", null);
            if (configured != null) {
                try {
                    boundary = Boundary.valueOf(configured.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("unknown assembler boundary: " + configured);
                }
            } else {
                boundary = udp ? Boundary.DATAGRAM : Boundary.STREAM;
            }
            maxMessage = ProtoUtil.intValue(block, "max_message", DEFAULT_MAX_MESSAGE);
            maxSkip = ProtoUtil.intValue(block, "max_skip", DEFAULT_MAX_SKIP);
            terminator = DataDecoder.StringToData.decode(
                    ProtoUtil.stringValue(block, "terminator", DEFAULT_TERMINATOR));
            if (boundary == Boundary.DELIMITED && terminator.length == 0)
                throw new IllegalArgumentException("delimited boundary with an empty terminator");
            stripCR = ProtoUtil.booleanValue(block, "strip_cr", false);
            NVGenericMap length = subMap(block, "length");
            lengthOffset = ProtoUtil.intValue(length, "offset", 0);
            lengthSize = ProtoUtil.intValue(length, "size", 2);
            lengthBigEndian = !"little".equalsIgnoreCase(ProtoUtil.stringValue(length, "endian", "big"));
            lengthAdjust = ProtoUtil.intValue(length, "adjust", 0);
            if (boundary == Boundary.LENGTH_PREFIXED
                    && lengthSize != 1 && lengthSize != 2 && lengthSize != 3 && lengthSize != 4)
                throw new IllegalArgumentException("length_prefixed size must be 1, 2, 3 or 4: " + lengthSize);
        }
    }

    /**
     * One compiled step: a static literal is decoded at compile time ({@code data} set); a
     * {@code ${var}}-bearing literal keeps its raw form ({@code literal}) and resolves at send
     * time; a {@code validate} step carries its match meta; a {@code boundary} step carries its
     * pre-validated framing. Guarded-linear extensions: a {@code label} step carries its name, a
     * {@code jump}/{@code on_mismatch} route resolves to a forward step index, an {@code expect}
     * block form carries alternative-pattern routes, a {@code validate} may carry a compiled
     * regex, and a {@code record} step carries its constants snapshot.
     */
    static final class Step {
        final String op;
        final byte[] data;
        final String literal;
        final NVGenericMap meta;
        final Object framing;
        final String labelName;                     // OP_LABEL: the (lowercased) jump target name
        final String routeLabel;                    // OP_JUMP target / OP_VALIDATE on_mismatch
        final Alt[] alts;                           // OP_EXPECT block form; null for string form
        final Pattern regex;                        // OP_VALIDATE; compiled fail-fast
        final int regexGroup;                       // capture group reported on a regex match
        final List<GetNameValue<?>> recordEntries;  // OP_RECORD constants
        int routeIndex = UNRESOLVED;                // routeLabel resolved to a forward step index

        Step(String op, byte[] data, String literal, NVGenericMap meta, Object framing) {
            this(op, data, literal, meta, framing, null, null, null, null, 0, null);
        }

        Step(String op, byte[] data, String literal, NVGenericMap meta, Object framing,
             String labelName, String routeLabel, Alt[] alts, Pattern regex, int regexGroup,
             List<GetNameValue<?>> recordEntries) {
            this.op = op;
            this.data = data;
            this.literal = literal;
            this.meta = meta;
            this.framing = framing;
            this.labelName = labelName;
            this.routeLabel = routeLabel;
            this.alts = alts;
            this.regex = regex;
            this.regexGroup = regexGroup;
            this.recordEntries = recordEntries;
        }
    }

    /**
     * One alternative pattern of an {@code expect} block: literal rules identical to the main
     * match (compile-time decode, or send-time {@code ${var}} resolution), routed to a
     * forward-resolved {@code goto} target on match.
     */
    static final class Alt {
        final byte[] data;
        final String literal;
        final String gotoLabel;
        int gotoIndex = UNRESOLVED;

        Alt(byte[] data, String literal, String gotoLabel) {
            this.data = data;
            this.literal = literal;
            this.gotoLabel = gotoLabel;
        }
    }

    // ---- compiled configuration (immutable after construction) ----
    private final Host host;
    private final String name;
    private final boolean udp;
    private final int[] ports;
    private final int timeoutSec;
    private final boolean closeOnReady;
    private final TLSMode tlsMode;
    private final boolean certValidation;
    private final List<Step> steps;
    private final NVGenericMap vars = new NVGenericMap("vars");

    // ---- session state (single read-worker, no locks) ----
    private final NVGenericMap results = new NVGenericMap("results");
    private final UByteArrayOutputStream acc; // the assembly buffer; tokens consumed via shiftLeft
    private Framing framing; // current framing; a boundary step swaps it mid-script
    private int cursor;
    private int skipped;
    private boolean started;
    private boolean done;
    private boolean failed;
    private boolean assemblyFinished;
    private boolean awaitSecure;
    private boolean secure;
    private byte[] inbox;
    private byte[] current;
    private long openNanos; // latency clock; 0 = not started

    /**
     * Compiles a definition fail-fast with an engine-private assembly buffer.
     *
     * @param config the parsed JSON definition
     * @param host   the transport host
     * @throws IllegalArgumentException on any invalid definition
     */
    public ExchangeScript(NVGenericMap config, Host host) {
        this(config, host, new UByteArrayOutputStream(64));
    }

    /**
     * Compiles a definition fail-fast: an unknown key/op or a malformed literal never surfaces
     * mid-session.
     *
     * @param config    the parsed JSON definition
     * @param host      the transport host
     * @param assembler the assembly buffer the session accumulates in — a host may share its own
     *                  (pooled) buffer and append received bytes directly, then call
     *                  {@link #parse()}; the engine consumes tokens off it via
     *                  {@code shiftLeft}, and the buffer's owner (the host) recaches it at
     *                  teardown
     * @throws IllegalArgumentException on any invalid definition
     */
    public ExchangeScript(NVGenericMap config, Host host, UByteArrayOutputStream assembler) {
        // unsynchronized dispatch: NIOSocket already serializes per-session dispatches on one
        // worker — the CustomSSLStateMachine pattern
        super(false);
        SUS.checkIfNulls("config, host and assembler can't be null", config, host, assembler);
        this.host = host;
        this.acc = assembler;
        register(OP_SEND, this::opSend)
                .register(OP_EXPECT, this::opExpect)
                .register(OP_VALIDATE, this::opValidate)
                .register(OP_BOUNDARY, this::opBoundary)
                .register(OP_START_TLS, this::opStartTLS)
                .register(OP_LABEL, this::opLabel)
                .register(OP_JUMP, this::opJump)
                .register(OP_RECORD, this::opRecord);

        // retired sm-era keys are rejected with a pointer to the replacement
        if (config.getNV("protocol") != null || config.getNV("ssh") != null || config.getNV("states") != null)
            throw new IllegalArgumentException(
                    "protocol/ssh/states are not supported: protocol shapes are plain JSON definitions"
                            + " (assembler + exchange), see META-PROTOCOL.md");

        name = ProtoUtil.stringValue(config, "name", DEFAULT_NAME);
        String transport = ProtoUtil.stringValue(config, "transport", "tcp").toLowerCase();
        if (!"tcp".equals(transport) && !"udp".equals(transport))
            throw new IllegalArgumentException("unknown transport: " + transport);
        udp = "udp".equals(transport);
        ports = ProtoUtil.ports(config);
        timeoutSec = ProtoUtil.intValue(config, "timeout_sec", DEFAULT_TIMEOUT_SEC);
        closeOnReady = ProtoUtil.booleanValue(config, "close_on_ready", udp);

        // tls block
        NVGenericMap tls = subMap(config, "tls");
        if (tls != null) {
            if (udp)
                throw new IllegalArgumentException("tls over udp is not supported (no DTLS in this stack)");
            String mode = ProtoUtil.stringValue(tls, "mode", "on_demand").toLowerCase();
            if ("immediate".equals(mode))
                tlsMode = TLSMode.IMMEDIATE;
            else if ("on_demand".equals(mode))
                tlsMode = TLSMode.ON_DEMAND;
            else
                throw new IllegalArgumentException("unknown tls mode: " + mode);
            certValidation = ProtoUtil.booleanValue(tls, "cert_validation", true);
        } else {
            tlsMode = TLSMode.NONE;
            certValidation = true;
        }

        // initial framing from the assembler block (defaults by transport)
        framing = new Framing(subMap(config, "assembler"), udp);

        // vars defaults (caller may override via setVar before connect)
        NVGenericMap varsBlock = subMap(config, "vars");
        if (varsBlock != null) {
            for (GetNameValue<?> gnv : varsBlock.values()) {
                Object v = gnv.getValue();
                if (v != null)
                    vars.add(new NVPair(gnv.getName(), v.toString()));
            }
        }

        // the script
        steps = compile(config.getNV("exchange"), udp);
        boolean hasStartTLS = false;
        for (Step s : steps) {
            if (OP_START_TLS.equals(s.op))
                hasStartTLS = true;
        }
        if (hasStartTLS && udp)
            throw new IllegalArgumentException("start_tls over udp is not supported");
        if (hasStartTLS && tlsMode != TLSMode.ON_DEMAND)
            throw new IllegalArgumentException("start_tls requires a tls block with mode on_demand");
    }

    /**
     * Compiles the {@code exchange} entry — either the JSON-parsed {@code NVPairList} (a
     * {@code validate}/{@code boundary} step's block arrives as its JSON text and is re-parsed
     * here) or a programmatic {@code NamedValue<List<GetNameValue<?>>>}. Fail-fast on unknown
     * ops, malformed literals, and invalid framing blocks.
     */
    private static List<Step> compile(Object exchange, boolean udp) {
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
                ret.add(new Step(op, EMPTY, null, null, null));
            } else if (OP_VALIDATE.equals(op)) {
                NVGenericMap meta = stepBlock(value, "validate step without match meta");
                validateExtractMeta(meta);
                boolean optional = ProtoUtil.booleanValue(meta, "optional", false);
                if (optional && ProtoUtil.stringValue(meta, "report", null) == null)
                    throw new IllegalArgumentException("optional validate without a report key");
                String onMismatch = routeName(meta, "on_mismatch");
                if (optional && onMismatch != null)
                    throw new IllegalArgumentException("optional and on_mismatch conflict: a probe never mismatches");
                Pattern regex = compileRegex(meta);
                int group = regexGroup(meta, regex);
                ret.add(new Step(op, null, null, meta, null, null, onMismatch, null, regex, group, null));
            } else if (OP_BOUNDARY.equals(op)) {
                if (udp)
                    throw new IllegalArgumentException("boundary step over udp is not supported (one datagram = one message)");
                NVGenericMap block = stepBlock(value, "boundary step without a framing block");
                ret.add(new Step(op, null, null, null, new Framing(block, false)));
            } else if (OP_LABEL.equals(op)) {
                String label = routeWord(value, "label");
                if (TARGET_DONE_NAME.equals(label) || TARGET_FAIL_NAME.equals(label))
                    throw new IllegalArgumentException("label '" + label + "' is a reserved route target");
                ret.add(new Step(op, null, null, null, null, label, null, null, null, 0, null));
            } else if (OP_JUMP.equals(op)) {
                ret.add(new Step(op, null, null, null, null, null, routeWord(value, "jump"), null, null, 0, null));
            } else if (OP_RECORD.equals(op)) {
                ret.add(new Step(op, null, null, null, null, null, null, null, null, 0, recordEntries(value)));
            } else if (OP_EXPECT.equals(op) && looksLikeBlock(value)) {
                ret.add(expectBlockStep(value));
            } else if (OP_SEND.equals(op) || OP_EXPECT.equals(op)) {
                String literal = (String) value;
                if (ProtoUtil.hasVars(literal))
                    ret.add(new Step(op, null, literal, null, null)); // send-time var resolution
                else
                    ret.add(new Step(op, DataDecoder.StringToData.decode(literal), null, null, null));
            } else {
                throw new IllegalArgumentException("unknown exchange step: " + op);
            }
        }
        resolveRoutes(ret);
        return ret;
    }

    /** A {@code label}/{@code jump} step value: a non-empty string, normalized lowercase. */
    private static String routeWord(Object value, String op) {
        if (!(value instanceof String) || SUS.isEmpty(((String) value).trim()))
            throw new IllegalArgumentException(op + " step requires a non-empty name");
        return ((String) value).trim().toLowerCase();
    }

    /** A route target named in a step's meta ({@code on_mismatch}), normalized lowercase. */
    private static String routeName(NVGenericMap meta, String key) {
        String raw = ProtoUtil.stringValue(meta, key, null);
        if (raw == null)
            return null;
        if (SUS.isEmpty(raw.trim()))
            throw new IllegalArgumentException(key + " requires a non-empty route target");
        return raw.trim().toLowerCase();
    }

    /** A {@code validate} step's optional regex, compiled fail-fast; {@code ${var}}s rejected. */
    private static Pattern compileRegex(NVGenericMap meta) {
        String raw = ProtoUtil.stringValue(meta, "regex", null);
        if (raw == null)
            return null;
        if (ProtoUtil.hasVars(raw))
            throw new IllegalArgumentException("${var} in a regex is not supported");
        try {
            return Pattern.compile(raw);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("malformed regex: " + e.getMessage(), e);
        }
    }

    /**
     * The reported capture group: an explicit {@code group} is range-checked against the compiled
     * pattern; the default is group 1 when the pattern captures, else the whole match.
     */
    private static int regexGroup(NVGenericMap meta, Pattern regex) {
        boolean explicit = meta.getNV("group") != null;
        if (regex == null) {
            if (explicit)
                throw new IllegalArgumentException("group without a regex");
            return 0;
        }
        int groupCount = regex.matcher("").groupCount();
        if (!explicit)
            return groupCount >= 1 ? 1 : 0;
        int group = ProtoUtil.intValue(meta, "group", -1);
        if (group < 0 || group > groupCount)
            throw new IllegalArgumentException("regex group " + group + " out of range [0, " + groupCount + "]");
        return group;
    }

    /** A {@code record} step's constants: a non-empty block, reserved results keys rejected. */
    private static List<GetNameValue<?>> recordEntries(Object value) {
        NVGenericMap block = stepBlock(value, "record step without a constants block");
        List<GetNameValue<?>> entries = new ArrayList<GetNameValue<?>>();
        for (GetNameValue<?> gnv : block.values()) {
            for (String reserved : RESERVED_RESULT_KEYS)
                if (reserved.equalsIgnoreCase(gnv.getName()))
                    throw new IllegalArgumentException("record key '" + gnv.getName() + "' is reserved for the verdict");
            entries.add(gnv);
        }
        if (entries.isEmpty())
            throw new IllegalArgumentException("record step with an empty constants block");
        return entries;
    }

    /**
     * Block-form detection for an {@code expect} step value. A byte literal that genuinely
     * starts with <code>{</code> must be written with the {@code txt:} prefix.
     */
    private static boolean looksLikeBlock(Object value) {
        return value instanceof NVGenericMap
                || (value instanceof String && ((String) value).trim().startsWith("{"));
    }

    /** Compiles an {@code expect} block form: the main {@code match} plus its {@code alt} routes. */
    private static Step expectBlockStep(Object value) {
        NVGenericMap block;
        try {
            block = stepBlock(value, "expect block without a match literal");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "expect block is not valid JSON (a byte literal starting with '{' needs the txt: prefix): "
                            + e.getMessage(), e);
        }
        String match = ProtoUtil.stringValue(block, "match", null);
        if (match == null)
            throw new IllegalArgumentException("expect block without a match literal");
        if (block.getNV("on_timeout") != null)
            throw new IllegalArgumentException(
                    "on_timeout is reserved (phase 2): no inactivity timer exists on client sessions");
        Alt[] alts = compileAlts(block.getNV("alt"));
        byte[] data = ProtoUtil.hasVars(match) ? null : DataDecoder.StringToData.decode(match);
        String literal = data == null ? match : null;
        return new Step(OP_EXPECT, data, literal, null, null, null, null, alts, null, 0, null);
    }

    /** Compiles an {@code alt} list: each entry a {@code {match, goto}} object. */
    private static Alt[] compileAlts(Object altValue) {
        if (altValue == null)
            return null;
        if (!(altValue instanceof NVGenericMapList))
            throw new IllegalArgumentException("alt entries must be {match, goto} objects");
        List<NVGenericMap> entries = ((NVGenericMapList) altValue).getValue();
        if (entries == null || entries.isEmpty())
            throw new IllegalArgumentException("expect block with an empty alt list");
        Alt[] alts = new Alt[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            NVGenericMap entry = entries.get(i);
            String match = ProtoUtil.stringValue(entry, "match", null);
            if (match == null)
                throw new IllegalArgumentException("alt entry without a match literal");
            String target = routeName(entry, "goto");
            if (target == null)
                throw new IllegalArgumentException("alt entry without a goto target");
            byte[] data = ProtoUtil.hasVars(match) ? null : DataDecoder.StringToData.decode(match);
            alts[i] = new Alt(data, data == null ? match : null, target);
        }
        return alts;
    }

    /**
     * Resolves every route ({@code jump}, {@code alt.goto}, {@code on_mismatch}) to a step
     * index. Targets are labels, or the reserved {@code done}/{@code fail}; every resolved route
     * must point strictly forward, so loops are impossible by construction.
     */
    private static void resolveRoutes(List<Step> steps) {
        Map<String, Integer> labels = new HashMap<String, Integer>();
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            if (OP_LABEL.equals(s.op) && labels.put(s.labelName, i) != null)
                throw new IllegalArgumentException("duplicate label: " + s.labelName);
        }
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            if (s.routeLabel != null)
                s.routeIndex = resolveTarget(s.routeLabel, i, steps.size(), labels);
            if (s.alts != null)
                for (Alt alt : s.alts)
                    alt.gotoIndex = resolveTarget(alt.gotoLabel, i, steps.size(), labels);
        }
    }

    /** One route target: {@code done}/{@code fail}, or a label strictly after the routing step. */
    private static int resolveTarget(String label, int at, int size, Map<String, Integer> labels) {
        if (TARGET_DONE_NAME.equals(label))
            return size;
        if (TARGET_FAIL_NAME.equals(label))
            return TARGET_FAIL;
        Integer target = labels.get(label);
        if (target == null)
            throw new IllegalArgumentException("unknown route target: " + label);
        if (target <= at)
            throw new IllegalArgumentException(
                    "backward or self route to '" + label + "': routes are forward-only");
        return target;
    }

    /** A step's object block: an {@code NVGenericMap}, or its JSON text re-parsed. */
    private static NVGenericMap stepBlock(Object value, String errorMessage) {
        if (value instanceof NVGenericMap)
            return (NVGenericMap) value;
        if (value instanceof String)
            return GSONUtil.fromJSONDefault((String) value, NVGenericMap.class);
        throw new IllegalArgumentException(errorMessage);
    }

    /** Fail-fast validation of a {@code validate} step's optional {@code extract} block. */
    private static void validateExtractMeta(NVGenericMap meta) {
        NVGenericMap extract = subMap(meta, "extract");
        if (extract == null)
            return;
        int size = ProtoUtil.intValue(extract, "size", 4);
        if (size != 1 && size != 2 && size != 3 && size != 4)
            throw new IllegalArgumentException("extract size must be 1, 2, 3 or 4: " + size);
        if (ProtoUtil.intValue(extract, "offset", 0) < 0)
            throw new IllegalArgumentException("extract offset must be >= 0");
    }

    private static NVGenericMap subMap(NVGenericMap config, String name) {
        if (config == null)
            return null;
        Object nv = config.getNV(name);
        return nv instanceof NVGenericMap ? (NVGenericMap) nv : null;
    }

    // ---- configuration accessors ----

    public String getName() {
        return name;
    }

    public boolean isUDP() {
        return udp;
    }

    /**
     * @return the definition's default-port hint — the first declared port — or -1 when none;
     * never an endpoint
     */
    public int getPort() {
        return ports.length > 0 ? ports[0] : -1;
    }

    /**
     * @return every declared well-known port ({@code "port": [25, 587]}), in declaration order;
     * empty when the definition declares none. Hints only — never endpoints
     */
    public int[] getPorts() {
        return ports.clone();
    }

    public int getTimeoutSec() {
        return timeoutSec;
    }

    public boolean isCloseOnReady() {
        return closeOnReady;
    }

    public TLSMode getTLSMode() {
        return tlsMode;
    }

    public boolean isCertValidation() {
        return certValidation;
    }

    /** @return the current framing strategy (a {@code boundary} step may switch it mid-script) */
    public Boundary getBoundary() {
        return framing.boundary;
    }

    /** Caller-side {@code ${name}} injection; overrides a {@code vars} default. Pre-connect only. */
    public ExchangeScript setVar(String name, String value) {
        SUS.checkIfNulls("var name can't be null", name);
        vars.build(name, value);
        return this;
    }

    // ---- session state accessors ----

    /** @return the verdict bag: {@code validated}/{@code reason}/{@code ready}/report keys/TLS params */
    @Override
    public NVGenericMap getResults() {
        return results;
    }

    /** @return true once the script completed (pass) — {@code ready} recorded */
    public boolean isDone() {
        return done;
    }

    /** @return true once the session failed (verdict recorded, host asked to close) */
    public boolean isFailed() {
        return failed;
    }

    // ---- the host-driven lifecycle ----

    /**
     * The host reports the transport is connected: starts the latency clock. Hosts call it at
     * connection establishment so TLS handshake time is measured; a script whose host never
     * calls it starts the clock at {@link #start()} instead.
     */
    public void markOpen() {
        if (openNanos == 0)
            openNanos = System.nanoTime();
    }

    /**
     * Starts the script (initial sends go out). Idempotent — the host calls it from its
     * connected/secured entry point.
     */
    public void start() {
        if (started || failed)
            return;
        started = true;
        markOpen();
        pump();
    }

    /**
     * The host reports the TLS handshake finished: resumes a {@code start_tls}-paused script, or
     * starts an IMMEDIATE-mode script whose first run was deferred until the session is secure.
     */
    public void secured() {
        secure = true;
        if (awaitSecure) {
            awaitSecure = false;
            pump();
        } else {
            start();
        }
    }

    /**
     * Feeds one detached chunk of received application bytes (post-decrypt when secure) into the
     * assembler; complete messages advance the script. The engine owns the array from here.
     */
    public void feed(byte[] chunk) {
        if (chunk == null || chunk.length == 0 || assemblyFinished || failed)
            return;
        if (framing.boundary == Boundary.DATAGRAM) {
            if (chunk.length > framing.maxMessage) {
                failSession(new IOException("datagram message exceeds max_message " + framing.maxMessage + " bytes"));
                return;
            }
            deliver(chunk);
            return;
        }
        acc.write(chunk, 0, chunk.length);
        drainFrames();
    }

    /**
     * Parses the shared assembler after the host appended received bytes directly into it (the
     * zero-intermediate-copy path of the {@code assembler} constructor): frames complete tokens
     * off the accumulation — consumed via {@code shiftLeft} — and advances the script.
     */
    public void parse() {
        if (assemblyFinished || failed)
            return;
        if (framing.boundary == Boundary.DATAGRAM) {
            // one appended blob = one message (a datagram framing on a stream host)
            int size = acc.size();
            if (size == 0)
                return;
            if (size > framing.maxMessage) {
                failSession(new IOException("datagram message exceeds max_message " + framing.maxMessage + " bytes"));
                return;
            }
            byte[] message = acc.copyBytes(0, size);
            acc.shiftLeft(size, 0);
            deliver(message);
            return;
        }
        drainFrames();
    }

    /**
     * Frames the accumulation under the <b>current</b> boundary until no complete frame remains;
     * re-reads the framing each round so a mid-script {@code boundary} step reframes the residue
     * under the new rule.
     */
    private void drainFrames() {
        while (!failed && !assemblyFinished) {
            Framing f = framing;
            if (f.boundary == Boundary.STREAM) {
                if (acc.size() > f.maxMessage) {
                    failSession(new IOException("accumulation exceeded max_message " + f.maxMessage + " bytes without a match"));
                    return;
                }
                // the unconsumed accumulation IS the current message; the pump consumes
                // through its match
                pump();
                if (framing.boundary == Boundary.STREAM)
                    return; // still streaming — wait for more data
                continue;   // a boundary step switched framing — reframe the residue
            }
            byte[] frame = f.boundary == Boundary.DELIMITED ? frameDelimited(f) : frameLengthPrefixed(f);
            if (frame == null)
                return; // incomplete (or the framer failed the session)
            deliver(frame);
        }
    }

    /** One delimited-frame attempt against the accumulation; null = incomplete or failed. */
    private byte[] frameDelimited(Framing f) {
        int at = acc.indexOf(f.terminator);
        if (at < 0) {
            if (acc.size() > f.maxMessage)
                failSession(new IOException("message exceeds max_message " + f.maxMessage + " bytes without a terminator"));
            return null;
        }
        int end = at;
        if (f.stripCR && end > 0 && acc.byteAt(end - 1) == '\r')
            end--;
        if (end > f.maxMessage) {
            failSession(new IOException("message exceeds max_message " + f.maxMessage + " bytes"));
            return null;
        }
        byte[] message = acc.copyBytes(0, end);
        acc.shiftLeft(at + f.terminator.length, 0); // consume through the terminator — next token at 0
        return message;
    }

    /** One length-prefixed-frame attempt against the accumulation; null = incomplete or failed. */
    private byte[] frameLengthPrefixed(Framing f) {
        if (acc.size() < f.lengthOffset + f.lengthSize)
            return null; // header incomplete
        long parsed = 0;
        for (int i = 0; i < f.lengthSize; i++)
            parsed = (parsed << 8) | (acc.byteAt(f.lengthOffset + (f.lengthBigEndian ? i : f.lengthSize - 1 - i)) & 0xFF);
        long frame = (long) f.lengthOffset + f.lengthSize + parsed + f.lengthAdjust;
        if (frame <= 0 || frame > f.maxMessage) {
            failSession(new IOException("length_prefixed frame of " + frame + " bytes outside (0, " + f.maxMessage + "]"));
            return null;
        }
        if (acc.size() < frame)
            return null; // frame incomplete
        byte[] message = acc.copyBytes(0, (int) frame);
        acc.shiftLeft((int) frame, 0); // consume the frame — next token at 0
        return message;
    }

    /** One complete framed message: into the inbox, then the pump runs against it. */
    private void deliver(byte[] message) {
        if (done)
            return;
        inbox = message;
        pump();
    }

    // ---- the step pump (META-PROTOCOL.md §3.2) ----
    // Dispatch is the inherited MonoStateMachine op→handler table (the CustomSSLStateMachine
    // pattern): publish IS the loop — each handler performs its step and advance() publishes
    // the next. The chain runs inline on the read worker and is bounded by the step count.

    /**
     * Resumes the script at the current step: runs forward until one must wait (an
     * {@code expect}/{@code validate} without a match, or a {@code start_tls} handshake) or the
     * script completes.
     */
    private void pump() {
        if (done || failed)
            return;
        advance(cursor);
    }

    /**
     * Moves the cursor to {@code next} and publishes that step — or completes when the step
     * list is exhausted ({@code done} route included), or fails on a {@code fail} route. Every
     * route is forward-only, so the publish chain can never loop.
     */
    private void advance(int next) {
        if (done || failed)
            return;
        if (next == TARGET_FAIL) {
            failSession(new IOException("routed to fail at step " + cursor));
            return;
        }
        if (next >= steps.size()) {
            complete();
            return;
        }
        cursor = next;
        Step step = steps.get(cursor);
        publish(step.op, step);
    }

    /** {@code send}: write the resolved literal; a send opens a new request/response round. */
    private void opSend(Step step) {
        byte[] out;
        try {
            out = data(step);
        } catch (IllegalArgumentException e) {
            failSession(e);
            return;
        }
        try {
            host.write(out);
        } catch (Exception e) {
            failSession(e);
            return;
        }
        current = null; // a send opens a new request/response round
        advance(cursor + 1);
    }

    /** {@code expect}: wait-and-match; the chain ends when input is still incomplete. */
    private void opExpect(Step step) {
        int next = expect(step);
        if (next == WAIT)
            return; // wait for more input (or the session failed)
        advance(next);
    }

    /** {@code validate}: apply the match meta to the current message and record the verdict. */
    private void opValidate(Step step) {
        byte[] message = current;
        if (message == null) {
            if (framing.boundary == Boundary.STREAM) {
                if (acc.size() == 0)
                    return; // wait for data
                message = acc.copyBytes(0, acc.size()); // validate reads, never consumes, the stream
            } else {
                message = takeInbox();
                if (message == null)
                    return; // wait for the next message
            }
            current = message;
        }
        int next = validate(step, message);
        if (next == WAIT)
            return; // verdict was false — the session is failed
        advance(next);
    }

    /** {@code label}: a no-op jump target. */
    private void opLabel(Step step) {
        advance(cursor + 1);
    }

    /** {@code jump}: an unconditional forward route. */
    private void opJump(Step step) {
        advance(step.routeIndex);
    }

    /**
     * {@code record}: merges the step's constants into the results as fresh values — the way a
     * branch marks which path ran. Reserved verdict keys were rejected at compile.
     */
    private void opRecord(Step step) {
        for (GetNameValue<?> gnv : step.recordEntries) {
            Object v = gnv.getValue();
            if (v instanceof Boolean)
                results.build(new NVBoolean(gnv.getName(), (Boolean) v));
            else if (v instanceof Long || v instanceof Integer)
                results.build(new NVLong(gnv.getName(), ((Number) v).longValue()));
            else if (v instanceof Number)
                results.build(new NVDouble(gnv.getName(), ((Number) v).doubleValue()));
            else
                results.build(gnv.getName(), v != null ? v.toString() : null);
        }
        advance(cursor + 1);
    }

    /** {@code boundary}: swap the framing; residue reframes on the caller's drainFrames round. */
    private void opBoundary(Step step) {
        framing = (Framing) step.framing;
        // residue in the accumulation reframes under the new rule on the caller's
        // drainFrames round; a stale framed inbox never crosses a boundary switch
        inbox = null;
        advance(cursor + 1);
    }

    /** {@code start_tls}: residue check, then pause the chain for the handshake. */
    private void opStartTLS(Step step) {
        int residue = acc.size() + (inbox != null ? inbox.length : 0);
        if (residue > 0) {
            // STARTTLS injection mitigation: unexpected plaintext before the upgrade
            // fails the session — never clear-and-continue
            failSession(new IOException("STARTTLS injection: " + residue + " byte(s) of residue after the last expect"));
            return;
        }
        if (secure) {
            failSession(new IOException("start_tls: transport is not plaintext"));
            return;
        }
        cursor++;
        awaitSecure = true;
        try {
            host.startTLS();
        } catch (Exception e) {
            failSession(e);
        }
        // the chain ends here; secured() resumes the pump at the next step
    }

    /**
     * One {@code expect} attempt against the available input. Returns the next step index —
     * {@code cursor + 1} on a main match, an alt's forward route on an alternative match — or
     * {@code WAIT} when the step must wait for more input (or the session failed). The main
     * match always wins over the alternatives (regardless of byte position on a stream); among
     * alternatives, declaration order wins.
     */
    private int expect(Step step) {
        byte[] want;
        try {
            want = data(step);
        } catch (IllegalArgumentException e) {
            failSession(e);
            return WAIT;
        }
        if (framing.boundary == Boundary.STREAM) {
            // stream: contains-match on the unconsumed accumulation, consume through the match
            int at = acc.indexOf(want);
            if (at >= 0) {
                current = acc.copyBytes(0, at + want.length);
                acc.shiftLeft(at + want.length, 0); // consume through the match — next token at 0
                return cursor + 1;
            }
            if (step.alts != null) {
                for (Alt alt : step.alts) {
                    byte[] altWant;
                    try {
                        altWant = altData(alt);
                    } catch (IllegalArgumentException e) {
                        failSession(e);
                        return WAIT;
                    }
                    int altAt = acc.indexOf(altWant);
                    if (altAt >= 0) {
                        // an alt consumes through its own match — a branch feeding start_tls
                        // must still consume through the line terminator (residue check)
                        current = acc.copyBytes(0, altAt + altWant.length);
                        acc.shiftLeft(altAt + altWant.length, 0);
                        return alt.gotoIndex;
                    }
                }
            }
            return WAIT;
        }
        // framed: per-message contains-match; a message matching neither the main pattern nor
        // any alt is skipped, bounded by max_skip (the SMTP 250- continuation idiom)
        byte[] message = takeInbox();
        while (message != null) {
            if (ProtoUtil.indexOf(message, want) >= 0) {
                current = message;
                return cursor + 1;
            }
            if (step.alts != null) {
                for (Alt alt : step.alts) {
                    byte[] altWant;
                    try {
                        altWant = altData(alt);
                    } catch (IllegalArgumentException e) {
                        failSession(e);
                        return WAIT;
                    }
                    if (ProtoUtil.indexOf(message, altWant) >= 0) {
                        current = message;
                        return alt.gotoIndex;
                    }
                }
            }
            skipped += message.length;
            if (skipped > framing.maxSkip) {
                failSession(new IOException("expect skipped more than max_skip bytes without a match"));
                return WAIT;
            }
            message = takeInbox();
        }
        return WAIT;
    }

    /** An alt's bytes: compile-time decode, or match-time variable-resolved decode. */
    private byte[] altData(Alt alt) {
        return alt.data != null ? alt.data : ProtoUtil.STRING_VARS_TO_DATA.decode(alt.literal, vars);
    }

    private byte[] takeInbox() {
        byte[] message = inbox;
        inbox = null;
        return message;
    }

    /** Step bytes: compile-time decode, or send-time variable-resolved decode. */
    private byte[] data(Step step) {
        return step.data != null ? step.data : ProtoUtil.STRING_VARS_TO_DATA.decode(step.literal, vars);
    }

    // ---- validation (META-PROTOCOL.md §3.3) ----

    /**
     * Applies the {@code validate} match meta to the message and records the verdict; a
     * mismatch fails the session so the close cause and the report agree — unless the step
     * declares an {@code on_mismatch} route, which forwards the cursor instead and leaves the
     * verdict untouched. An {@code extract} block ({@code {offset, size, endian, adjust}}) first
     * narrows the message to the length-prefixed field at the fixed offset — the matches and the
     * {@code report} then apply to the extracted field. A {@code regex} is checked last, against
     * the unfolded target decoded ISO-8859-1 (binary-safe); on a match the configured capture
     * group is what {@code report} stores. An {@code optional} step is a probe, not an
     * assertion: the match outcome is recorded as a boolean under the {@code report} key, the
     * script continues either way, and {@code validated}/{@code reason} are untouched.
     * <p>
     * Returns the next step index, or {@code WAIT} when the session failed.
     */
    private int validate(Step step, byte[] message) {
        NVGenericMap meta = step.meta;
        String reason = null;
        byte[] target = message;

        NVGenericMap extract = subMap(meta, "extract");
        if (extract != null) {
            int offset = ProtoUtil.intValue(extract, "offset", 0);
            int size = ProtoUtil.intValue(extract, "size", 4);
            boolean big = !"little".equalsIgnoreCase(ProtoUtil.stringValue(extract, "endian", "big"));
            int adjust = ProtoUtil.intValue(extract, "adjust", 0);
            if (message.length < offset + size) {
                reason = "extract header out of bounds (message " + message.length + " bytes)";
            } else {
                long len = 0;
                for (int i = 0; i < size; i++)
                    len = (len << 8) | (message[offset + (big ? i : size - 1 - i)] & 0xFF);
                len += adjust;
                if (len < 0 || offset + size + len > message.length)
                    reason = "extract field of " + len + " bytes out of bounds (message " + message.length + " bytes)";
                else
                    target = Arrays.copyOfRange(message, offset + size, (int) (offset + size + len));
            }
        }

        if (reason == null) {
            // ignore_case ASCII-folds both sides of the matches; report keeps the original bytes
            boolean ignoreCase = ProtoUtil.booleanValue(meta, "ignore_case", false);
            byte[] subject = ignoreCase ? DataEncoder.LowerAscii.encode(target) : target;
            byte[] prefix = fold(literal(meta, "prefix"), ignoreCase);
            if (prefix != null && !ProtoUtil.startsWith(subject, prefix))
                reason = "prefix mismatch";
            byte[] contains = reason == null ? fold(literal(meta, "contains"), ignoreCase) : null;
            if (contains != null && ProtoUtil.indexOf(subject, contains) < 0)
                reason = "does not contain expected sequence";
            byte[] exact = reason == null ? fold(literal(meta, "exact"), ignoreCase) : null;
            if (exact != null && !Arrays.equals(subject, exact))
                reason = "exact mismatch";
        }

        // regex last, against the unfolded target: ISO-8859-1 maps bytes 1:1 to chars, so binary
        // messages match safely; ignore_case never folds a regex — use an inline (?i)
        String capture = null;
        if (reason == null && step.regex != null) {
            Matcher matcher = step.regex.matcher(new String(target, StandardCharsets.ISO_8859_1));
            if (matcher.find()) {
                String group = matcher.group(step.regexGroup);
                capture = group != null ? group : ""; // an unparticipating group reports empty
            } else {
                reason = "regex mismatch";
            }
        }

        if (ProtoUtil.booleanValue(meta, "optional", false)) {
            // probe, not verdict: record the outcome and continue on match and mismatch alike
            results.build(new NVBoolean(ProtoUtil.stringValue(meta, "report", null), reason == null));
            return cursor + 1;
        }

        if (reason == null) {
            results.build(new NVBoolean("validated", true));
            String report = ProtoUtil.stringValue(meta, "report", null);
            if (report != null)
                results.build(report, capture != null ? capture : new String(target, StandardCharsets.UTF_8));
            return cursor + 1;
        }

        if (step.routeIndex != UNRESOLVED && step.routeIndex != TARGET_FAIL) {
            // on_mismatch: route forward instead of failing; the verdict stays untouched and
            // the current message stays in place for the routed path to examine
            return step.routeIndex;
        }

        // no route, or on_mismatch: "fail" — the legacy failure verdict either way
        reason = "validation failed: " + reason + ": " + new String(target, StandardCharsets.UTF_8);
        results.build(new NVBoolean("validated", false)).build("reason", reason);
        failed = true;
        recordLatency();
        host.fail(new IOException(reason));
        return WAIT;
    }

    /** ASCII-folds a decoded match literal when {@code ignore_case} is active; null passes through. */
    private static byte[] fold(byte[] data, boolean ignoreCase) {
        return (data == null || !ignoreCase) ? data : DataEncoder.LowerAscii.encode(data);
    }

    /** Decodes a match literal from the meta, {@code ${var}}s resolved; null when absent. */
    private byte[] literal(NVGenericMap meta, String key) {
        String raw = ProtoUtil.stringValue(meta, key, null);
        if (raw == null)
            return null;
        return ProtoUtil.hasVars(raw)
                ? ProtoUtil.STRING_VARS_TO_DATA.decode(raw, vars)
                : DataDecoder.StringToData.decode(raw);
    }

    // ---- completion and failure ----

    /**
     * The completion rule: the step list finishing = session done. Records the default verdict
     * when no {@code validate} ran — every run yields a verdict — marks the assembly finished
     * (later bytes are ignored), records {@code ready}, and hands the decision to the host
     * ({@code close_on_ready}).
     */
    private void complete() {
        done = true;
        assemblyFinished = true;
        if (results.getNV("validated") == null)
            results.build(new NVBoolean("validated", true)).build("reason", "script completed");
        results.build(new NVBoolean("ready", true));
        recordLatency();
        host.complete();
    }

    /** Freezes the connect-to-verdict duration as {@code latency_ms} — first measurement wins. */
    private void recordLatency() {
        if (openNanos != 0 && results.getNV("latency_ms") == null)
            results.build(new NVLong("latency_ms", (System.nanoTime() - openNanos) / 1000000L));
    }

    /**
     * Records the failure verdict for an externally-caused failure (I/O error, EOF before
     * completion, port-unreachable) without re-entering the host's fail path — the host calls
     * this from its own exception handling before closing.
     */
    public void recordFailure(Throwable cause) {
        failed = true;
        if (results.getNV("validated") == null) {
            String reason = cause != null
                    ? (cause.getMessage() != null ? cause.getMessage() : cause.toString())
                    : "session failed";
            results.build(new NVBoolean("validated", false)).build("reason", reason);
        }
        recordLatency();
    }

    /** Engine-detected failure: record the verdict, then ask the host to close with the cause. */
    private void failSession(Throwable cause) {
        recordFailure(cause);
        host.fail(cause);
    }
}
