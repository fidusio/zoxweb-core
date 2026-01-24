package org.zoxweb.server.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class DataPacket<I> {
    private final ByteBuffer buffer;
    private final InetSocketAddress address;
    private final I id;


    public DataPacket(InetSocketAddress sa, ByteBuffer buffer) {
        this(null, sa,  buffer);
    }

    public DataPacket(I id, InetSocketAddress sa, ByteBuffer buffer) {
        this.address = sa;
        this.buffer = buffer;
        this.id = id;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public I getID() {return id;}
}
