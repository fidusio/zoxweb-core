package org.zoxweb.server.net;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;


public class ChannelOutputStream
        extends BaseChannelOutputStream {

    public ChannelOutputStream(ProtocolHandler ph, ByteChannel byteChannel, int outAppBufferSize) throws IOException{
        super(ph, byteChannel, outAppBufferSize);
    }


    /**
     * @param bb buffer sent over the wire
     * @return the number of byte sent
     * @throws IOException in case of error
     */
    public synchronized int write(ByteBuffer bb) throws IOException {
        try {
            int ret = ByteBufferUtil.smartWrite(null, outChannel, bb);
            protocolHandler.updateUsage();
            return ret;
        } catch (IOException e) {
            IOUtil.close(this);
            throw e;
        }
    }

    public void close() throws IOException {

        if (!isClosed.getAndSet(true)) {
            if (log.isEnabled()) log.getLogger().info("Calling close");
            IOUtil.close(outChannel, protocolHandler);
            ByteBufferUtil.cache(outAppData);
            //ByteBufferUtil.cache(oneByteBuffer);
        }
    }

}
