package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.StateMachine;

import java.util.concurrent.Executor;

/**
 * Per-connection client state machine, set as the config of a
 * {@link TCPSMCallback}: it dictates what happens after socket
 * connect — TLS handshake, protocol banner validation, STARTTLS-style upgrade — based on the
 * phases it was built with ({@link ClientConnectionSMBuilder}).
 * <p>
 * <b>Always synchronous.</b> Constructed with a null executor so every publish runs inline on
 * the calling thread: NIOSocket serializes dispatches per session by zeroing the key's interest
 * ops, and the SSL handshake flight is recursive inline publication — an async executor would
 * interleave engine steps across threads. A consumer needing real async work hands off
 * explicitly (e.g. TaskUtil) and re-enters by publishing.
 * <p>
 * The machine is never {@code start()}ed — it is driven by the session callback's publishes
 * ({@code CONNECTED}, {@code RAW_IN_DATA}, {@code CLOSED}) plus internal republication — and it
 * is session-owned: TCPSMCallback teardown closes it last, after {@code CLOSED} is delivered.
 */
public class ClientConnectionSM extends StateMachine<ClientSessionContext> {

    ClientConnectionSM(String name) {
        super(name, (Executor) null);
    }

    /**
     * @return the per-session context (alias of {@link #getConfig()})
     */
    public ClientSessionContext getContext() {
        return getConfig();
    }

    /**
     * Creates the session callback for this machine and binds it to the context; hand the
     * result to {@code NIOSocket.addClientSocket}.
     *
     * @return the bound session callback
     */
    public TCPSMCallback newSessionCallback() {
        TCPSMCallback cb = new TCPSMCallback(this);
        getContext().bind(cb);
        return cb;
    }

    /**
     * Creates the session callback with a caller-supplied id and binds it to the context.
     *
     * @param id the session id
     * @return the bound session callback
     */
    public TCPSMCallback newSessionCallback(String id) {
        TCPSMCallback cb = new TCPSMCallback(id, this);
        getContext().bind(cb);
        return cb;
    }
}
