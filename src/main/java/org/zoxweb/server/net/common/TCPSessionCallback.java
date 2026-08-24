package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.*;
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
        extends BaseSessionCallback<SSLConfigInt>
        implements ConnectionCallback<ByteBuffer>, SSLHandshakeFinished {
    public static final LogWrapper log = new LogWrapper(TCPSessionCallback.class).setEnabled(false);

    private volatile SSLContextInfo sslContextInfo;
    private int timeoutInSec = 5;
    private final ByteBuffer dataBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, SharedIOUtil.K_1);
    protected volatile boolean implWillFlipBuffer = false;

    private DNSResolverInt dnsResolver;
    protected volatile int interestOps = SelectionKey.OP_READ;

    protected TCPSessionCallback(String id) {

        setID(id != null ? id : UUID.randomUUID().toString());
        boolean stat = closeableDelegate.setDelegate(() -> {
            SharedIOUtil.close(getChannel(), getOutputStream());
            ByteBufferUtil.cache(dataBuffer);
        });
        if (!stat)
            throw new IllegalStateException("Cannot set delegate to TCPSessionCallback");
    }

    protected TCPSessionCallback(IPAddress ipAddress) {
        this((String) null);
        setRemoteAddress(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()));

    }

    protected TCPSessionCallback(IPAddress ipAddress, boolean certValidationEnabled) throws NoSuchAlgorithmException, KeyManagementException {
        this(new SSLContextInfo(ipAddress, certValidationEnabled), null);
    }

    protected TCPSessionCallback(SSLContextInfo sslContextInfo, String id) {
        this(id);
        setSSLContextInfo(sslContextInfo)
                .setRemoteAddress(sslContextInfo.getClientAddress());

//        setID(id != null ? id : UUID.randomUUID().toString());
//        boolean stat = closeableDelegate.setDelegate(() -> {
//            SharedIOUtil.close(getChannel(), getOutputStream());
//            ByteBufferUtil.cache(dataBuffer);
//        });
//        if (!stat)
//            throw new IllegalStateException("Cannot set delegate to TCPSessionCallback");
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
            getConfig().getSSLConnectionHelper().publish(getConfig().getHandshakeStatus(), this);
        } else {
            try {

                if (log.isEnabled()) log.getLogger().info("Accepting connection " + key);


                int read;
                do {
                    // loop-top guard: accept(dataBuffer) may close the session (e.g. a
                    // validator's close_on_ready) — teardown has recached nothing here, but the
                    // channel is dead and the loop must not keep touching session state
                    if (isClosed())
                        return;
                    ((Buffer) dataBuffer).clear();
                    read = ((SocketChannel) key.channel()).isConnected() ? ((SocketChannel) key.channel()).read(dataBuffer) : -1;
                    if (log.isEnabled()) log.getLogger().info("Read " + read + " bytes");

                    if (read > 0) {

                        if (!implWillFlipBuffer)
                            dataBuffer.flip();

                        accept(dataBuffer);
                    }
                }
                // getConfig() != null: accept(dataBuffer) upgraded to TLS mid-dispatch — exit so
                // the next selector dispatch routes handshake bytes through the SSL path instead
                // of feeding ciphertext to the plain accept
                while (read > 0 && getConfig() == null);


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


    protected void startTLS(boolean certValidationEnabled) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        if (log.isEnabled()) log.getLogger().info("Starting TLS");
        if (sslContextInfo == null) {
            SSLContextInfo temp = new SSLContextInfo(getRemoteAddress(), certValidationEnabled);
            setSSLContextInfo(temp);
            internalSSLUpgrade(getChannel());
        }
    }

    /**
     * perform the ssl upgrade
     * @param channel
     * @return
     * @throws IOException
     */
    private boolean internalSSLUpgrade(SocketChannel channel) throws IOException {
        if (log.isEnabled()) log.getLogger().info("SSL upgrade started");
        if (sslContextInfo != null) {
            if (log.isEnabled())
                log.getLogger().info("SSLContextInfo: " + getSSLContextInfo() + " isClient: " + isClient());
            SSLSessionConfig sslConfig = new SSLSessionConfig(getSSLContextInfo());
//            sslConfig.selectorController = getSelectorController();
            sslConfig.sslChannel = channel;

            sslConfig.sslOutputStream = new CommonChannelOutputStream(null, (ByteChannel) channel)
                    .setSSLConfigInt(sslConfig);
            setConfig(sslConfig);
            setOutputStream(sslConfig.sslOutputStream);


            sslConfig.beginHandshake(null);
            sslConfig.setSSLConnectionHelper(new CustomSSLStateMachine(sslConfig, this));
            // trigger the handshake process as client
            getConfig().getSSLConnectionHelper().publish(getConfig().getHandshakeStatus(), this);

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
        if (!internalSSLUpgrade((SocketChannel) sk.channel())) {
            // this not a secure connection
            setOutputStream(new CommonChannelOutputStream(null, (ByteChannel) sk.channel()));
            connectedFinished();
        }

        return interestOps();

    }

    @Override
    public int interestOps() {
        return interestOps;
    }


    protected abstract void connectedFinished() throws IOException;

    protected abstract void sslUpgraded(SSLConfigInt sslConfig) throws IOException;

    /**
     * will be called in one condition when the connection is secure and finished the ssl handshake
     */
    public void sslHandshakeSuccessful(SSLConfigInt sci) throws IOException {
        //connectedFinished();
        sslUpgraded(sci);
    }

//    public void close() throws IOException {
////        if (!isClosed.getAndSet(true)) {
////            IOUtil.close(getChannel(), getOutputStream());
////            ByteBufferUtil.cache(dataBuffer);
////        }
//        closeableDelegate.close();
//    }


}
