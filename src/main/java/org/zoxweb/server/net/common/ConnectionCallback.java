package org.zoxweb.server.net.common;

import org.zoxweb.shared.task.ExceptionCallback;
import org.zoxweb.shared.util.CloseableType;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface ConnectionCallback<D>
        extends ExceptionCallback, CommonAcceptSK, CloseableType {
    /**
     * Called when incoming data or something to do
     * @param key the input argument
     */
    void accept(SelectionKey key);

    void accept(D byteBuffer) throws IOException;

    int connected(SelectionKey key) throws IOException;

    default void sslHandshakeSuccessful() {
    }

}