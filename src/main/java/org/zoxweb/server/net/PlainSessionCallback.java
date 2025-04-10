package org.zoxweb.server.net;

public abstract class PlainSessionCallback extends BaseSessionCallback<ChannelOutputStream>
{
//    private volatile ChannelOutputStream cos = null;
//    public synchronized void setConfig(ChannelOutputStream bc)
//    {
//        if(cos == null) {
//            cos = bc;
//        }
//    }



    public final BaseChannelOutputStream get()
    {
        return config;
    }
}
