package org.zoxweb.server.net.ssl;

import org.zoxweb.shared.util.GetConfig;

import javax.net.ssl.SSLEngineResult;

public interface SSLConnectionHelper
        extends GetConfig<SSLSessionConfig> {
    void publish(SSLEngineResult.HandshakeStatus status, SSLSessionCallback callback);

    void createRemoteConnection();

    SSLSessionConfig getConfig();
}
