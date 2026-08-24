package org.zoxweb.server.net;

import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.util.Identifier;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

/**
 * Base class for byte-stream session callbacks: a {@link SessionCallback} consuming
 * {@link ByteBuffer}s and supplying an {@link OutputStream}, with the common session plumbing —
 * channel and {@link BaseChannelOutputStream}. Remote address, the optional
 * {@link ProtocolHandler} and delegate-backed close/isClosed are inherited from
 * {@link SessionCallback}.
 * <p>
 * This is the base of the SSL/protocol session callbacks, whose data unit is the raw or decrypted
 * ByteBuffer delivered via {@code accept(ByteBuffer)}. Callbacks that consume a different data
 * unit (e.g. a DataPacket consumer) extend {@link SessionCallback} directly
 * instead: Java's erasure forbids inheriting {@code accept(ByteBuffer)} from this class alongside
 * another {@code accept(D)} of the same erasure.
 *
 * @param <CF> the session configuration type
 */
public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
        implements CloseableType, Identifier<String> {
    private volatile BaseChannelOutputStream bcos;
    private volatile ByteChannel channel;

    protected String instanceID = null;


    /**
     * Connection establishment hook, invoked by NIOSocket once the connection is fully
     * established and before any read dispatch.
     *
     * @param key the session's selection key
     * @return the selection interest ops to install, the protocol handler's if one is set,
     * OP_READ otherwise
     * @throws IOException in case of error
     */
    public int connected(SelectionKey key) throws IOException {
        return protocolHandler != null ? protocolHandler.interestOps() : SelectionKey.OP_READ;
    }

    /**
     * @return the session instance id, null if never set
     */
    @Override
    public String getID() {
        return instanceID;
    }

    /**
     * Sets the session instance id.
     *
     * @param instanceID the session instance id
     */
    public void setID(String instanceID) {
        this.instanceID = instanceID;
    }

    /**
     * @return the session output stream, same as {@link #getOutputStream()}
     */
    public BaseChannelOutputStream get() {
        return bcos;
    }

    /**
     * Sets the session output stream.
     *
     * @param bcos the session output stream
     * @return this
     */
    public BaseSessionCallback<?> setOutputStream(BaseChannelOutputStream bcos) {
        this.bcos = bcos;
        return this;
    }

    /**
     * @return the session output stream, null until set
     */
    public BaseChannelOutputStream getOutputStream() {
        return bcos;
        ///return bcos != null ? bcos : get();
    }

    /**
     * @return the session channel, falls back to the output stream's data channel when no channel
     * was set directly
     */
    public <V extends Channel> V getChannel() {
        return channel != null ? (V) channel : (getOutputStream() != null ? (V) getOutputStream().dataChannel : null);
    }

    /**
     * Binds the session's channel.
     *
     * @param channel the session's ByteChannel
     */
    public void setChannel(Channel channel) {
        this.channel = (ByteChannel) channel;
    }

    /**
     * @return the session's protocol handler, null if none
     */
    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    /**
     * Sets the session's protocol handler.
     *
     * @param protocolHandler the protocol handler
     * @return this
     */
    public BaseSessionCallback<?> setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
        return this;
    }

    /**
     * @return true if the session was closed, backed by the closeable delegate
     */
    @Override
    public boolean isClosed() {
        return closeableDelegate.isClosed();
    }

    /**
     * Closes the session once via the closeable delegate, subsequent calls are no-ops.
     *
     * @throws IOException in case of error
     */
    @Override
    public void close() throws IOException {
        closeableDelegate.close();
    }


}
