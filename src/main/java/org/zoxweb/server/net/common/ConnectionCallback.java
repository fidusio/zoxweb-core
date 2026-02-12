package org.zoxweb.server.net.common;

import org.zoxweb.shared.task.ExceptionCallback;
import org.zoxweb.shared.io.CloseableType;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

public interface ConnectionCallback<D>
        extends ExceptionCallback, SKHandler, CloseableType {
    /**
     * Called when incoming data or something to do
     * @param key the input argument
     */
    void accept(SelectionKey key);

    void accept(D byteBuffer) throws IOException;

    /**
     * When the connection is fully established this method will be invoked
     * @param key selection associated with the channel in question
     * @return the selection interested ops READ WRITE etc
     * @throws IOException in case of error
     */
    int connected(SelectionKey key) throws IOException;

    <V extends Channel>V getChannel();

    void setChannel(Channel channel);

    default void sslHandshakeSuccessful() {
    }

}