/*
 * Copyright (c) 2017-2021 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.StateMachine;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.server.net.common.CommonChannelOutputStream;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


public class SSLNIOSocketHandler
        extends ProtocolHandler {


    private static class TunnelCallback extends BaseSessionCallback<SSLSessionConfig> {


        @Override
        public void exception(Throwable e) {
            // exception handling
            //e.printStackTrace();
            if (log.isEnabled()) log.getLogger().info(e + "");
        }

        @Override
        public void accept(ByteBuffer buffer) {
            // data handling for the remote tunnel
            // we have a byte buffer with data
            // we forward it to ssl connection

            if (buffer != null) {
                if (log.isEnabled()) log.getLogger().info("We have data to send: " + buffer);
                try {
                    ByteBufferUtil.smartWrite(null, getConfig().remoteChannel, buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (log.isEnabled()) log.getLogger().info(e + "");
                    // we should close
                    IOUtil.close(this);
                }
            }
        }

        /**
         * Closes this stream and releases any system resources associated
         * with it. If the stream is already closed then invoking this
         * method has no effect.
         *
         * <p> As noted in {@link AutoCloseable#close()}, cases where the
         * close may fail require careful attention. It is strongly advised
         * to relinquish the underlying resources and to internally
         * <em>mark</em> the {@code Closeable} as closed, prior to throwing
         * the {@code IOException}.
         *
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void close() throws IOException {
            IOUtil.close(getOutputStream());
        }

        /**
         * @return
         */
        @Override
        public boolean isClosed() {
            return getOutputStream() != null && getOutputStream().isClosed();
        }
    }


    private volatile SSLConnectionHelper sslDispatcher = null;
    private volatile SSLSessionConfig sslConfig = null;
    public final IPAddress remoteConnection;
    private transient SSLContextInfo sslContextInfo;
    //private final SSLSessionCallback sessionCallback;
    private final boolean simpleStateMachine;
    private transient boolean trustAll = false;

    ///private StaticSSLStateMachine staticSSLStateMachine = null;

