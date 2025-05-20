package org.zoxweb.server.io;

import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.CloseableTypeHolder;
import org.zoxweb.shared.util.NadaInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.zoxweb.shared.util.Const.EMPTY_BYTE_ARRAY;

public class UByteArrayInputStream
    extends ByteArrayInputStream
    implements CloseableType {

    public static final UByteArrayInputStream EMPTY_INPUT_STREAM = new UByteArrayInputStream(EMPTY_BYTE_ARRAY);

    private final CloseableTypeHolder cth;

    /**
     * Creates a {@code UByteArrayInputStream}
     * so that it  uses {@code buf} as its
     * buffer array.
     * The buffer array is not copied.
     * The initial value of {@code pos}
     * is {@code 0} and the initial value
     * of  {@code count} is the length of
     * {@code buf}.
     *
     * @param   buf   the input buffer.
     */
    public UByteArrayInputStream(byte[] buf) {
        this(buf, 0, buf.length, null);
    }

    /**
     * Creates {@code UByteArrayInputStream}
     * that uses {@code buf} as its
     * buffer array. The initial value of {@code pos}
     * is {@code offset} and the initial value
     * of {@code count} is the minimum of {@code offset+length}
     * and {@code buf.length}.
     * The buffer array is not copied. The buffer's mark is
     * set to the specified offset.
     *
     * @param   buf      the input buffer.
     * @param   offset   the offset in the buffer of the first byte to read.
     * @param   length   the maximum number of bytes to read from the buffer.
     */

    public UByteArrayInputStream(byte[] buf, int offset, int length) {
        this(buf, offset, length, null);
    }

    public UByteArrayInputStream(byte[] buf,Runnable afterClose) {
        this(buf, 0, buf.length, afterClose);
    }

    public UByteArrayInputStream(byte[] buf, int offset, int length, Runnable afterClose) {
        super(buf, offset, length);
        cth = afterClose != null ? new CloseableTypeHolder(afterClose) : new CloseableTypeHolder((Runnable) NadaInstance::nada);
    }


    public void close() throws IOException {
        cth.close();
    }

    /**
     * Checks if closed.
     *
     * @return true if closed
     */
    @Override
    public boolean isClosed() {
        return cth.isClosed();
    }


}
