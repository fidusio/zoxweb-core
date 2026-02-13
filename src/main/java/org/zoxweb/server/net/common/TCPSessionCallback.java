package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.CustomSSLStateMachine;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.DNSResolverInt;
import org.zoxweb.shared.net.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public abstract class TCPSessionCallback
        extends BaseSessionCallback<SSLSessionConfig>
        implements ConnectionCallback<ByteBuffer> {
    public static final LogWrapper log = new LogWrapper(TCPSessionCallback.class).setEnabled(false);

    private volatile SSLContextInfo sslContextInfo;
    private int timeoutInSec = 5;
    private final ByteBuffer dataBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, 1024);

    private DNSResolverInt dnsResolver;
    protected volatile int interestOps = SelectionKey.OP_READ;
    protected TCPSessionCallback() {

        setID(UUID.randomUUID().toString());
        boolean stat = closeableDelegate.setDelegate(()->{
            SharedIOUtil.close(getChannel(), getOutputStream());
            ByteBufferUtil.cache(dataBuffer);
        });
        if(!stat)
            throw new IllegalStateException("Cannot set delegate to TCPSessionCallback");
    }

    protected TCPSessionCallback(IPAddress ipAddress) {
        this();
        setRemoteAddress(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()));

    }

    protected TCPSessionCallback(IPAddress ipAddress, boolean certValidationEnabled) throws NoSuchAlgorithmException, KeyManagementException {
        this(new SSLContextInfo(ipAddress, certValidationEnabled), null);
    }

    protected TCPSessionCallback(SSLContextInfo sslContextInfo, String id) {
        setSSLContextInfo(sslContextInfo);
        setRemoteAddress(sslContextInfo.getClientAddress());
        if (id != null) {
            id = UUID.randomUUID().toString();
        }
        setID(id);
        boolean stat = closeableDelegate.setDelegate(()->{
            SharedIOUtil.close(getChannel(), getOutputStream());
            ByteBufferUtil.cache(dataBuffer);
        });
        if(!stat)
            throw new IllegalStateException("Cannot set delegate to TCPSessionCallback");
    }

    public DNSResolverInt dnsResolver() {
        return dnsResolver;
    }

    public TCPSessionCallback dnsResolver(DNSResolverInt dnsResolver) {
        this.dnsResolver = dnsResolver;
        return this;
    }

    public int timeoutInSec() {
        return timeoutInSec;
    }

    public TCPSessionCallback timeoutInSec(int timeoutInSec) {
        if (timeoutInSec < 1)
            throw new IllegalArgumentException("timeoutInSec must be greater than zero " + timeoutInSec);
        this.timeoutInSec = timeoutInSec;
        return this;
    }

    public SSLContextInfo getSSLContextInfo() {
        return sslContextInfo;
    }

    public TCPSessionCallback setSSLContextInfo(SSLContextInfo sslContextInfo) {
        if (!sslContextInfo.isClient()) {
            throw new IllegalArgumentException("SSLContextInfo is not client mode");
        }
        this.sslContextInfo = sslContextInfo;
        setRemoteAddress(sslContextInfo.getClientAddress());
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
                    SharedIOUtil.close(this);
                }
            } catch (Exception e) {
                if (log.isEnabled()) e.printStackTrace();
                SharedIOUtil.close(this);
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
        return true;
    }


    /**
     * perform the ssl upgrade
     * @param sk
     * @return
     * @throws IOException
     */
    protected boolean sslUpgrade(SelectionKey sk) throws IOException {
        if (log.isEnabled()) log.getLogger().info("SSL upgrade started");
        if (sslContextInfo != null) {
            if (log.isEnabled()) log.getLogger().info("SSLContextInfo: " + sslContextInfo + " isClient: " + isClient());
            SSLSessionConfig sslConfig = new SSLSessionConfig(sslContextInfo);
//            sslConfig.selectorController = getSelectorController();
            sslConfig.sslChannel = (SocketChannel) sk.channel();

            sslConfig.sslOutputStream = new CommonChannelOutputStream(null, (ByteChannel) sk.channel(), 512)
                    .setSSLSessionConfig(sslConfig);
            setConfig(sslConfig);
            setOutputStream(sslConfig.sslOutputStream);


            sslConfig.beginHandshake();
            sslConfig.sslConnectionHelper = new CustomSSLStateMachine(this);
            getConfig().sslConnectionHelper.publish(getConfig().getHandshakeStatus(), this);

            if (log.isEnabled()) log.getLogger().info("Will return true");
            return true;
        }
        if (log.isEnabled()) log.getLogger().info("Will return false");

        return false;
    }

    @Override
    public final int connected(SelectionKey sk) throws IOException {
        setRemoteAddress((InetSocketAddress) ((SocketChannel) sk.channel()).getRemoteAddress());
        setChannel(sk.channel());
        if (!sslUpgrade(sk)) {
            // this not a secure connection
            setOutputStream(new CommonChannelOutputStream(null, (ByteChannel) sk.channel(), 512));
            connectedFinished();
        }

        return interestOps();

    }

    @Override
    public int interestOps() {
        return interestOps;
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

//    public void close() throws IOException {
////        if (!isClosed.getAndSet(true)) {
////            IOUtil.close(getChannel(), getOutputStream());
////            ByteBufferUtil.cache(dataBuffer);
////        }
//        closeableDelegate.close();
//    }


}
