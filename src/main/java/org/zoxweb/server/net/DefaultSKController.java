package org.zoxweb.server.net;

import java.nio.channels.SelectionKey;

public class DefaultSKController
implements SKController{
    private boolean selectable = true;
    @Override
    public void setSelectable(SelectionKey sk, boolean stat) {
        selectable = stat;
    }

    @Override
    public boolean isSelectable(SelectionKey sk) {
        return selectable;
    }
}
