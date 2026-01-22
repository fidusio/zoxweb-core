package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.*;
import org.zoxweb.server.task.TaskUtil;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CommonSessionCallback
        extends BaseSessionCallback<SSLSessionConfig>
        implements ConnectionCallback {
    private volatile boolean isClient;
    private volatile SSLContextInfo sslContextInfo;
    private volatile SSLConnectionHelper sslDispatcher = null;
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ByteBuffer dataBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, 1024);


    public SSLContextInfo getSSLContextInfo() {
        return sslContextInfo;
    }

    public CommonSessionCallback setSSLContextInfo(SSLContextInfo sslContextInfo) {
        this.sslContextInfo = sslContextInfo;
        return this;
    }


    public void accept(SelectionKey key) {
        if (sslDispatcher != null) {
            sslDispatcher.publish(getConfig().getHandshakeStatus(), this);
        } else {
            try {

                if (log.isEnabled()) log.getLogger().info("Accepting connection " + key);


                int read;
                do {
                    ((Buffer) dataBuffer).clear();
                    read = ((SocketChannel)key.channel()).isConnected() ? ((SocketChannel)key.channel()).read(dataBuffer) : -1;

                    if (read > 0)
                        accept(dataBuffer);
                }
                while (read > 0);


                if (read == -1) {
                    if (log.isEnabled()) log.getLogger().info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);
                    IOUtil.close(this);
                    if (log.isEnabled())
                        log.getLogger().info(key + ":" + key.isValid() + " " + Thread.currentThread() + " " + TaskUtil.defaultTaskProcessor().availableExecutorThreads());
                }
            } catch (Exception e) {
                if (log.isEnabled()) e.printStackTrace();
                IOUtil.close(this);
                if (log.isEnabled())
                    log.getLogger().info(System.currentTimeMillis() + ":Connection end " + key + ":" + key.isValid() + " " + Thread.currentThread() + " " + TaskUtil.defaultTaskProcessor().availableExecutorThreads());

            }
        }
    }


    public abstract void accept(ByteBuffer byteBuffer);

    public boolean isClient() {
        return isClient;
    }

    public CommonSessionCallback setClient(boolean isClient) {
        this.isClient = isClient;
        return this;
    }

    public boolean sslUpgrade(SelectionKey sk) throws IOException {
        if (sslContextInfo != null) {
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
        }
        return false;
    }

    public int connected(SelectionKey sk) throws IOException {
        if (!sslUpgrade(sk)) {
                setOutputStream(new CommonChannelOutputStream(null, (ByteChannel) sk.channel(), 512));
        }
        return connectedFinished(sk);
    }

    protected abstract int connectedFinished(SelectionKey sk);

    public boolean isClosed() {
        return isClosed.get();
    }
}
