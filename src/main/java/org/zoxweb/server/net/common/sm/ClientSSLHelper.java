package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.net.ssl.SSLConnectionHelper;
import org.zoxweb.server.net.ssl.SSLSessionConfig;

import javax.net.ssl.SSLEngineResult;
import java.io.Closeable;

/**
 * The session's {@link SSLConnectionHelper}: routes handshake statuses into the client machine
 * as canonical-ID publishes (enum name = ID), where {@link SSLClientHandshakeState} /
 * {@link SSLClientDataState} consume them.
 * <p>
 * <b>Load-bearing: {@link #close()} is a no-op and must stay one.</b>
 * {@code SSLSessionConfig.close()} closes its {@code sslConnectionHelper} (unconditional
 * {@code AutoCloseable} cast — hence {@code Closeable} here), and that happens during session
 * teardown BEFORE the {@code CLOSED} event is published. If this helper closed the machine,
 * the publish would throw on a closed machine and {@code CLOSED} would be silently lost.
 * The machine is closed exclusively by TCPSMCallback's close delegate, last.
 */
class ClientSSLHelper implements SSLConnectionHelper, Closeable {

    private final ClientConSM sm;

    ClientSSLHelper(ClientConSM sm) {
        this.sm = sm;
    }

    @Override
    public void publish(SSLEngineResult.HandshakeStatus status, BaseSessionCallback<SSLConfigInt> callback) {
        // guard instead of throw: the config-close drain publishes after teardown started
        if (!sm.isClosed())
            sm.publishSync(status, callback);
    }

    @Override
    public void createRemoteConnection() {
        // client session, no tunnel
    }

    @Override
    public SSLSessionConfig getConfig() {
        return sm.getContext().getSSLConfig();
    }

    @Override
    public void close() {
        // no-op by contract — never closes the machine, see class javadoc
    }
}
