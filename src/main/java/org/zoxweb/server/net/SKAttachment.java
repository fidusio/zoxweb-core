package org.zoxweb.server.net;

public class SKAttachment<V>
{
    private boolean status = true;
    private V attachment = null;

    public SKAttachment(V attachment)
    {
        attach(attachment);;
    }

    public boolean isSelectable()
    {
        return status;
    }
    public void setSelectable(boolean stat)
    {
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
}
