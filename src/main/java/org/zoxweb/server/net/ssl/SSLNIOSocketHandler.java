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
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;


public class SSLNIOSocketHandler
        extends ProtocolHandler {


    private static class TunnelCallback extends SSLSessionCallback {


        @Override
        public void exception(Exception e) {
            // exception handling
            //e.printStackTrace();
            if (log.isEnabled()) log.getLogger().info(e + "");
        }

        @Override
        public void accept(ByteBuffer buffer) {
            // data handling
            if (buffer != null) {
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
            IOUtil.close(get());
        }

        /**
         * @return
         */
        @Override
        public boolean isClosed() {
            return get() != null && get().isClosed();
        }
    }


    private volatile SSLConnectionHelper sslDispatcher = null;
    private volatile SSLSessionConfig config = null;
    public final IPAddress remoteConnection;
    private final SSLContextInfo sslContext;
    private final SSLSessionCallback sessionCallback;
    private final boolean simpleStateMachine;

    ///private StaticSSLStateMachine staticSSLStateMachine = null;

//	public SSLNIOSocket(SSLContextInfo sslContext, InetSocketAddressDAO ra)
//	{
//
//		this(sslContext, ra, new TunnelCallback());
//	}
    public SSLNIOSocketHandler(SSLContextInfo sslContext, SSLSessionCallback sessionCallback) {
        this(sslContext, sessionCallback, true, null);
    }

    public SSLNIOSocketHandler(SSLContextInfo sslContext, SSLSessionCallback sessionCallback, boolean simpleStateMachine,
                               IPAddress rc) {
        super(true);
        SUS.checkIfNulls("context  can't be null", sslContext);
        this.sslContext = sslContext;
        remoteConnection = rc;
        this.simpleStateMachine = simpleStateMachine;
        if (remoteConnection != null && sessionCallback == null) {
            //this.sessionCallback = new TunnelCallback();
            this.sessionCallback = new TunnelCallback();
        } else {
            SUS.checkIfNulls("SSL session call can't be null", sessionCallback);
            this.sessionCallback = sessionCallback;
        }

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
        IOUtil.close(config, sessionCallback);
    }


    @Override
    public boolean isClosed() {
        return isClosed.get() || (config != null && config.sslChannel != null && !config.sslChannel.isOpen());
    }


    /**
     * This method is exposed at the package level purposely
     * @return SSLContextInfo
     */
    SSLContextInfo getSSLContextInfo() {
        return sslContext;
    }

    @Override
    public void accept(SelectionKey key) {
        if (log.isEnabled()) log.getLogger().info("Start of Accept SSLNIOSocket");
        try {
            // begin handshake will be called once subsequent calls are ignored
            config.beginHandshake(false);

            if (log.isEnabled()) log.getLogger().info("AcceptNewData: " + key);

            // channel selection data coming from ssl channel or tunnel response
            if (key.channel() == config.sslChannel && config.sslChannel.isConnected()) {
                sslDispatcher.publish(config.getHandshakeStatus(), sessionCallback);
            } else if (key.channel() == config.remoteChannel && config.remoteChannel.isConnected()) {
                // this is the tunnel section connection
                int bytesRead;
                do {
                    bytesRead = config.remoteChannel.isConnected() ? config.remoteChannel.read(config.inRemoteData) : -1;
                    if (bytesRead > 0) config.sslOutputStream.write(config.inRemoteData);
                } while (bytesRead > 0);

                if (bytesRead == -1) {
                    if (log.isEnabled())
                        log.getLogger().info("SSL-CHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytesRead: " + bytesRead);
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
    public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
        if (simpleStateMachine) {
            // CustomSSLStateMachine mode
            config = new SSLSessionConfig(sslContext);
            config.selectorController = getSelectorController();
            config.sslChannel = (SocketChannel) asc;
            config.remoteConnection = remoteConnection;
            config.sslOutputStream = new SSLChannelOutputStream(this, config, 512);
            sessionCallback.setConfig(config);
            sslDispatcher = new CustomSSLStateMachine(this);
            sessionCallback.setProtocolHandler(this);

            // not sure about
            // start the handshake here
            if (log.isEnabled()) log.getLogger().info("CustomSSLStateMachine");
        } else {
            // SSLStateMachineMode
            SSLStateMachine sslStateMachine = SSLStateMachine.create(this);
            sslDispatcher = sslStateMachine;
            config = sslStateMachine.getConfig();
            config.selectorController = getSelectorController();
            config.sslChannel = (SocketChannel) asc;
            config.remoteConnection = remoteConnection;
            config.sslOutputStream = new SSLChannelOutputStream(this, config, 512);
            sessionCallback.setConfig(config);
            sessionCallback.setProtocolHandler(this);
            sslStateMachine.start(true);
            if (log.isEnabled()) log.getLogger().info("SSLStateMachine");
        }
        sessionCallback.setRemoteAddress(((InetSocketAddress) ((SocketChannel) asc).getRemoteAddress()).getAddress());
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
            SSLContext sslContext = SecUtil.SINGLETON.initSSLContext(keystore, ksType, ksPassword.toCharArray(), null, null, null);

            new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler()).
                    addServerSocket(new InetSocketAddress(port), 512, new SSLNIOSocketHandlerFactory(new SSLContextInfo(sslContext), remoteAddress));
        } catch (Exception e) {
            e.printStackTrace();
            TaskUtil.defaultTaskScheduler().close();
            TaskUtil.defaultTaskProcessor().close();
        }
    }


    public SSLSessionConfig getConfig() {
        return config;
    }


    void createRemoteConnection() {
        if (config.remoteConnection != null && config.inRemoteData == null) {
            synchronized (config) {
                if (config.inRemoteData == null) {
                    try {

                        config.inRemoteData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, 512);
                        config.remoteChannel = SocketChannel.open((new InetSocketAddress(config.remoteConnection.getInetAddress(), config.remoteConnection.getPort())));
                        getSelectorController().register(config.remoteChannel, SelectionKey.OP_READ, this, false);
                        //sessionCallback.setRemoteSocket(config.remoteChannel);

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (log.isEnabled()) log.getLogger().info("" + e);
                        if (log.isEnabled()) log.getLogger().info("connect to " + config.remoteConnection + " FAILED");
                        config.close();
                    }
                }
            }
        }
    }

}