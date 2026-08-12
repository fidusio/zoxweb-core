package org.zoxweb.server.net;

import org.zoxweb.shared.io.BytesArray;
import org.zoxweb.shared.util.Identifier;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * A datagram unit pairing a payload {@link ByteBuffer} with its peer address: the source of a
 * received packet or the destination of an outbound one, plus an optional identifier (e.g. a
 * read counter on the UDP receive path).
 * <p>
 * The buffer is held by reference, not copied, and on the receive path it is a pooled buffer
 * that the dispatcher recaches after the callback returns — do not retain it beyond that scope.
 * To keep the payload past the dispatch, use {@link #asBytesArray()}, which snapshots a
 * detached copy.
 *
 * @param <I> the identifier type
 */
public class DataPacket<I>
        implements Identifier<I> {
    private final ByteBuffer buffer;
    private final InetSocketAddress address;
    private final I id;
    private volatile BytesArray bytesArray = null;


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
        this.address = sa;
        this.buffer = buffer;
        this.id = id;
    }



    /**
     * @return the payload buffer, the live instance not a copy; on the receive path it is pooled
     * and recached after dispatch
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns the packet payload as a detached BytesArray, lazily copied from the buffer's
     * readable window [position, limit) on first call and cached; the buffer's position is not
     * altered. The copy outlives the buffer, so it stays usable after the buffer is recached.
     * Call it before consuming the buffer, otherwise the snapshot only holds what was left.
     *
     * @return a permanently valid copy of the packet data
     */
    public BytesArray asBytesArray() {
        if (bytesArray == null) {
            synchronized (this) {
                if (bytesArray == null) {
                    if (buffer.hasArray())
                        bytesArray = BytesArray.create(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
                    else {
                        byte[] data = new byte[buffer.remaining()];
                        buffer.duplicate().get(data);
                        bytesArray = new BytesArray(data);
                    }
                }
            }
        }
        return bytesArray;
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
