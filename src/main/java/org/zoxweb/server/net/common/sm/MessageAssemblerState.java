package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NamedValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Catalog state {@code assembler} (META-SM-PROTO-DESIGN.md §7): consumes
 * {@link ClientEvent#IN_DATA}, accumulates until the configured boundary strategy frames one
 * complete protocol message, and publishes it as {@link ClientEvent#IN_MESSAGE}
 * ({@code byte[]}, detached, consumer-owned).
 * <p>
 * <b>Configuration is the state's properties bag</b> (seeded from the JSON {@code config}
 * block; every key optional):
 * <ul>
 * <li>{@code boundary} — {@code datagram} / {@code delimited} / {@code length_prefixed} /
 * {@code stream}; default by transport: UDP → {@code datagram}, TCP → {@code stream}.</li>
 * <li>{@code max_message} — accumulation/message cap in bytes (default 65536); breach fails
 * the session.</li>
 * <li>{@code terminator} — {@code delimited} only: the boundary literal
 * ({@link SMProtoUtil} encoding, default {@code txt:\r\n}), binary-safe, detected across
 * chunks.</li>
 * <li>{@code strip_cr} — {@code delimited} only: strip one trailing {@code '\r'} from each
 * framed message (CR-tolerant line protocols, e.g. the SSH identification line; default
 * false).</li>
 * <li>{@code length} — {@code length_prefixed} only: {@code {offset, size (1/2/4),
 * endian (big|little, default big), adjust}}; the frame is {@code offset + size + parsed
 * length + adjust} bytes and is published whole.</li>
 * </ul>
 * Working memory — the accumulation — lives in an {@link Assembly} holder the state installs
 * into the <b>machine</b> properties bag ({@link SMProtoUtil#ASSEMBLY}) when it is registered:
 * the shared blackboard is the coordination seam. The {@code stream} strategy publishes the
 * unconsumed accumulation snapshot as the current message and the {@code controller} advances
 * the consumed offset through the holder (consume-through-match — byte-for-byte the v1
 * {@code expect} semantics); the {@code start_tls} residue check reads
 * {@link Assembly#pending()}.
 * <p>
 * When the controller completes the script it calls {@link Assembly#finish()}: from then on
 * the assembler ignores {@code IN_DATA} without touching the buffer — ownership belongs to the
 * post-{@code READY} application consumer. A machine composed with an assembler and no
 * controller never finishes the assembly; the application consumes {@code IN_MESSAGE} for the
 * life of the session.
 */
public class MessageAssemblerState extends State<Object> {

    public static final String NAME = "assembler";
    public static final int DEFAULT_MAX_MESSAGE = 65536;
    public static final String DEFAULT_TERMINATOR = "txt:\r\n";

    /**
     * The boundary strategies (§7) — an extensible vocabulary: a new framer is one enum entry
     * plus its meta keys, no change to the assembly machinery.
     */
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

    /**
     * The accumulation holder, installed into the machine properties bag under
     * {@link SMProtoUtil#ASSEMBLY} — the blackboard object through which the assembler and the
     * controller coordinate ({@code stream} consumption, {@code start_tls} residue,
     * end-of-script leftover drain).
     */
    public static final class Assembly {

        private final ByteArrayOutputStream acc = new ByteArrayOutputStream(64);
        private final Boundary boundary;
        private volatile boolean finished;

        Assembly(Boundary boundary) {
            this.boundary = boundary;
        }

        public Boundary getBoundary() {
            return boundary;
        }

        /** @return unconsumed accumulated bytes — the {@code start_tls} residue count */
        public synchronized int pending() {
            return acc.size();
        }

        /** @return a detached copy of the unconsumed accumulation */
        public synchronized byte[] snapshot() {
            return acc.toByteArray();
        }

        synchronized void append(byte[] data) {
            acc.write(data, 0, data.length);
        }

        /** Drops the first {@code count} unconsumed bytes (consume-through-match). */
        public synchronized void consume(int count) {
            byte[] all = acc.toByteArray();
            acc.reset();
            if (count < all.length)
                acc.write(all, count, all.length - count);
        }

        /** @return the unconsumed accumulation, clearing it — the end-of-script leftover drain */
        public synchronized byte[] drain() {
            byte[] all = acc.toByteArray();
            acc.reset();
            return all;
        }

        /**
         * Marks the assembly finished (the controller's script completed): the assembler stops
         * consuming {@code IN_DATA} — post-{@code READY} buffers belong to the application.
         */
        public void finish() {
            finished = true;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    public MessageAssemblerState() {
        this(null);
    }

    /**
     * @param config the {@code config} block seeded into this state's properties bag
     *               (null = all defaults)
     */
    public MessageAssemblerState(NVGenericMap config) {
        super(NAME);
        SMProtoUtil.seed(this, config);
        register(new InData());
    }

    /**
     * Registration hook: installs the {@link Assembly} holder into the machine bag so the
     * blackboard coordination is in place before any dispatch (compose first, bind last).
     */
    @Override
    public void setStateMachine(StateMachineInt<?> smi) {
        super.setStateMachine(smi);
        if (smi != null && SMProtoUtil.assembly(smi) == null)
            smi.getProperties().add(new NamedValue<Assembly>(SMProtoUtil.ASSEMBLY, new Assembly(resolveBoundary(smi))));
    }

    private Boundary resolveBoundary(StateMachineInt<?> smi) {
        String configured = SMProtoUtil.stringValue(getProperties(), "boundary", null);
        if (configured != null) {
            try {
                return Boundary.valueOf(configured.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown assembler boundary: " + configured);
            }
        }
        Object config = smi.getConfig();
        if (config instanceof ClientSessionContext
                && ((ClientSessionContext) config).getTransport() == ClientSessionContext.Transport.UDP)
            return Boundary.DATAGRAM;
        return Boundary.STREAM;
    }

    private class InData extends TriggerConsumer<ByteBuffer> {

        InData() {
            super(ClientEvent.IN_DATA);
        }

        @Override
        public void accept(ByteBuffer bb) {
            Assembly asm = SMProtoUtil.assembly(getStateMachine());
            if (asm == null || asm.isFinished())
                return; // script completed — the post-READY owner owns this buffer, do not touch it
            byte[] chunk = new byte[bb.remaining()];
            bb.get(chunk);
            ByteBufferUtil.cache(bb);

            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            // the STATE's bag is the configuration (a TriggerConsumer has its own bag — never
            // read config from it)
            int maxMessage = SMProtoUtil.intValue(MessageAssemblerState.this.getProperties(),
                    "max_message", DEFAULT_MAX_MESSAGE);
            switch (asm.getBoundary()) {
                case DATAGRAM:
                    if (chunk.length > maxMessage) {
                        ctx.fail(new IOException("datagram message exceeds max_message " + maxMessage + " bytes"));
                        return;
                    }
                    publishSync(ClientEvent.IN_MESSAGE, chunk);
                    break;
                case DELIMITED:
                    asm.append(chunk);
                    delimited(ctx, asm, maxMessage);
                    break;
                case LENGTH_PREFIXED:
                    asm.append(chunk);
                    lengthPrefixed(ctx, asm, maxMessage);
                    break;
                case STREAM:
                    asm.append(chunk);
                    if (asm.pending() > maxMessage) {
                        ctx.fail(new IOException("accumulation exceeded max_message " + maxMessage + " bytes without a match"));
                        return;
                    }
                    // the unconsumed snapshot IS the current message; the controller consumes
                    // through its match via the Assembly holder (v1 expect semantics)
                    publishSync(ClientEvent.IN_MESSAGE, asm.snapshot());
                    break;
            }
        }

        /** Frames every complete terminator-delimited message out of the accumulation. */
        private void delimited(ClientSessionContext ctx, Assembly asm, int maxMessage) {
            byte[] terminator = SMProtoUtil.STRING_TO_DATA.decode(
                    SMProtoUtil.stringValue(MessageAssemblerState.this.getProperties(), "terminator", DEFAULT_TERMINATOR));
            boolean stripCR = SMProtoUtil.booleanValue(MessageAssemblerState.this.getProperties(), "strip_cr", false);
            while (!ctx.getStateMachine().isClosed() && !asm.isFinished()) {
                byte[] have = asm.snapshot();
                int at = SMProtoUtil.indexOf(have, terminator);
                if (at < 0) {
                    if (have.length > maxMessage)
                        ctx.fail(new IOException("message exceeds max_message " + maxMessage + " bytes without a terminator"));
                    return;
                }
                asm.consume(at + terminator.length);
                int end = at;
                if (stripCR && end > 0 && have[end - 1] == '\r')
                    end--;
                if (end > maxMessage) {
                    ctx.fail(new IOException("message exceeds max_message " + maxMessage + " bytes"));
                    return;
                }
                publishSync(ClientEvent.IN_MESSAGE, Arrays.copyOfRange(have, 0, end));
            }
        }

        /** Frames every complete length-prefixed frame out of the accumulation. */
        private void lengthPrefixed(ClientSessionContext ctx, Assembly asm, int maxMessage) {
            // a nested config block is itself an NVGenericMap entry — reach it via getNV
            Object lengthNV = MessageAssemblerState.this.getProperties().getNV("length");
            NVGenericMap length = lengthNV instanceof NVGenericMap ? (NVGenericMap) lengthNV : null;
            int offset = SMProtoUtil.intValue(length, "offset", 0);
            int size = SMProtoUtil.intValue(length, "size", 2);
            boolean big = !"little".equalsIgnoreCase(SMProtoUtil.stringValue(length, "endian", "big"));
            int adjust = SMProtoUtil.intValue(length, "adjust", 0);
            if (size != 1 && size != 2 && size != 4)
                throw new IllegalArgumentException("length_prefixed size must be 1, 2 or 4: " + size);

            while (!ctx.getStateMachine().isClosed() && !asm.isFinished()) {
                byte[] have = asm.snapshot();
                if (have.length < offset + size)
                    return; // header incomplete
                long parsed = 0;
                for (int i = 0; i < size; i++)
                    parsed = (parsed << 8) | (have[offset + (big ? i : size - 1 - i)] & 0xFF);
                long frame = (long) offset + size + parsed + adjust;
                if (frame <= 0 || frame > maxMessage) {
                    ctx.fail(new IOException("length_prefixed frame of " + frame + " bytes outside (0, " + maxMessage + "]"));
                    return;
                }
                if (have.length < frame)
                    return; // frame incomplete
                asm.consume((int) frame);
                publishSync(ClientEvent.IN_MESSAGE, Arrays.copyOfRange(have, 0, (int) frame));
            }
        }
    }
}
