package org.zoxweb.server.net.common;

import org.zoxweb.server.net.ssl.*;

import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class CommonSessionCallback
        extends SSLSessionCallback
        implements ConnectionCallback {
    private volatile boolean isClient;
    private volatile SSLContextInfo sslContextInfo;
    private volatile SSLConnectionHelper sslDispatcher = null;


    public SSLContextInfo getSSLContextInfo() {
        return sslContextInfo;
    }

    public CommonSessionCallback setSSLContextInfo(SSLContextInfo sslContextInfo) {
        this.sslContextInfo = sslContextInfo;
        return this;
    }


    public void accept(SelectionKey sk) {
        if (sslDispatcher != null) {
            sslDispatcher.publish(getConfig().getHandshakeStatus(), this);
        } else {

        }
    }

    public boolean isClient() {
        return isClient;
    }

    public CommonSessionCallback setClient(boolean isClient) {
        this.isClient = isClient;
        return this;
    }

    public boolean sslUpgrade(SelectionKey sk) {
        if (sslContextInfo != null) {
            try {
                SSLSessionConfig sslConfig = new SSLSessionConfig(sslContextInfo);
                //sslConfig.selectorController = getSelectorController();
                sslConfig.sslChannel = (SocketChannel) sk.channel();
                //sslConfig.remoteConnection = remoteConnection;
                sslConfig.sslOutputStream = new CommonChannelOutputStream(null, (ByteChannel) sk.channel(), 512)
                        .setSSLSessionConfig(sslConfig)
                        .setSSLMode(true);
                setConfig(sslConfig);
                sslDispatcher = new CustomSSLStateMachine(this);

                if (isClient())
                    sslConfig.beginHandshake(isClient);

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int connected(SelectionKey sk) {
        sslUpgrade(sk);
        return SelectionKey.OP_READ;
    }


}
