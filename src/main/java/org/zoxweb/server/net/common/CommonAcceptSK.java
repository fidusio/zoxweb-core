package org.zoxweb.server.net.common;

import java.nio.channels.SelectionKey;

public interface CommonAcceptSK {
    void accept(SelectionKey sk);

    /**
     * Selection key interested ops
     * @return READ, WRITE etc
     */
    int interestOps();

}
