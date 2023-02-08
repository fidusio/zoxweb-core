package org.zoxweb.server.net.ssl;


import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.BaseSessionCallback;

public abstract class SSLSessionCallback extends BaseSessionCallback<SSLSessionConfig>
{

    public final BaseChannelOutputStream get()
    {
        return getConfig().sslOutputStream;
    }
}
