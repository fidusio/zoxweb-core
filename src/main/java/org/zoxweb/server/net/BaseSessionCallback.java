package org.zoxweb.server.net;

import org.zoxweb.shared.util.CloseableType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
        implements CloseableType {
    private InetSocketAddress remoteAddress;
    private transient BaseChannelOutputStream bcos;
    private transient ByteChannel channel;
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);


    ///public abstract BaseChannelOutputStream get();


    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }


    public int connected(SelectionKey key) throws IOException {
        return key.interestOps();
    }

    public BaseChannelOutputStream get() {
        return bcos;
    }

    public BaseSessionCallback<?> setOutputStream(BaseChannelOutputStream bcos) {
        this.bcos = bcos;
        return this;
    }

    public BaseChannelOutputStream getOutputStream() {
        return bcos;
        ///return bcos != null ? bcos : get();
    }

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


    public boolean isClosed() {
        return isClosed.get();
    }

}
