/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
package org.zoxweb.server.net;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;


public class NIOSocketHandler
        extends ProtocolHandler {

    public static final LogWrapper log = new LogWrapper(ProtocolHandler.class).setEnabled(false);

    private volatile ByteBuffer phBB = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, Const.SizeInBytes.K.mult(1));

    //private final PlainSessionCallback sessionCallback;

    public NIOSocketHandler(PlainSessionCallback psc) {
       this(psc, true);
    }

    public NIOSocketHandler(PlainSessionCallback psc, boolean timeout) {
        super(timeout);
        this.sessionCallback = psc;
    }

    @Override
    public String getName() {
        return "NIOSocketHandler";
    }

    @Override
    public String getDescription() {
        return "NIO Socket Handler";
    }

    @Override
    protected void close_internal() throws IOException {
        IOUtil.close(phSChannel, sessionCallback);
        ByteBufferUtil.cache(phBB);
    }


    @Override
    public void accept(SelectionKey key) {
        try {

            if (log.isEnabled()) log.getLogger().info("Accepting connection " + key);
            if (sessionCallback.getConfig() == null) {
                synchronized (this) {
                    if (sessionCallback.getConfig() == null) {
                        ((PlainSessionCallback) sessionCallback).setConfig(new ChannelOutputStream(this, phSChannel, Const.SizeInBytes.K.mult(1)));
                        // need to notify session callback in case waiting for connection
                        sessionCallback.connected(key);
                    }
                }
            }

            int read;
            do {
                ((Buffer) phBB).clear();
                read = phSChannel.isConnected() ? phSChannel.read(phBB) : -1;

                if (read > 0)
                    sessionCallback.accept(phBB);
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

    public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
        phSChannel = (SocketChannel) asc;
        getSelectorController().register(phSChannel, SelectionKey.OP_READ, this, isBlocking);
        sessionCallback.setProtocolHandler(this);
        sessionCallback.setRemoteAddress(((InetSocketAddress) phSChannel.getRemoteAddress()));

    }


    @SuppressWarnings("resource")
    public static void main(String... args) {
        try {
            int index = 0;
            int port = Integer.parseInt(args[index++]);
            Class<? extends PlainSessionCallback> clazz = (Class<? extends PlainSessionCallback>) Class.forName(args[index++]);
            TaskUtil.setThreadMultiplier(8);


            new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler()).addServerSocket(new InetSocketAddress(port), 128, new NIOSocketHandlerFactory(clazz));
        } catch (Exception e) {
            e.printStackTrace();
            TaskUtil.defaultTaskScheduler().close();
            TaskUtil.defaultTaskProcessor().close();
        }
    }


}