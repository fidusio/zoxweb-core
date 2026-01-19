package org.zoxweb.server.net.common;

import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLSessionCallback;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public abstract class CommonSessionCallback
    extends SSLSessionCallback
{
    private volatile boolean isClient;
    private volatile SSLContextInfo sslContextInfo;


    public SSLContextInfo getSSLContextInfo() {
        return sslContextInfo;
    }

    public CommonSessionCallback setSSLContextInfo(SSLContextInfo sslContextInfo) {
        this.sslContextInfo = sslContextInfo;
        return this;
    }

    public abstract void accept(SelectionKey selectionKey);

    public abstract void accept(ByteBuffer byteBuffer);

    public boolean isClient() {
        return isClient;
    }

    public CommonSessionCallback setClient(boolean isClient) {
        this.isClient = isClient;
        return this;
    }

    public abstract boolean sslUpgrade();


}
