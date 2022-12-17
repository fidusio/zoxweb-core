package org.zoxweb.server.net;

import java.util.concurrent.atomic.AtomicBoolean;

public class SKAttachment<V>
{

    private volatile V attachment = null;
    //private boolean stat = true;
    private final AtomicBoolean stat = new AtomicBoolean(true);

    public SKAttachment(V attachment)
    {
        attach(attachment);
    }


    public void attach(V obj)
    {
        attachment = obj;
    }

    public V attachment()
    {
        return attachment;
    }



    public void setSelectable(boolean stat) {
        this.stat.set(stat);
    }


    public boolean isSelectable() {
        return stat.get();
    }
}
