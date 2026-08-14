package org.zoxweb.server.net.common.sm;

/**
 * Event vocabulary of the client-connection state machine, published as bare enum names
 * (canonical ID = {@code name()}).
 * <p>
 * Reserved canonical IDs on a {@link ClientConSM} — application vocabularies sharing the
 * machine must avoid all of them (one machine per session, enforced by the TCPSMCallback
 * constructor, keeps this a per-session concern):
 * <ul>
 * <li>{@code CONNECTED}, {@code RAW_IN_DATA}, {@code CLOSED} —
 * {@link TCPSMCallback.BasicEvent}; {@code RAW_IN_DATA} is consumed
 * exclusively by the transport router ({@link ClientTransportState}), never by phases.</li>
 * <li>{@code NEED_WRAP}, {@code NEED_UNWRAP}, {@code NEED_UNWRAP_AGAIN}, {@code NEED_TASK},
 * {@code FINISHED}, {@code NOT_HANDSHAKING} — {@link javax.net.ssl.SSLEngineResult.HandshakeStatus}
 * names, routed to the SSL handshake states.</li>
 * <li>the five members of this enum.</li>
 * </ul>
 */
public enum ClientEvent {
    /**
     * Application bytes, whatever the transport: payload is a detached read-mode
     * {@link java.nio.ByteBuffer}; exactly one active owner consumes it and recaches it via
     * {@code ByteBufferUtil.cache} when done. Distinct from {@code RAW_IN_DATA} (wire bytes) —
     * republishing decrypted bytes as {@code RAW_IN_DATA} would re-enter the transport router
     * and feed plaintext back into the SSL engine.
     */
    IN_DATA,
    /**
     * TLS handshake completed and the session output stream now encrypts: payload is the
     * session's {@link org.zoxweb.server.net.ssl.SSLConfigInt}. Gates the first encrypted write.
     */
    SECURE,
    /**
     * The connection initialization pipeline finished (published exactly once per session):
     * payload null. The application's "you may write now" gate.
     */
    READY,
    /**
     * Start the TLS upgrade: payload null. Published by the SSL phase itself in IMMEDIATE mode,
     * or by a protocol negotiator after its go-ahead (which must have verified that no residue
     * followed the go-ahead line — residue is fatal, see the STARTTLS injection contract).
     */
    START_TLS,
    /**
     * A protocol banner was received and validated (e.g. SSH identification line): payload is
     * the banner {@link String} without the line terminator.
     */
    BANNER_RECEIVED,
}
