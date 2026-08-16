package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.StateMachine;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

/**
 * Per-connection client state machine, set as the config of a
 * {@link TCPSMCallback}: it dictates what happens after socket
 * connect — TLS handshake, protocol banner validation, STARTTLS-style upgrade — based on the
 * phases it was built with ({@link ClientConSMBuilder}).
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
public class ClientConSM extends StateMachine<ClientSessionContext> {


    ClientConSM(String name) {
        super(name, (Executor) null);
    }

    /**
     * @return the per-session context (alias of {@link #getConfig()})
     */
    public ClientSessionContext getContext() {
        return getConfig();
    }

    /**
     * Creates the TCP session callback for this machine and binds it to the context; hand the
     * result to {@code NIOSocket.addClientSocket}.
     *
     * @return the bound session callback
     * @throws IllegalStateException if the machine transport is not TCP
     */
    public TCPSMCallback newSessionCallback() {
        checkTransport(ClientSessionContext.Transport.TCP);
        TCPSMCallback cb = new TCPSMCallback(this);
        getContext().bind(cb);
        return cb;
    }

    /**
     * Creates the TCP session callback with a caller-supplied id and binds it to the context.
     *
     * @param id the session id
     * @return the bound session callback
     * @throws IllegalStateException if the machine transport is not TCP
     */
    public TCPSMCallback newSessionCallback(String id) {
        checkTransport(ClientSessionContext.Transport.TCP);
        TCPSMCallback cb = new TCPSMCallback(id, this);
        getContext().bind(cb);
        return cb;
    }

    /**
     * Creates the UDP session callback targeting {@code remote} and binds it to the context; hand
     * the result to {@code NIOSocket.addDatagramSocket} with an ephemeral local bind (e.g.
     * {@code new InetSocketAddress(0)}).
     *
     * @param remote the peer the datagram channel connects to
     * @return the bound session callback
     * @throws IllegalStateException if the machine transport is not UDP
     */
    public UDPSMCallback newSessionCallback(InetSocketAddress remote) {
        checkTransport(ClientSessionContext.Transport.UDP);
        UDPSMCallback cb = new UDPSMCallback(remote, this);
        getContext().bind(cb);
        return cb;
    }

    // before the callback constructor mutates the machine's properties, so a transport
    // mismatch leaves no half-bound state behind
    private void checkTransport(ClientSessionContext.Transport expected) {
        if (getContext().getTransport() != expected)
            throw new IllegalStateException("machine transport is " + getContext().getTransport()
                    + ": " + expected + " session callback rejected");
    }
}
