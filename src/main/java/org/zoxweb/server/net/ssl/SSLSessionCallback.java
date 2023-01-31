package org.zoxweb.server.net.ssl;


import org.zoxweb.server.net.BaseSessionCallback;

import java.io.OutputStream;

public abstract class SSLSessionCallback extends BaseSessionCallback<SSLSessionConfig>
{

    public final OutputStream get()
    {
        return getConfig().sslOutputStream;
    }
}
