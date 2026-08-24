package org.zoxweb.server.net.protocols;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * <b>Hard limit:</b> the script is linear — no branching, no computing a value from a reply. A
 * richer check is purpose-written code, not a growth of this grammar. (Declarative framing and
 * fixed-offset field extraction are configuration, not computation.)
 */
public class ExchangeScript {

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
     * pre-validated framing.
     */
    static final class Step {
        final String op;
        final byte[] data;
        final String literal;
        final NVGenericMap meta;
        final Object framing;

        Step(String op, byte[] data, String literal, NVGenericMap meta, Object framing) {
            this.op = op;
            this.data = data;
            this.literal = literal;
            this.meta = meta;
            this.framing = framing;
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
        SUS.checkIfNulls("config, host and assembler can't be null", config, host, assembler);
        this.host = host;
        this.acc = assembler;

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
                if (ProtoUtil.booleanValue(meta, "optional", false)
                        && ProtoUtil.stringValue(meta, "report", null) == null)
                    throw new IllegalArgumentException("optional validate without a report key");
                ret.add(new Step(op, null, null, meta, null));
            } else if (OP_BOUNDARY.equals(op)) {
                if (udp)
                    throw new IllegalArgumentException("boundary step over udp is not supported (one datagram = one message)");
                NVGenericMap block = stepBlock(value, "boundary step without a framing block");
                ret.add(new Step(op, null, null, null, new Framing(block, false)));
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
        return ret;
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
     * Starts the script (initial sends go out). Idempotent — the host calls it from its
     * connected/secured entry point.
     */
    /**
     * The host reports the transport is connected: starts the latency clock. Hosts call it at
     * connection establishment so TLS handshake time is measured; a script whose host never
     * calls it starts the clock at {@link #start()} instead.
     */
    public void markOpen() {
        if (openNanos == 0)
            openNanos = System.nanoTime();
    }

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

    /**
     * Runs forward through the steps until one must wait (an {@code expect}/{@code validate}
     * without a match, or a {@code start_tls} handshake) or the script completes.
     */
    private void pump() {
        while (!done && !failed && cursor < steps.size()) {
            Step step = steps.get(cursor);
            if (OP_SEND.equals(step.op)) {
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
                cursor++;
            } else if (OP_EXPECT.equals(step.op)) {
                if (!expect(step))
                    return; // wait for more input (or the session failed)
                cursor++;
            } else if (OP_VALIDATE.equals(step.op)) {
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
                if (!validate(step.meta, message))
                    return; // verdict was false — the session is failed
                cursor++;
            } else if (OP_BOUNDARY.equals(step.op)) {
                framing = (Framing) step.framing;
                cursor++;
                // residue in the accumulation reframes under the new rule on the caller's
                // drainFrames round; a stale framed inbox never crosses a boundary switch
                inbox = null;
            } else { // OP_START_TLS
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
                return; // resume on secured()
            }
        }
        if (!done && !failed && cursor >= steps.size())
            complete();
    }

    /**
     * One {@code expect} attempt against the available input; true = matched (cursor may
     * advance), false = must wait for more input (or the session failed).
     */
    private boolean expect(Step step) {
        byte[] want;
        try {
            want = data(step);
        } catch (IllegalArgumentException e) {
            failSession(e);
            return false;
        }
        if (framing.boundary == Boundary.STREAM) {
            // stream: contains-match on the unconsumed accumulation, consume through the match
            int at = acc.indexOf(want);
            if (at < 0)
                return false;
            current = acc.copyBytes(0, at + want.length);
            acc.shiftLeft(at + want.length, 0); // consume through the match — next token at 0
            return true;
        }
        // framed: per-message contains-match; a non-matching complete message is skipped,
        // bounded by max_skip (the SMTP 250- continuation idiom)
        byte[] message = takeInbox();
        while (message != null) {
            if (ProtoUtil.indexOf(message, want) >= 0) {
                current = message;
                return true;
            }
            skipped += message.length;
            if (skipped > framing.maxSkip) {
                failSession(new IOException("expect skipped more than max_skip bytes without a match"));
                return false;
            }
            message = takeInbox();
        }
        return false;
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
     * mismatch fails the session so the close cause and the report agree. An {@code extract}
     * block ({@code {offset, size, endian, adjust}}) first narrows the message to the
     * length-prefixed field at the fixed offset — the matches and the {@code report} then apply
     * to the extracted field. An {@code optional} step is a probe, not an assertion: the match
     * outcome is recorded as a boolean under the {@code report} key, the script continues either
     * way, and {@code validated}/{@code reason} are untouched.
     */
    private boolean validate(NVGenericMap meta, byte[] message) {
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

        if (ProtoUtil.booleanValue(meta, "optional", false)) {
            // probe, not verdict: record the outcome and continue on match and mismatch alike
            results.build(new NVBoolean(ProtoUtil.stringValue(meta, "report", null), reason == null));
            return true;
        }

        if (reason == null) {
            results.build(new NVBoolean("validated", true));
            String report = ProtoUtil.stringValue(meta, "report", null);
            if (report != null)
                results.build(report, new String(target, StandardCharsets.UTF_8));
            return true;
        }
        reason = "validation failed: " + reason + ": " + new String(target, StandardCharsets.UTF_8);
        results.build(new NVBoolean("validated", false)).build("reason", reason);
        failed = true;
        recordLatency();
        host.fail(new IOException(reason));
        return false;
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
