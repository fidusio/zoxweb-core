package org.zoxweb.server.net.ssl;

import org.zoxweb.shared.io.CloseableType;

import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public interface SSLConfigInt
extends CloseableType {
    SSLEngine getSSLEngine();
    ByteChannel getChannel();
    ByteBuffer getSSLInboundBuffer();
    ByteBuffer getSSLOutboundBuffer();

    default int getApplicationBufferSize() {
        return getSSLEngine().getSession().getApplicationBufferSize();
    }
}
