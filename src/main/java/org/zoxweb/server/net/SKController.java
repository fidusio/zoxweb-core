package org.zoxweb.server.net;

import java.nio.channels.SelectionKey;

public interface SKController {
    void setSelectable(SelectionKey sk, boolean stat);
    boolean isSelectable(SelectionKey sk);
}
