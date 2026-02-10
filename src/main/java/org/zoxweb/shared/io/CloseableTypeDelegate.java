package org.zoxweb.shared.io;

import org.zoxweb.server.io.IOUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloseableTypeDelegate implements CloseableType {
    private volatile Closeable delegate;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    public CloseableTypeDelegate(Closeable delegate) {
        this.delegate = delegate;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if(delegate != null) {
            if (!isClosed.getAndSet(true)) {
                IOUtil.close(delegate);
            }
        }
    }

    /**
     * Checks if closed.
     *
     * @return true if closed
     */
    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    public synchronized boolean setDelegate(Closeable delegate) {
        if(this.delegate == null && delegate != null) {
            this.delegate = delegate;
            return true;
        }
        return false;
    }
}
