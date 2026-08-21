package org.zoxweb.server.net.ssl;

import java.io.IOException;

/**
 * Handshake-completion target: where {@code SSLUtil._finished} lands once the engine reports
 * {@code FINISHED}, routed via {@link SSLConnectionHelper#notifySSLHandshakeFinished()}.
 * Implementations: {@code SSLNIOSocketHandler} (server transport — the tunnel hook, lazy
 * opener of a tunnel's remote leg) and {@code TCPSessionCallback} (plain client —
 * {@code connectedFinished()}); the validator machine reaches its {@code SSLClientBridge}
 * equivalent through {@code ClientSSLHelper} without implementing this interface.
 */
public interface SSLHandshakeFinished {
    /**
     * Called exactly once per session, on the handshake worker, when the TLS handshake has
     * completed. A thrown exception is terminal — {@code SSLUtil._finished} closes the session.
     *
     * @param sslConfigInt the session whose handshake finished
     * @throws IOException on completion failure — the session will be closed
     */
    void sslHandshakeSuccessful(SSLConfigInt sslConfigInt) throws IOException;
}
