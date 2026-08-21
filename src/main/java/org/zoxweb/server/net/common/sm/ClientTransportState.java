package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOBuffers;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * The base state every {@link ClientConSM} registers first: initializes the transport on
 * {@code CONNECTED} and routes every {@code RAW_IN_DATA} packet by {@link ClientSessionContext.Mode}.
 * <p>
 * The router owns {@code IN_RAW_DATA} exclusively — routing is broadcast pub/sub and a second
 * consumer would double-consume wire bytes; catalog and application states consume
 * {@link CommonTrigger#IN_DATA} instead, each buffer having exactly one active owner.
 * <p>
 * <b>The {@code IN_RAW_DATA} payload is borrowed, not owned</b> (TCP, since 2026-08-20): it is a
 * {@link DataPacket} (read counter, socket, peer address) wrapping the session's live read pair
 * ({@link IOBuffers}), published zero-copy by {@code TCPSMCallback}; the pair is still owned by
 * the callback, which clears and refills its in-buffer on the next read and
 * recaches the pair at teardown. The borrow is safe because {@code publishSync} runs this
 * consumer to completion on the same worker before the read loop continues — but the router must
 * <b>never recache it</b> and never retain it past this dispatch. Everything the router forwards
 * downstream is a detached copy under the old contract:
 * <ul>
 * <li>{@code PLAIN} — the router mints a detached copy of the wire bytes and publishes it as
 * {@link CommonTrigger#IN_DATA}; ownership of the copy transfers to the consuming state, which
 * recaches it. The borrowed pair itself is never recached here.</li>
 * <li>{@code TLS_HANDSHAKING} / {@code TLS_SECURE} — the ciphertext is copied out of the borrowed
 * in-buffer into the SSL engine's inbound net buffer chunk by chunk, publishing the current
 * handshake status per chunk. No consumer of these publishes reads the channel:
 * {@link SSLClientHandshakeState} and {@link SSLClientDataState} both unwrap only what the
 * router buffered — a handler-side read would race this chunked feed and reorder the
 * ciphertext, and would discard a peer's final-data-plus-FIN. EOF belongs to TCPSMCallback's
 * read loop.</li>
 * </ul>
 */
public class ClientTransportState extends State<Object> {

    public static final LogWrapper log = new LogWrapper(ClientTransportState.class).setEnabled(false);
    public static final String NAME = "client-transport";

    public ClientTransportState() {
        super(NAME);
        register(new Connected());
        register(new RawInData());
        register(new Closed());
    }

    private class Connected extends TriggerConsumer<SelectionKey> {
        Connected() {
            super(CommonTrigger.CONNECTED);
        }

        @Override
        public void accept(SelectionKey key) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            ctx.setMode(ClientSessionContext.Mode.PLAIN);
            ctx.gateComplete(NAME);
        }
    }

    private class RawInData extends TriggerConsumer<DataPacket<Long>> {
        RawInData() {
            super(CommonTrigger.IN_RAW_DATA);
        }

        @Override
        public void accept(DataPacket<Long> packet) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            ByteBuffer wire = packet.getIOBuffers().getInBuffer();
            switch (ctx.getMode()) {
                case PLAIN:
                    // the wire buffer is borrowed — mint the detached copy here (the callback no
                    // longer does) so the IN_DATA contract stays copy-owned-by-consumer
                    publishSync(CommonTrigger.IN_DATA, ByteBufferUtil.allocateByteBuffer(
                            ByteBufferUtil.BufferType.HEAP, wire.array(), 0, wire.remaining(), true));
                    break;
                case TLS_HANDSHAKING:
                case TLS_SECURE:
                    feedSSL(ctx, wire);
                    break;
            }
        }

        /**
         * Feeds the ciphertext out of the borrowed wire buffer into the engine's inbound net
         * buffer and drives the SSL states. The wire buffer belongs to the callback — it is
         * <b>never recached here</b>; the copy into the net buffer is the handoff. Stops feeding
         * the moment the SSL session closed (its buffers are recached at that point — writing
         * into them would corrupt the pool).
         */
        private void feedSSL(ClientSessionContext ctx, ByteBuffer wire) {
            SSLSessionConfig cfg = ctx.getSSLConfig();
            while (wire.hasRemaining() && !cfg.isClosed()) {
                ByteBuffer net = cfg.getSSLIOBuffers().getInBuffer();
                int n = Math.min(wire.remaining(), net.remaining());
                if (n == 0) {
                    ctx.fail(new IOException("SSL inbound net buffer overflow, no progress"));
                    return;
                }
                int savedLimit = wire.limit();
                wire.limit(wire.position() + n);
                net.put(wire);
                wire.limit(savedLimit);
                cfg.sslConnectionHelper.publish(cfg.getHandshakeStatus(), ctx.getSSLBridge());
            }
            // clean TLS EOF closes only the SSL config inside the handlers; close the session
            // here so teardown still publishes CLOSED
            if (cfg.isClosed() && ctx.getSession() != null && !ctx.getSession().isClosed())
                SharedIOUtil.close(ctx.getSession());
        }
    }

    private class Closed extends TriggerConsumer<Throwable> {
        Closed() {
            super(CommonTrigger.CLOSED);
        }

        @Override
        public void accept(Throwable t) {
            if (log.isEnabled()) log.getLogger().info(getStateMachine().getName() + " closed: " + t);
        }
    }
}
