package org.zoxweb.server.net;

import java.nio.channels.SelectionKey;

public class SKAttachment<V>
{
    private volatile boolean status = true;
    private V attachment = null;
    private volatile  SKController skController = null;
    public SKAttachment(V attachment)
    {
        attach(attachment);
    }

    public boolean isSelectable()
    {
        return status;
    }
    public void setSelectable(SelectionKey sk, boolean stat)
    {
        if(stat &&  skController != null)
            status = skController.enableSelectionKey(sk, stat);
        else
            status = stat;
    }

    public void attach(V obj)
    {
        attachment = obj;
    }

    public V attachment()
    {
        return attachment;
    }

    public SKController getSKController()
    {
        return skController;
    }
    public void setSKController(SKController skController)
    {
        this.skController = skController;
    }
}
