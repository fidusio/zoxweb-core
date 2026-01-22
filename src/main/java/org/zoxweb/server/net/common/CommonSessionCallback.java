package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.CustomSSLStateMachine;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.task.TaskUtil;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class CommonSessionCallback
        extends BaseSessionCallback<SSLSessionConfig>
        implements ConnectionCallback {
    public static final LogWrapper log = new LogWrapper(CommonSessionCallback.class).setEnabled(false);
    private volatile boolean isClient;
    private volatile SSLContextInfo sslContextInfo;
    //private volatile SSLConnectionHelper sslDispatcher = null;
    //protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ByteBuffer dataBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, 1024);


    public SSLContextInfo getSSLContextInfo() {
        return sslContextInfo;
    }

    public CommonSessionCallback setSSLContextInfo(SSLContextInfo sslContextInfo) {
        this.sslContextInfo = sslContextInfo;
        return this;
    }


    public void accept(SelectionKey key) {
        if (log.isEnabled()) log.getLogger().info("Accepting connection from " + getRemoteAddress());
        if (getConfig() != null && key.channel().isOpen()) {
            getConfig().sslConnectionHelper.publish(getConfig().getHandshakeStatus(), this);
        } else {
            try {

                if (log.isEnabled()) log.getLogger().info("Accepting connection " + key);


                int read;
                do {
                    ((Buffer) dataBuffer).clear();
                    read = ((SocketChannel) key.channel()).isConnected() ? ((SocketChannel) key.channel()).read(dataBuffer) : -1;

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

    /**
     * The application specific data processor
     * @param byteBuffer the input argument
     */
    public abstract void accept(ByteBuffer byteBuffer);

    public boolean isClient() {
        return isClient;
    }

    /**
     * Set this session a client session
     * @param isClient
     * @return
     */
    public CommonSessionCallback setClient(boolean isClient) {
        this.isClient = isClient;
        return this;
    }

    /**
     * perform the ssl upgrade
     * @param sk
     * @return
     * @throws IOException
     */
    protected boolean sslUpgrade(SelectionKey sk) throws IOException {
        if (log.isEnabled()) log.getLogger().info("SSL upgrade started");
        setChannel((ByteChannel) sk.channel());
        setRemoteAddress(((SocketChannel) sk.channel()).getRemoteAddress());
        if (sslContextInfo != null) {
            if (log.isEnabled()) log.getLogger().info("SSLContextInfo: " + sslContextInfo + " isClient: " + isClient());
            SSLSessionConfig sslConfig = new SSLSessionConfig(sslContextInfo);
//            sslConfig.selectorController = getSelectorController();
            sslConfig.sslChannel = (SocketChannel) sk.channel();

            sslConfig.sslOutputStream = new CommonChannelOutputStream(null, (ByteChannel) sk.channel(), 512)
                    .setSSLSessionConfig(sslConfig)
                    .setSSLMode(true);
            setConfig(sslConfig);
            setOutputStream(sslConfig.sslOutputStream);


            sslConfig.beginHandshake(isClient());
            sslConfig.sslConnectionHelper = new CustomSSLStateMachine(this);
            getConfig().sslConnectionHelper.publish(getConfig().getHandshakeStatus(), this);

            if (log.isEnabled()) log.getLogger().info("Will return true");
            return true;
        }
        if (log.isEnabled()) log.getLogger().info("Will return false");
        setOutputStream(new CommonChannelOutputStream(null, (ByteChannel) sk.channel(), 512));
        return false;
    }

    public final int connected(SelectionKey sk) throws IOException {
        if (!sslUpgrade(sk)) {
            // this not a secure connection
            connectedFinished();
        }
        return SelectionKey.OP_READ;

    }


    protected abstract void connectedFinished() throws IOException;

    /**
     * will be called in one condition when the connection is secure and finished the ssl handshake
     */
    public void sslHandshakeSuccessful() {
        try {
            connectedFinished();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isClosed() {
        return isClosed.get();
    }
}
