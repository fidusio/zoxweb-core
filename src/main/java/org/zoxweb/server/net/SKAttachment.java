package org.zoxweb.server.net;

import java.nio.channels.SelectionKey;

public class SKAttachment<V>
{

    private volatile V attachment = null;
    private final  SKController skController;
    public SKAttachment(V attachment, SKController skController)
    {
        attach(attachment);
        this.skController = skController;
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

}
