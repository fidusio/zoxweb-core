package org.zoxweb.server.net.ssl;

import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.util.GetConfig;

import javax.net.ssl.SSLEngineResult;

public interface SSLConnectionHelper<C>
        extends GetConfig<C> {
    void publish(SSLEngineResult.HandshakeStatus status, BaseSessionCallback<C> callback);

    void createRemoteConnection();

    C getConfig();
}
