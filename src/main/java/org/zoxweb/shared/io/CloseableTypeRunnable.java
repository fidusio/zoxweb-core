package org.zoxweb.shared.io;

import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloseableTypeRunnable
        implements CloseableType {

    private final Runnable runnable;
    // private final Closeable reference;
    private final AtomicBoolean status;


    public CloseableTypeRunnable(Runnable runnable) {
        SUS.checkIfNulls("Null closeable", runnable);
        this.runnable = runnable;
        //this.reference = null;
        status = new AtomicBoolean(false);
    }


//    public CloseableTypeRunnable(Closeable ref) {
//        SUS.checkIfNulls("Null closeable", ref);
//        this.reference = ref;
//        if (reference instanceof CloseableType)
//            status = null;
//        else
//            status = new AtomicBoolean(false);
//
//        runnable = null;
//    }


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
        if (status != null) {
            if (!status.getAndSet(true)) {
//                if (reference != null)
//                    reference.close();
//                else
                runnable.run();
            }

        }
//        else
//            reference.close();
    }

    /**
     * Checks if closed.
     *
     * @return true if closed
     */
    @Override
    public boolean isClosed() {
//        if (status != null)
        return status.get();

//        return ((CloseableType) reference).isClosed();
    }
}
