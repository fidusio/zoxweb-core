package org.zoxweb.server.net.common;

import org.zoxweb.shared.task.ExceptionCallback;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface ConnectionCallback
        extends ExceptionCallback {
    /**
     * Called when incoming data or something to do
     * @param key the input argument
     */
    void accept(SelectionKey key);

    void accept(ByteBuffer byteBuffer);

    int connected(SelectionKey key);

    default void sslHandshakeSuccessful() {
    }

}