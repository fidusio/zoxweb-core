package org.zoxweb.server.net;

import java.io.Closeable;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
        implements Closeable
{
    private InetAddress remoteAddress;

    private ProtocolHandler protocolHandler;
    public abstract BaseChannelOutputStream get();

    //public ProtocolHandler getProtocolHandler() {
    //    return protocolHandler;
    //}

    public void setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }


    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
}
