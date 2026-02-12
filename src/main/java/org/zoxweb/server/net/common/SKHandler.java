package org.zoxweb.server.net.common;

import java.nio.channels.SelectionKey;

public interface SKHandler {
    /**
     * Selection key has data/signal to be processed
     * @param sk SelectionKey in question
     */
    void accept(SelectionKey sk);

    /**
     * Selection key interested ops
     * @return READ, WRITE etc
     */
    int interestOps();

}
