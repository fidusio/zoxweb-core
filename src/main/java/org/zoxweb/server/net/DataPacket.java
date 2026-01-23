package org.zoxweb.server.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class DataPacket {
    private final ByteBuffer buffer;
    private final InetSocketAddress address;

    public DataPacket(InetSocketAddress sa, ByteBuffer buffer) {
        this.address = sa;
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
