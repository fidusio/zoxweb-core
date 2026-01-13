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

    public abstract BaseChannelOutputStream get();


    public void setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }


    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public ByteChannel getChannel() {
        if (get() != null) {
            return get().dataChannel;
        }
        return null;
    }

    public void connected(SelectionKey key) {};
}
