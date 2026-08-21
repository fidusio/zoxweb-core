package org.zoxweb.server.net.ssl;

import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.util.GetConfig;

import javax.net.ssl.SSLEngineResult;
import java.io.IOException;

public interface SSLConnectionHelper<C>
        extends GetConfig<C> {
    void publish(SSLEngineResult.HandshakeStatus status, BaseSessionCallback<C> callback);

    /**
     * Handshake-completion routing, invoked unconditionally by {@code SSLUtil._finished}
     * (every dispatcher installs itself as the session's helper before any publish, so the
     * helper is always present). Implementations deliver the event to the session's
     * {@link SSLHandshakeFinished} target — tunnel hook (server), {@code connectedFinished}
     * (plain client), SECURE/READY gate (validator machine). A thrown exception is terminal:
     * {@code _finished} closes the session and there is no continuation.
     *
     * @throws IOException on completion failure — the session will be closed
     */
    void notifySSLHandshakeFinished() throws IOException;

    C getConfig();
}
