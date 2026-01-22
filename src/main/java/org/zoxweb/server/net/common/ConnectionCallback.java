package org.zoxweb.server.net.common;

import org.zoxweb.shared.task.ExceptionCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface ConnectionCallback
        extends ExceptionCallback, CommonAcceptSK {
    /**
     * Called when incoming data or something to do
     * @param key the input argument
     */
    void accept(SelectionKey key);

    void accept(ByteBuffer byteBuffer);

    int connected(SelectionKey key) throws IOException;

    default void sslHandshakeSuccessful() {
    }

}