//	public SSLNIOSocket(SSLContextInfo sslContext, InetSocketAddressDAO ra)
//	{
//
//		this(sslContext, ra, new TunnelCallback());
//	}
    public SSLNIOSocketHandler(SSLContextInfo sslContextInfo, BaseSessionCallback<SSLSessionConfig> sessionCallback) {
        this(sslContextInfo, sessionCallback, true, null);
    }

    public SSLNIOSocketHandler(SSLContextInfo sslContextInfo, BaseSessionCallback<SSLSessionConfig> sessionCallback, boolean simpleStateMachine,
                               IPAddress rc) {
        super(true);
        SUS.checkIfNulls("context  can't be null", sslContextInfo);
        this.sslContextInfo = sslContextInfo;
        remoteConnection = rc;
        this.simpleStateMachine = simpleStateMachine;
        if (remoteConnection != null && sessionCallback == null) {
            this.sessionCallback = new TunnelCallback();
        } else {
            SUS.checkIfNulls("SSL session call can't be null", sessionCallback);
            this.sessionCallback = sessionCallback;
        }

        //SUS.checkIfNulls("Session callback can't be null", this.sessionCallback);
    }

    public SSLNIOSocketHandler(BaseSessionCallback<SSLSessionConfig> sessionCallback, boolean trustAll) {
        super(false);
        remoteConnection = null;
        this.simpleStateMachine = true;
        SUS.checkIfNulls("SSL session call can't be null", sessionCallback);
        this.sessionCallback = sessionCallback;
        this.trustAll = trustAll;

        //SUS.checkIfNulls("Session callback can't be null", this.sessionCallback);
    }


    @Override
    public String getName() {
        return "SSLNIOSocket";
    }

    @Override
    public String getDescription() {
        return "SSL NIO Server Socket";
    }

    @Override
    protected void close_internal() throws IOException {
        IOUtil.close(sslConfig, sessionCallback);
    }


    @Override
    public boolean isClosed() {
        return isClosed.get() || (sslConfig != null && sslConfig.sslChannel != null && !sslConfig.sslChannel.isOpen());
    }


    /**
     * This method is exposed at the package level purposely
     * @return SSLContextInfo
     */
    SSLContextInfo getSSLContextInfo() {
        return sslContextInfo;
    }

    @Override
    public void accept(SelectionKey key) {
        if (log.isEnabled()) log.getLogger().info("Start of Accept SSLNIOSocket");
        try {
            // begin handshake will be called once subsequent calls are ignored
            //sslConfig.beginHandshake(sslContextInfo.isClient());
//            if(sslContextInfo.isClient()) {
//                sessionCallback.connected(key);
//            }

            if (log.isEnabled()) log.getLogger().info("AcceptNewData: " + key);

            // channel selection data coming from ssl channel or tunnel response
            if (key.channel() == sslConfig.sslChannel && sslConfig.sslChannel.isConnected()) {
                // here we have an application code that will process decrypted data
                sslDispatcher.publish(sslConfig.getHandshakeStatus(), (BaseSessionCallback<SSLSessionConfig>) sessionCallback);
            } else if (key.channel() == sslConfig.remoteChannel && sslConfig.remoteChannel.isConnected()) {
                // this is the tunnel section connection
                int bytesRead;
                do {
                    bytesRead = sslConfig.remoteChannel.isConnected() ? sslConfig.remoteChannel.read(sslConfig.inRemoteData) : -1;
                    if (bytesRead > 0) sslConfig.sslOutputStream.write(sslConfig.inRemoteData);
                } while (bytesRead > 0);

                if (bytesRead == -1) {
                    if (log.isEnabled())
                        log.getLogger().info("SSL-CHANNEL-CLOSED-NEED_UNWRAP: " + sslConfig.getHandshakeStatus() + " bytesRead: " + bytesRead);
                    IOUtil.close(this);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            IOUtil.close(this);
            if (log.isEnabled())
                log.getLogger().info(System.currentTimeMillis() + ":Connection end " + key + ":" + key.isValid() + " " + TaskUtil.availableThreads(getExecutor()));
        }
        if (log.isEnabled())
            log.getLogger().info("End of SSLNIOSocket-ACCEPT  available thread:" + TaskUtil.availableThreads(getExecutor()));
    }


    @Override
    public synchronized void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
        if(sslContextInfo == null) {
            try {
                sslContextInfo = new SSLContextInfo((InetSocketAddress) ((SocketChannel)asc).getRemoteAddress(), !trustAll);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException(e);
            }
        }
        if (simpleStateMachine) {
            // CustomSSLStateMachine mode
            sslConfig = new SSLSessionConfig(sslContextInfo);
            sslConfig.selectorController = getSelectorController();
            sslConfig.sslChannel = (SocketChannel) asc;
            sslConfig.remoteConnection = remoteConnection;
            sslConfig.sslOutputStream = new CommonChannelOutputStream(this, (ByteChannel) asc, 512)
                    .setSSLSessionConfig(sslConfig)
                    .setSSLMode(true);
            ((BaseSessionCallback<SSLSessionConfig>)sessionCallback).setConfig(sslConfig);
            sslDispatcher = new CustomSSLStateMachine(this);
            sessionCallback.setProtocolHandler(this);
            sessionCallback.setOutputStream(sslConfig.sslOutputStream);

            // not sure about
            // start the handshake here
            if (log.isEnabled()) log.getLogger().info("CustomSSLStateMachine");
        } else {
            // SSLStateMachineMode
            SSLStateMachine sslStateMachine = SSLStateMachine.create(this);
            sslDispatcher = sslStateMachine;
            sslConfig = sslStateMachine.getConfig();
            sslConfig.selectorController = getSelectorController();
            sslConfig.sslChannel = (SocketChannel) asc;
            sslConfig.remoteConnection = remoteConnection;
            //sslConfig.sslOutputStream = new SSLChannelOutputStream(this, sslConfig, 512);
            sslConfig.sslOutputStream = new CommonChannelOutputStream(this, (ByteChannel) asc, 512)
                    .setSSLSessionConfig(sslConfig)
                    .setSSLMode(true);
            ((BaseSessionCallback<SSLSessionConfig>)sessionCallback).setConfig(sslConfig);
            sessionCallback.setProtocolHandler(this);
            sessionCallback.setOutputStream(sslConfig.sslOutputStream);
            sslStateMachine.start(true);
            if (log.isEnabled()) log.getLogger().info("SSLStateMachine");
        }
        sessionCallback.setRemoteAddress((InetSocketAddress) ((SocketChannel) asc).getRemoteAddress());
        sslConfig.beginHandshake(sslContextInfo.isClient());
        // not sure about
        //config.beginHandshake(false);
        getSelectorController().register(asc, SelectionKey.OP_READ, this, isBlocking);
    }


//	@Override
//	public void setSessionCallback(BaseSessionCallback<?> sessionCallback) {
//		//if it is a tunnel we should throw exception
//		if(!(sessionCallback instanceof SSLSessionCallback))
//		{
//			throw new IllegalArgumentException("sessionCallback is not instance of SSLSessionCallback");
//		}
//
//		this.sessionCallback = (SSLSessionCallback) sessionCallback;
//		this.sessionCallback.setConfig(config);
//		this.sessionCallback.setProtocolHandler(this);
//	}


    @SuppressWarnings("resource")
    public static void main(String... args) {

        try {
            ParamUtil.ParamMap params = ParamUtil.parse("-", args);
            int port = params.intValue("-port");
            String keystore = params.stringValue("-keystore");
            String ksType = params.stringValue("-kstype");
            String ksPassword = params.stringValue("-kspassword");
            boolean dbg = params.nameExists("-dbg");
            String ra = params.stringValue("-ra", true);
            IPAddress remoteAddress = ra != null ? new IPAddress(ra) : null;

            if (dbg) {
                StateMachine.log.setEnabled(true);
                TriggerConsumer.log.setEnabled(true);
                SSLSessionConfig.log.setEnabled(true);
            }


            //TaskUtil.setThreadMultiplier(4);
            SSLContext sslContext = SecUtil.initSSLContext(keystore, ksType, ksPassword.toCharArray(), null, null, null);

            new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler()).
                    addServerSocket(new InetSocketAddress(port), 512, new SSLNIOSocketHandlerFactory(new SSLContextInfo(sslContext), remoteAddress));
        } catch (Exception e) {
            e.printStackTrace();
            TaskUtil.defaultTaskScheduler().close();
            TaskUtil.defaultTaskProcessor().close();
        }
    }


    public SSLSessionConfig getConfig() {
        return sslConfig;
    }

    @Override
    public synchronized boolean upgradeToTLS() throws IOException
    {
        return sslConfig != null;
    }


    void createRemoteConnection() {
        if (sslConfig.remoteConnection != null && sslConfig.inRemoteData == null) {
            synchronized (sslConfig) {
                if (sslConfig.inRemoteData == null) {
                    try {

                        sslConfig.inRemoteData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, 512);
                        sslConfig.remoteChannel = SocketChannel.open((new InetSocketAddress(sslConfig.remoteConnection.getInetAddress(), sslConfig.remoteConnection.getPort())));
                        getSelectorController().register(sslConfig.remoteChannel, SelectionKey.OP_READ, this, false);
                        //sessionCallback.setRemoteSocket(config.remoteChannel);

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (log.isEnabled()) log.getLogger().info("" + e);
                        if (log.isEnabled()) log.getLogger().info("connect to " + sslConfig.remoteConnection + " FAILED");
                        sslConfig.close();
                    }
                }
            }
        }
    }

}