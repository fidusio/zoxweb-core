/**
 * Meta-driven client-connection state machine framework: a {@link ClientConSM}
 * set as the config of a {@link TCPSMCallback} / {@link UDPSMCallback} dictates what
 * happens after socket connect — scripted request/response dialogue, protocol validation,
 * immediate TLS handshake, or a STARTTLS-style plaintext-negotiate-then-upgrade — <b>composed
 * from predefined catalog states</b>, programmatically ({@link ClientConSMBuilder}) or from a
 * JSON config ({@link org.zoxweb.server.net.common.sm.ClientSMFactory}'s state catalog). Each
 * state is configured by its own properties bag and is a set of TriggerConsumers; states
 * coordinate through the machine's shared bag (the blackboard), never through instance
 * references. The catalog: {@link org.zoxweb.server.net.common.sm.MessageAssemblerState
 * assembler} (boundary strategies), {@link org.zoxweb.server.net.common.sm.ProtocolControllerState
 * controller} (the linear exchange script), {@link org.zoxweb.server.net.common.sm.ResponseControllerState
 * responder}, {@link org.zoxweb.server.net.common.sm.ProtocolTypeValidatorState validator}
 * (verdict → results bag), {@link org.zoxweb.server.net.common.sm.SSLClientState ssl}.
 *
 * <h2>TLS orchestration (SSLStateMachineV2)</h2>
 * The machine owns the handshake orchestration: {@link org.zoxweb.server.net.common.sm.SSLClientHandshakeState}
 * and {@link org.zoxweb.server.net.common.sm.SSLClientDataState} route
 * {@code SSLEngineResult.HandshakeStatus} canonical IDs to the load-proven
 * {@code SSLUtil._needWrap/_needUnwrap/_needTask/_finished/_notHandshaking} handlers —
 * orchestration only, zero changes to the engine steps. The
 * {@link org.zoxweb.server.net.common.sm.ClientSSLHelper} is the session's
 * {@code SSLConnectionHelper} whose {@code close()} is a deliberate no-op:
 * {@code SSLSessionConfig.close()} closes its helper during session teardown BEFORE the
 * {@code CLOSED} event publishes, so a helper that closed the machine would silently lose
 * {@code CLOSED}. The {@link org.zoxweb.server.net.common.sm.SSLClientBridge} is the
 * handler-facing {@code BaseSessionCallback<SSLSessionConfig>} (TCPSMCallback deliberately is
 * not one).
 *
 * <h2>Threading</h2>
 * Machines are always synchronous: NIOSocket serializes dispatches per session by zeroing the
 * key's interest ops, and the handshake flight is recursive inline publication — an executor
 * would interleave engine steps. Async work escapes explicitly (TaskUtil) and re-enters by
 * publishing.
 *
 * <h2>Buffer ownership (each buffer cached exactly once)</h2>
 * <table border="1">
 * <caption>owners</caption>
 * <tr><th>Buffer</th><th>Recached by</th></tr>
 * <tr><td>TCPSMCallback {@code rawReadBuffer}</td><td>TCPSMCallback close delegate (distinct
 * from all SSL buffers — the upgrade always calls {@code beginHandshake(null)})</td></tr>
 * <tr><td>SSL {@code IOBuffers} (net in/out) / {@code inDecryptedData} /
 * {@code inRemoteData}</td><td>{@code SSLSessionConfig.close()}, once (AtomicBoolean)</td></tr>
 * <tr><td>each {@code RAW_IN_DATA} packet</td><td>the transport router (TLS modes) or the one
 * active {@code IN_DATA} owner (plain pass-through)</td></tr>
 * <tr><td>each decrypted {@code IN_DATA} copy</td><td>the consuming state or application</td></tr>
 * </table>
 * Applications register their {@code IN_DATA} consumer from their {@code READY} handler (late
 * registration is supported while the machine is operational) — earlier registration would
 * receive pre-{@code READY} negotiation bytes on the broadcast.
 *
 * <h2>STARTTLS injection rule</h2>
 * A negotiator publishing {@link CommonTrigger#START_TLS} must
 * first verify that NO byte followed its go-ahead line in the triggering packet: such residue
 * is attacker-controllable plaintext (the classic STARTTLS injection class) and is fatal —
 * fail the session, never clear-and-continue.
 */
package org.zoxweb.server.net.common.sm;
