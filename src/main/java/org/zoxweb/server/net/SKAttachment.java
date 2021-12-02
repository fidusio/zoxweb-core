package org.zoxweb.server.net;

import java.nio.channels.SelectionKey;

public class SKAttachment<V>
{
//    private volatile boolean status = true;
    private volatile V attachment = null;
    private final  SKController skController;
    public SKAttachment(V attachment, SKController skController)
    {
        attach(attachment);
        this.skController = skController;
    }

//    public boolean isSelectable()
//    {
//        return skController != null ? skController.isSelectable() : false;
//    }
//    public void setSelectable(SelectionKey sk, boolean stat)
//    {
//        if(skController != null)
//            skController.enableSelectionKey(sk, stat);
//
//    }

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
//    public void setSKController(SKController skController)
//    {
//        this.skController = skController;
//    }
}
