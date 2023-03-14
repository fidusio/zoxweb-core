package org.zoxweb.server.net.ssl;

import javax.net.ssl.SSLEngineResult;

public interface SSLDispatcher
{
    void dispatch(SSLEngineResult.HandshakeStatus status, SSLSessionCallback callback);
//    void createRemoteConnection();
}
