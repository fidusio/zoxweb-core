package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * The base state every {@link ClientConnectionSM} registers first: initializes the transport on
 * {@code CONNECTED} and routes every {@code RAW_IN_DATA} packet by {@link ClientSessionContext.Mode}.
 * <p>
 * The router owns {@code RAW_IN_DATA} exclusively (see {@link ConnectionPhase} contract):
 * <ul>
 * <li>{@code PLAIN} — the packet passes through as {@link ClientEvent#IN_DATA}; ownership
 * transfers to the consuming phase/application, which recaches it.</li>
 * <li>{@code TLS_HANDSHAKING} / {@code TLS_SECURE} — the packet's ciphertext is copied into the
 * SSL engine's inbound net buffer chunk by chunk, publishing the current handshake status per
 * chunk; the packet is recached here. No consumer of these publishes reads the channel:
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
            super(TCPSMCallback.BasicEvent.CONNECTED);
        }

        @Override
        public void accept(SelectionKey key) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            ctx.setMode(ClientSessionContext.Mode.PLAIN);
            ctx.phaseComplete(NAME);
        }
    }

    private class RawInData extends TriggerConsumer<ByteBuffer> {
        RawInData() {
            super(TCPSMCallback.BasicEvent.RAW_IN_DATA);
        }

        @Override
        public void accept(ByteBuffer packet) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            switch (ctx.getMode()) {
                case PLAIN:
                    // pass-through: ownership transfers to the active IN_DATA owner
                    publishSync(ClientEvent.IN_DATA, packet);
                    break;
                case TLS_HANDSHAKING:
                case TLS_SECURE:
                    feedSSL(ctx, packet);
                    break;
            }
        }

        /**
         * Feeds the ciphertext packet into the engine's inbound net buffer and drives the SSL
         * states; recaches the packet. Stops feeding the moment the SSL session closed (its
         * buffers are recached at that point — writing into them would corrupt the pool).
         */
        private void feedSSL(ClientSessionContext ctx, ByteBuffer packet) {
            SSLSessionConfig cfg = ctx.getSSLConfig();
            while (packet.hasRemaining() && !cfg.isClosed()) {
                ByteBuffer net = cfg.getSSLInboundBuffer();
                int n = Math.min(packet.remaining(), net.remaining());
                if (n == 0) {
                    ByteBufferUtil.cache(packet);
                    ctx.fail(new IOException("SSL inbound net buffer overflow, no progress"));
                    return;
                }
                int savedLimit = packet.limit();
                packet.limit(packet.position() + n);
                net.put(packet);
                packet.limit(savedLimit);
                cfg.sslConnectionHelper.publish(cfg.getHandshakeStatus(), ctx.getSSLBridge());
            }
            ByteBufferUtil.cache(packet);
            // clean TLS EOF closes only the SSL config inside the handlers; close the session
            // here so teardown still publishes CLOSED
            if (cfg.isClosed() && ctx.getSession() != null && !ctx.getSession().isClosed())
                SharedIOUtil.close(ctx.getSession());
        }
    }

    private class Closed extends TriggerConsumer<Throwable> {
        Closed() {
            super(TCPSMCallback.BasicEvent.CLOSED);
        }

        @Override
        public void accept(Throwable t) {
            if (log.isEnabled()) log.getLogger().info(getStateMachine().getName() + " closed: " + t);
        }
    }
}
