package org.zoxweb.server.net.common.sm;

/**
 * Event vocabulary of the client-connection state machine, published as bare enum names
 * (canonical ID = {@code name()}).
 * <p>
 * Reserved canonical IDs on a {@link ClientConSM} — application vocabularies sharing the
 * machine must avoid all of them (one machine per session, enforced by the TCPSMCallback
 * constructor, keeps this a per-session concern):
 * <ul>
 * <li>{@code CONNECTED}, {@code IN_RAW_DATA}, {@code DATAGRAM}, {@code CLOSED} —
 * {@link SMProtoUtil.BasicEvent}; {@code IN_RAW_DATA} / {@code DATAGRAM} are consumed
 * exclusively by the transport router ({@link ClientTransportState}), never by other states.</li>
 * <li>{@code NEED_WRAP}, {@code NEED_UNWRAP}, {@code NEED_UNWRAP_AGAIN}, {@code NEED_TASK},
 * {@code FINISHED}, {@code NOT_HANDSHAKING} — {@link javax.net.ssl.SSLEngineResult.HandshakeStatus}
 * names, routed to the SSL handshake states.</li>
 * <li>the seven members of this enum.</li>
 * </ul>
 * Naming convention (META-SM-PROTO-DESIGN.md §6): facts are nouns on the inbound data ladder
 * ({@code IN_DATA} → {@code IN_MESSAGE}), commands are imperatives ({@code VALIDATE},
 * {@code START_TLS}).
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
     * One complete protocol message, as framed by the assembler's boundary strategy
     * (META-SM-PROTO-DESIGN.md §7): payload is the message bytes as a detached {@code byte[]}
     * copy — consumer-owned, no pooled-buffer bookkeeping. Published by the assembler state,
     * consumed by the controller state.
     */
    IN_MESSAGE,
    /**
     * A message to transmit: payload is the decoded bytes ({@code byte[]}, {@code ${var}}s
     * already resolved). Published by the controller state; the responder state writes it to the
     * session via {@code ClientSessionContext.write} — no other state touches the transport.
     */
    OUT_MESSAGE,
    /**
     * Validate the current message: payload carries the message plus the {@code validate} match
     * meta (prefix / contains / exact / report — META-SM-PROTO-DESIGN.md §8). Published by the
     * controller state, consumed by the validator state, which writes its verdict
     * ({@code validated} / {@code reason} / optional report key) into the machine results bag —
     * there is no event back (VALIDATED was rejected by design).
     */
    VALIDATE,
}
