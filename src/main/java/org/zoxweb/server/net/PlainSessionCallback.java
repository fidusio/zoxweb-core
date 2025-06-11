package org.zoxweb.server.net;

public abstract class PlainSessionCallback extends BaseSessionCallback<ChannelOutputStream> {

    public final BaseChannelOutputStream get() {
        return config;
    }

}
