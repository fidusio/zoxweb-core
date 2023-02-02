package org.zoxweb.server.net;

import java.util.concurrent.atomic.AtomicBoolean;

public class SKAttachment
{

    private volatile Object attachment = null;
    private final AtomicBoolean stat = new AtomicBoolean(true);

    public SKAttachment(Object attachment)
    {
        attach(attachment);
    }


    public void attach(Object obj)
    {
        attachment = obj;
    }

    public <V> V attachment()
    {
        return (V)attachment;
    }



    public void setSelectable(boolean stat) {
        this.stat.set(stat);
    }


    public boolean isSelectable() {
        return stat.get();
    }
}
