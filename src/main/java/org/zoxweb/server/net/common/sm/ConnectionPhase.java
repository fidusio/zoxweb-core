package org.zoxweb.server.net.common.sm;

import org.zoxweb.shared.util.GetName;

/**
 * A pluggable connection-initialization phase: contributes its state(s) and trigger consumers
 * to a {@link ClientConnectionSM} at build time.
 * <p>
 * Contract:
 * <ul>
 * <li><b>{@code RAW_IN_DATA} is owned exclusively by the transport router</b>
 * ({@link ClientTransportState}) — phases never register it. Routing is broadcast pub/sub; a
 * second consumer would double-consume wire bytes. Phases consume {@link ClientEvent#IN_DATA}
 * instead.</li>
 * <li>Broadcast order across states is registration order; the builder registers the transport
 * state first, then phases in declared order — so transport initialization always precedes a
 * phase's reaction to the same event.</li>
 * <li>Each {@code IN_DATA} buffer has exactly one active owner, which recaches it via
 * {@code ByteBufferUtil.cache} when done; a phase that has finished its work must ignore later
 * {@code IN_DATA} dispatches without touching the buffer.</li>
 * </ul>
 */
public interface ConnectionPhase extends GetName {

    /**
     * Registers this phase's state(s) and consumers on the machine; called once by the builder.
     *
     * @param sm the machine under construction
     */
    void contribute(ClientConnectionSM sm);

    /**
     * @return true if this phase gates {@link ClientEvent#READY} — the pipeline is not complete
     * until this phase reports {@code phaseComplete}
     */
    default boolean gatesReady() {
        return false;
    }
}
