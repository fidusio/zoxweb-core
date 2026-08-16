package org.zoxweb.server.net.common.sm;

/**
 * The complete event vocabulary of the client-connection state machine, published as bare enum
 * names (canonical ID = {@code name()}).
 * <p>
 * Reserved canonical IDs on a {@link ClientConSM} — application vocabularies sharing the
 * machine must avoid all of them (one machine per session, enforced by the session-callback
 * constructors, keeps this a per-session concern):
 * <ul>
 * <li>the eleven members of this enum;</li>
 * <li>{@code NEED_WRAP}, {@code NEED_UNWRAP}, {@code NEED_UNWRAP_AGAIN}, {@code NEED_TASK},
 * {@code FINISHED}, {@code NOT_HANDSHAKING} — {@link javax.net.ssl.SSLEngineResult.HandshakeStatus}
 * names, routed to the SSL handshake states.</li>
 * </ul>
 * Naming convention (META-SM-PROTO-DESIGN.md §6): facts are nouns on the inbound data ladder
 * ({@code RAW_IN_DATA} → {@code IN_DATA} → {@code IN_MESSAGE}), commands are imperatives
 * ({@code VALIDATE}, {@code START_TLS}).
 */
public enum ClientEvent {
    /**
     * The single session kickoff, published exactly once from the callback's
     * {@code connected(SelectionKey)} — nothing happens on the machine before it. Payload is
     * transport-dependent: the {@link java.nio.channels.SelectionKey} over TCP, the remote
     * {@link java.net.InetSocketAddress} over UDP — a consumer of this event must be
     * payload-agnostic ({@code TriggerConsumer<Object>}).
     */
    CONNECTED,
    /**
     * Wire bytes from one TCP read — partial or complete, and plain, handshake, or encrypted:
     * payload is a detached read-mode {@link java.nio.ByteBuffer} copy. Consumed exclusively by
     * the transport router ({@link ClientTransportState}), which recaches it; no other state
     * ever registers this event.
     */
    RAW_IN_DATA,
    /**
     * One received datagram (UDP): payload is a {@code DataPacket} holding a detached copy of
     * the datagram. Consumed exclusively by the UDP transport router
     * ({@link UDPClientTransportState}); no other state ever registers this event.
     */
    DATAGRAM,
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
     * Start the TLS upgrade: payload null. Published by the ssl state itself in IMMEDIATE mode,
     * or by a negotiator (the controller's {@code start_tls} step) after its go-ahead — which
     * must have verified that no residue followed the go-ahead line; residue is fatal, see the
     * STARTTLS injection contract.
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
    /**
     * End of session, published exactly once from the callback's close delegate — whatever the
     * termination path (remote disconnect, fatal error, or the machine completing its run):
     * payload is the terminating {@link Throwable} (relayed via {@code Params.EXCEPTION}) or
     * null on a clean close. The report in the machine results bag is final when it fires; the
     * machine itself is closed <b>after</b> this publish, as teardown's last act.
     */
    CLOSED,
}
