package org.zoxweb.server.net;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseChannelOutputStream extends OutputStream
        implements CloseableType {
    public static final LogWrapper log = new LogWrapper(BaseChannelOutputStream.class).setEnabled(false);


    protected final ByteChannel outChannel;
    protected final ByteBuffer outAppData;
    protected final byte[] oneByteBuffer = new byte[1];
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    protected final ProtocolHandler protocolHandler;

    protected BaseChannelOutputStream(ProtocolHandler ph, ByteChannel outByteChannel, int outAppBufferSize) {
        SUS.checkIfNulls("Protocol handler or channel can't be null ", ph, outByteChannel);
        this.outChannel = outByteChannel;
        if (outAppBufferSize > 0) {
            outAppData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, outAppBufferSize);
            this.protocolHandler = ph;
        } else
            throw new IllegalArgumentException("Invalid buffer size");
    }


    @Override
    public synchronized void write(int b) throws IOException {
        oneByteBuffer[0] = (byte) b;
        write(oneByteBuffer, 0, 1);
    }

    public void write(UByteArrayOutputStream ubaos, boolean reset) throws IOException {
        synchronized (ubaos) {
            write(ubaos.getInternalBuffer(), 0, ubaos.size());
            if (reset)
                ubaos.reset();
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (off > b.length || len > (b.length - off) || off < 0 || len < 0)
            throw new IndexOutOfBoundsException();
        // len == 0 condition implicitly handled by loop bounds
        while (off < len) {
            int tempLen = len - off;
            if (tempLen > (outAppData.capacity() - outAppData.position()))
                tempLen = outAppData.capacity() - outAppData.position();


            outAppData.put(b, off, tempLen);
            write(outAppData);
            off += tempLen;
        }
    }

    public abstract int write(ByteBuffer byteBuffer) throws IOException;

    public boolean isClosed() {
        return isClosed.get() || !outChannel.isOpen();
    }
}
