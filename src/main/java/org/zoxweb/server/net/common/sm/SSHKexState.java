package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NamedValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Catalog state {@code ssh_kex}: captures the server's {@code SSH_MSG_KEXINIT} after the
 * controller's script completes and records the key-exchange posture — the post-quantum
 * readiness check. The exchange grammar deliberately cannot parse fields out of a reply
 * (META-SM-PROTO-DESIGN.md §8), so this is a purpose-written catalog state, the same way the
 * roadmap's SMTP negotiator is.
 * <p>
 * <b>Activation window.</b> The state consumes {@link CommonTrigger#IN_DATA} only between the
 * controller finishing its script ({@link MessageAssemblerState.Assembly#isFinished()} — the
 * banner is validated, the assembler has gone dormant) and its own completion; outside that
 * window the buffer belongs to the assembler or to the post-{@code READY} application owner and
 * is not touched (one active owner per buffer, Iron Rule 7). Inside the window the state owns
 * the buffer: it copies the bytes out and recaches it. An SSH server sends its KEXINIT
 * unsolicited right after the identification line, so no write is ever needed — the capture is
 * fully passive.
 * <p>
 * <b>Framing</b> (RFC 4253 §6, pre-encryption): {@code uint32 packet_length, byte
 * padding_length, payload, padding}; the first payload byte is the message type. Transport
 * chatter before the KEXINIT ({@code SSH_MSG_IGNORE}, {@code SSH_MSG_DEBUG}, ...) is skipped,
 * bounded by {@code max_skip}; {@code SSH_MSG_DISCONNECT} fails the session. The KEXINIT
 * payload is {@code byte type, byte[16] cookie, name-list kex_algorithms, ...} — only the first
 * name-list is read.
 * <p>
 * <b>Configuration is the state's properties bag</b> (every key optional):
 * <ul>
 * <li>{@code pq_required} — a KEXINIT offering none of the PQ algorithms fails the session
 * ({@code validated=false} + {@code reason}, mirroring the validator); default false —
 * record-only.</li>
 * <li>{@code pq_algorithms} — comma-separated exact-name override of {@link #DEFAULT_PQ_KEX}
 * (the OpenSSH/IETF hybrid PQ key-exchange names).</li>
 * <li>{@code max_packet} — per-frame cap, default 35000 (RFC 4253 §6.1); breach fails the
 * session.</li>
 * <li>{@code max_skip} — total pre-KEXINIT skipped bytes before the session fails, default
 * 65536.</li>
 * </ul>
 * <b>Results</b> ({@link SMProtoUtil#results}): {@code kex_algorithms} — the server's list as
 * received; {@code pq_kex} — true if at least one PQ algorithm is offered;
 * {@code pq_kex_algorithms} — the matched names, when any. With {@code pq_required} the report
 * is complete on the fail paths too: no PQ offer, or a session that closes before the KEXINIT,
 * records {@code validated=false} + {@code reason} (an earlier false verdict is preserved).
 * <p>
 * The state gates {@link CommonTrigger#READY} ({@code ready_gate}), so a {@code close_on_ready}
 * probe stays open until the KEXINIT is captured; bytes after the KEXINIT frame are republished
 * as {@code IN_DATA} for the post-{@code READY} owner — after the {@code READY} broadcast, the
 * controller's leftover-drain idiom. Composition requires an assembler + controller upstream
 * (the activation signal is the script completing) and the state must be <b>declared before the
 * assembler</b>: broadcast order is declaration order, and the handoff depends on this state
 * seeing each {@code IN_DATA} buffer first — skipping it untouched while the assembly is
 * unfinished — so the mid-dispatch transition (the script completing inside the assembler's
 * consumer) can never make both states consume one buffer. {@code ClientConSMBuilder} fails
 * fast on both rules; the {@code ssh} factory sugar composes it correctly via
 * {@code kex_check} / {@code pq_required}.
 */
public class SSHKexState extends State<Object> {

    public static final String NAME = "ssh_kex";
    /** RFC 4253 §6.1: the minimum packet size every implementation must accept. */
    public static final int DEFAULT_MAX_PACKET = 35000;
    public static final int DEFAULT_MAX_SKIP = 65536;

    /**
     * The hybrid post-quantum key-exchange names recognized by default (exact-name match;
     * override via {@code pq_algorithms}): the OpenSSH sntrup761 pair and the ML-KEM hybrids
     * (draft-ietf-sshm-mlkem-hybrid-kex / OpenSSH 9.9+).
     */
    public static final String[] DEFAULT_PQ_KEX = {
            "sntrup761x25519-sha512@openssh.com",
            "sntrup761x25519-sha512",
            "mlkem768x25519-sha256",
            "mlkem768nistp256-sha256",
            "mlkem1024nistp384-sha384",
    };

    static final int SSH_MSG_DISCONNECT = 1;
    static final int SSH_MSG_KEXINIT = 20;

    private static final byte[] EMPTY = new byte[0];

    // working-memory keys in the state bag
    private static final String ACC = "acc";
    private static final String DONE = "done";
    private static final String SKIPPED = "skipped";

    public SSHKexState() {
        this(null);
    }

    /**
     * @param config the {@code config} block seeded into this state's properties bag
     *               (null = all defaults: record-only capture)
     */
    public SSHKexState(NVGenericMap config) {
        super(NAME);
        SMProtoUtil.seed(this, config);
        NVGenericMap bag = getProperties();
        bag.add(new NVBoolean(ClientConSMBuilder.READY_GATE, true));
        bag.add(new NamedValue<byte[]>(ACC, EMPTY));
        bag.build(new NVBoolean(DONE, false));
        bag.add(new NamedValue<int[]>(SKIPPED, new int[]{0}));
        register(new InData());
        register(new Closed());
    }

    // ---- working-memory accessors (all state lives in the bag) ----

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

    private int[] counter(String name) {
        return (int[]) ((NamedValue<?>) getProperties().getNV(name)).getValue();
    }

    private static int readInt(byte[] b, int at) {
        return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16)
                | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
    }

    /** The recognized PQ names: the {@code pq_algorithms} override, or the default set. */
    private String[] pqAlgorithms() {
        String override = SMProtoUtil.stringValue(getProperties(), "pq_algorithms", null);
        if (override == null)
            return DEFAULT_PQ_KEX;
        String[] ret = override.split(",");
        for (int i = 0; i < ret.length; i++)
            ret[i] = ret[i].trim();
        return ret;
    }

    /**
     * Frames every complete SSH binary packet out of the accumulation: skips pre-KEXINIT
     * transport chatter (bounded), fails on DISCONNECT or a malformed/oversize frame, and on
     * the KEXINIT records the verdict and completes — any residue after the frame goes to the
     * post-{@code READY} owner.
     */
    private void scan(ClientSessionContext ctx) {
        int maxPacket = SMProtoUtil.intValue(getProperties(), "max_packet", DEFAULT_MAX_PACKET);
        byte[] have = bytes(ACC);
        int pos = 0;
        while (!ctx.getStateMachine().isClosed()) {
            if (have.length - pos < 5)
                break; // header incomplete
            long packetLength = ((long) readInt(have, pos)) & 0xFFFFFFFFL;
            long frame = 4 + packetLength;
            if (packetLength < 2 || frame > maxPacket) {
                ctx.fail(new IOException("SSH packet of " + frame + " bytes outside [6, " + maxPacket + "]"));
                return;
            }
            if (have.length - pos < frame)
                break; // frame incomplete
            int padding = have[pos + 4] & 0xFF;
            int payloadLen = (int) packetLength - padding - 1;
            if (payloadLen < 1) {
                ctx.fail(new IOException("malformed SSH packet: padding " + padding + " leaves no payload"));
                return;
            }
            int type = have[pos + 5] & 0xFF;
            if (type == SSH_MSG_KEXINIT) {
                kexinit(ctx, have, pos, payloadLen);
                if (flag(DONE) && !ctx.getStateMachine().isClosed()) {
                    // completion fired READY inside gateComplete — hand any bytes after the
                    // KEXINIT frame to the post-READY owner (the controller drain idiom, so a
                    // consumer registered from a READY handler receives them)
                    int residueAt = pos + (int) frame;
                    bytes(ACC, EMPTY);
                    if (residueAt < have.length) {
                        // allocateByteBuffer expects offset 0 (capacity is length - offset)
                        byte[] residue = Arrays.copyOfRange(have, residueAt, have.length);
                        getStateMachine().publishSync(CommonTrigger.IN_DATA,
                                ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP,
                                        residue, 0, residue.length, true));
                    }
                }
                return;
            }
            if (type == SSH_MSG_DISCONNECT) {
                ctx.fail(new IOException("SSH_MSG_DISCONNECT received before KEXINIT"));
                return;
            }
            // pre-KEXINIT transport chatter (IGNORE, DEBUG, ...) — skip the frame, bounded
            int[] skipped = counter(SKIPPED);
            skipped[0] += (int) frame;
            if (skipped[0] > SMProtoUtil.intValue(getProperties(), "max_skip", DEFAULT_MAX_SKIP)) {
                ctx.fail(new IOException("skipped more than max_skip bytes before KEXINIT"));
                return;
            }
            pos += (int) frame;
        }
        if (pos > 0)
            have = Arrays.copyOfRange(have, pos, have.length);
        bytes(ACC, have);
    }

    /**
     * Parses the KEXINIT's first name-list ({@code kex_algorithms}), records the verdict, and
     * completes the gate — or fails the session ({@code pq_required} with no PQ offer, or a
     * malformed payload).
     */
    private void kexinit(ClientSessionContext ctx, byte[] have, int pos, int payloadLen) {
        // payload: byte SSH_MSG_KEXINIT, byte[16] cookie, name-list kex_algorithms, ...
        if (payloadLen < 1 + 16 + 4) {
            ctx.fail(new IOException("malformed KEXINIT: payload of " + payloadLen + " bytes"));
            return;
        }
        int kexLen = readInt(have, pos + 22);
        if (kexLen < 0 || 1 + 16 + 4 + kexLen > payloadLen) {
            ctx.fail(new IOException("malformed KEXINIT: kex_algorithms name-list of " + kexLen + " bytes"));
            return;
        }
        String kexAlgorithms = new String(have, pos + 26, kexLen, StandardCharsets.US_ASCII);
        List<String> matched = new ArrayList<String>();
        for (String offered : kexAlgorithms.split(",")) {
            for (String pq : pqAlgorithms()) {
                if (offered.equals(pq)) {
                    matched.add(offered);
                    break;
                }
            }
        }
        NVGenericMap results = SMProtoUtil.results(ctx.getStateMachine());
        results.build("kex_algorithms", kexAlgorithms);
        results.build(new NVBoolean("pq_kex", !matched.isEmpty()));
        if (!matched.isEmpty())
            results.build("pq_kex_algorithms", String.join(",", matched));
        flag(DONE, true);
        if (matched.isEmpty() && SMProtoUtil.booleanValue(getProperties(), "pq_required", false)) {
            String reason = "validation failed: no post-quantum key exchange offered: " + kexAlgorithms;
            results.build(new NVBoolean("validated", false)).build("reason", reason);
            ctx.fail(new IOException(reason));
            return;
        }
        ctx.gateComplete(NAME);
    }

    // ---- consumers ----

    private class InData extends TriggerConsumer<ByteBuffer> {

        InData() {
            super(CommonTrigger.IN_DATA);
        }

        @Override
        public void accept(ByteBuffer bb) {
            MessageAssemblerState.Assembly asm = SMProtoUtil.assembly(getStateMachine());
            // active window only: pre-completion buffers belong to the assembler, post-done
            // buffers to the post-READY application owner — do not touch them
            if (asm == null || !asm.isFinished() || flag(DONE))
                return;
            byte[] chunk = new byte[bb.remaining()];
            bb.get(chunk);
            ByteBufferUtil.cache(bb);
            byte[] acc = bytes(ACC);
            if (acc.length == 0) {
                bytes(ACC, chunk);
            } else {
                byte[] merged = Arrays.copyOf(acc, acc.length + chunk.length);
                System.arraycopy(chunk, 0, merged, acc.length, chunk.length);
                bytes(ACC, merged);
            }
            scan((ClientSessionContext) getStateMachine().getConfig());
        }
    }

    private class Closed extends TriggerConsumer<Object> {

        // payload is the terminating Throwable or null — not needed here
        Closed() {
            super(CommonTrigger.CLOSED);
        }

        @Override
        public void accept(Object cause) {
            if (flag(DONE))
                return;
            // report complete on the fail path too: a required PQ check that never saw the
            // KEXINIT is a failed validation — an earlier false verdict (e.g. the banner)
            // keeps its own reason. The STATE's bag is the configuration (a TriggerConsumer
            // has its own bag — never read config from it)
            if (SMProtoUtil.booleanValue(SSHKexState.this.getProperties(), "pq_required", false)) {
                NVGenericMap results = SMProtoUtil.results(getStateMachine());
                if (!Boolean.FALSE.equals(results.getValue("validated")))
                    results.build(new NVBoolean("validated", false))
                            .build("reason", "session closed before SSH KEXINIT");
            }
        }
    }
}
