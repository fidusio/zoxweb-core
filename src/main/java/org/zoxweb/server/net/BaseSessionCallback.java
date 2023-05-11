package org.zoxweb.server.net;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
{



    private ProtocolHandler protocolHandler;
    public abstract BaseChannelOutputStream get();

    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public void setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
}
