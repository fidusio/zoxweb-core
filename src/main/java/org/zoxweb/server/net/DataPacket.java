package org.zoxweb.server.net;

import org.zoxweb.server.io.IOBuffers;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.SUS;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/**
 * A datagram unit pairing an {@link IOBuffers} pair with its peer address: the source of a
 * received packet or the destination of an outbound one, plus an optional identifier (e.g. a
 * read counter on the UDP receive path). The payload is the pair's in-buffer, reached through
 * {@link #getIOBuffers()}; the {@link ByteBuffer} constructor wraps a single buffer into a pair.
 * A packet may also carry the {@link Channel} it arrived on or is bound for; it is optional and
 * null on every {@link ByteBuffer} constructor path.
 * <p>
 * The buffers are held by reference, not copied, and on the receive path the payload is a pooled
 * buffer that the dispatcher recaches after the callback returns — do not retain it beyond that
 * scope. To keep the payload past the dispatch, copy it out before returning.
 *
 * @param <I> the identifier type
 */
public class DataPacket<I>
        implements Identifier<I> {
    private final IOBuffers ioBuffers;
    private final Channel channel;
    private final InetSocketAddress address;
    private final I id;


    /**
     * Creates a packet with no identifier.
     *
     * @param sa     the peer address, source or destination of the packet
     * @param buffer the payload, held by reference not copied
     */
    public DataPacket(InetSocketAddress sa, ByteBuffer buffer) {
        this(null, sa, buffer);
    }

    /**
     * Creates a packet.
     *
     * @param id     the packet identifier, can be null
     * @param sa     the peer address, source or destination of the packet
     * @param buffer the payload, held by reference not copied
     */
    public DataPacket(I id, InetSocketAddress sa, ByteBuffer buffer) {
        this(id, null, sa, new IOBuffers().setInBuffer(buffer));
    }

    /**
     * Creates a packet over an existing buffer pair.
     *
     * @param id        the packet identifier, can be null
     * @param channel   the channel the packet arrived on or is bound for, can be null
     * @param sa        the peer address, source or destination of the packet
     * @param ioBuffers the buffer pair, held by reference not copied; its in-buffer is the payload
     */
    public DataPacket(I id, Channel channel, InetSocketAddress sa, IOBuffers ioBuffers) {
        SUS.checkIfNulls("Null ioBuffers or sa", ioBuffers, sa);
        this.address = sa;
        this.channel = channel;
        this.id = id;
        this.ioBuffers = ioBuffers;
    }



    /**
     * @return the packet's buffer pair, the live instance not a copy; the payload is
     * {@code getIOBuffers().getInBuffer()}, and on the receive path that buffer is pooled and
     * recached after dispatch — do not retain it beyond that scope
     */
    public IOBuffers getIOBuffers() {
        return ioBuffers;
    }


    /**
     * @return the channel the packet arrived on or is bound for, null when the packet was created
     * without one (every {@link ByteBuffer} constructor path)
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * @return the peer address, source of a received packet or destination of an outbound one
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * @return the packet identifier, null if none was set
     */
    public I getID() {
        return id;
    }
}
