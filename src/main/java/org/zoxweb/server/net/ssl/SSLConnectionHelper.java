package org.zoxweb.server.net.ssl;

import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.util.GetConfig;

import javax.net.ssl.SSLEngineResult;

public interface SSLConnectionHelper
        extends GetConfig<SSLSessionConfig> {
    void publish(SSLEngineResult.HandshakeStatus status, BaseSessionCallback<SSLSessionConfig> callback);

    void createRemoteConnection();

    SSLSessionConfig getConfig();
}
