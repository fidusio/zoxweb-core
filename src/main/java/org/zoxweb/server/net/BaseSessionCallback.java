package org.zoxweb.server.net;

import org.zoxweb.shared.util.CloseableType;

import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;

public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
        implements CloseableType {
    private SocketAddress remoteAddress;
    private transient BaseChannelOutputStream bcos;
    private transient ByteChannel channel;

    public abstract BaseChannelOutputStream get();



    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }



    public int connected(SelectionKey key) {return key.interestOps();}



    public BaseSessionCallback<?> setOutputStream(BaseChannelOutputStream bcos) {
        this.bcos = bcos;
        return this;
    }
    public BaseChannelOutputStream getOutputStream() { return bcos != null ? bcos : get();}

    public ByteChannel getChannel() {
        return channel != null ? channel : (getOutputStream() != null ? getOutputStream().dataChannel : null);
    }

    public BaseSessionCallback<?> setChannel(ByteChannel channel) {
        this.channel = channel;
        return this;
    }

    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public BaseSessionCallback<?> setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
        return this;
    }


}
