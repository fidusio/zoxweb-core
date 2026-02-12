package org.zoxweb.shared.io;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link CloseableType} implementation that delegates close operations to an underlying
 * {@link AutoCloseable} resource. This class ensures thread-safe, one-time closing semantics
 * using an {@link AtomicBoolean} guard.
 */
public class CloseableTypeDelegate implements CloseableType {
    private volatile AutoCloseable delegate;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * Creates a new CloseableTypeDelegate wrapping the given resource.
     *
     * @param delegate the AutoCloseable resource to delegate close operations to
     */
    public CloseableTypeDelegate(AutoCloseable delegate) {
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
     */
    @Override
    public void close(){
        if (delegate != null) {
            if (!isClosed.getAndSet(true)) {
                try {
                    // DO NOT USE IOUtil here it is not available
                    delegate.close();
                } catch (Exception e) {

                }

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

    /**
     * Sets the delegate if it has not already been assigned. This method is synchronized
     * to prevent concurrent reassignment.
     *
     * @param delegate the Closeable resource to set as the delegate
     * @return true if the delegate was set, false if a delegate was already present or the argument is null
     */
    public synchronized boolean setDelegate(Closeable delegate) {
        if (this.delegate == null && delegate != null) {
            this.delegate = delegate;
            return true;
        }
        return false;
    }
}
