package org.zoxweb.server.net;

import org.zoxweb.shared.io.CloseableTypeDelegate;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.util.Identifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
        implements CloseableType, Identifier<String> {
    private InetSocketAddress remoteAddress;
    private volatile BaseChannelOutputStream bcos;
    private volatile ByteChannel channel;
    //protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    protected final CloseableTypeDelegate closeableDelegate= new CloseableTypeDelegate(null, false);
    protected String instanceID = null;


    ///public abstract BaseChannelOutputStream get();


    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setRemoteAddress(IPAddress ipAddress) {
        this.remoteAddress = new InetSocketAddress(ipAddress.getInetAddress(),  ipAddress.getPort());
    }


    public int connected(SelectionKey key) throws IOException {
        return protocolHandler !=null ? protocolHandler.interestOps() : SelectionKey.OP_READ ;
    }

    @Override
    public String getID() {
        return instanceID;
    }

    public void setID(String instanceID) {
        this.instanceID = instanceID;
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

    public <V extends Channel> V getChannel() {
        return channel != null ? (V) channel : (getOutputStream() != null ? (V) getOutputStream().dataChannel : null);
    }

    public void setChannel(Channel channel) {
        this.channel = (ByteChannel) channel;
    }

    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public BaseSessionCallback<?> setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
        return this;
    }

    @Override
    public boolean isClosed() {
        return closeableDelegate.isClosed();
    }

    @Override
    public void close() throws IOException {
        closeableDelegate.close();
    }


}
