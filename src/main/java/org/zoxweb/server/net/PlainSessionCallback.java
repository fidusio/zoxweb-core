package org.zoxweb.server.net;

import java.nio.channels.ByteChannel;

public abstract class PlainSessionCallback extends BaseSessionCallback<ByteChannel>
{
    private ChannelOutputStream cos = null;
    public synchronized void setConfig(ByteChannel bc)
    {
        if(cos == null)
            cos = new ChannelOutputStream(bc, 512);
    }



    final public BaseChannelOutputStream get()
    {
        return cos;
    }
}
