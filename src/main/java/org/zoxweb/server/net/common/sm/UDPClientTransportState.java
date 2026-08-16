package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;

/**
 * The base state of a UDP-transport {@link ClientConSM} (the {@link ClientTransportState} analog
 * for datagram sessions): initializes the transport on {@code CONNECTED} and routes every
 * {@code DATAGRAM} packet's payload to the phases as {@link ClientEvent#IN_DATA}.
 * <p>
 * UDP is always plaintext in this stack (no DTLS), so the routing is a pure pass-through — no
 * transport modes, no SSL feed. The router owns {@code DATAGRAM} exclusively, mirroring the
 * {@code RAW_IN_DATA} ownership rule of the TCP router: phases consume {@link ClientEvent#IN_DATA}
 * only. The packet buffer is the detached consumer-owned copy minted by
 * {@link UDPSMCallback}; ownership transfers to the active {@code IN_DATA} owner, which recaches
 * it. The datagram's source address is dropped here — the channel is connected, so it is always
 * the session's remote.
 */
public class UDPClientTransportState extends State<Object> {

    public static final LogWrapper log = new LogWrapper(UDPClientTransportState.class).setEnabled(false);
    public static final String NAME = "udp-client-transport";

    public UDPClientTransportState() {
        super(NAME);
        register(new Connected());
        register(new Datagram());
        register(new Closed());
    }

    private class Connected extends TriggerConsumer<Object> {
        Connected() {
            super(SMProtoUtil.BasicEvent.CONNECTED);
        }

        @Override
        public void accept(Object remote) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            ctx.setMode(ClientSessionContext.Mode.PLAIN);
            ctx.phaseComplete(NAME);
        }
    }

    private class Datagram extends TriggerConsumer<DataPacket<Long>> {
        Datagram() {
            super(SMProtoUtil.BasicEvent.DATAGRAM);
        }

        @Override
        public void accept(DataPacket<Long> packet) {
            // pass-through: ownership of the detached buffer transfers to the active IN_DATA owner
            publishSync(ClientEvent.IN_DATA, packet.getBuffer());
        }
    }

    private class Closed extends TriggerConsumer<Throwable> {
        Closed() {
            super(SMProtoUtil.BasicEvent.CLOSED);
        }

        @Override
        public void accept(Throwable t) {
            if (log.isEnabled()) log.getLogger().info(getStateMachine().getName() + " closed: " + t);
        }
    }
}
