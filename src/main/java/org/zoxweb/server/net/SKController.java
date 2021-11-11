package org.zoxweb.server.net;

import java.nio.channels.SelectionKey;

public interface SKController {
    boolean enableSelectionKey(SelectionKey sk, boolean stat);
}
