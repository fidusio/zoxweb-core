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
import org.zoxweb.shared.net.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class NIOTunnel
        extends ProtocolHandler {
    private static final LogWrapper log = new LogWrapper(NIOTunnel.class).setEnabled(false);


    public static class NIOTunnelFactory
            extends ProtocolFactoryBase<NIOTunnel> {

        private IPAddress remoteAddress;

        public NIOTunnelFactory() {

        }


        public NIOTunnelFactory(IPAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public void setRemoteAddress(IPAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public IPAddress getRemoteAddress() {
            return remoteAddress;
        }

        @Override
        public NIOTunnel newInstance() {
            return new NIOTunnel(remoteAddress);
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return "NIOTunnelFactory";
        }

        public void init() {
            setRemoteAddress(new IPAddress(getProperties().getValue("remote_host")));
        }

    }


    private volatile SocketChannel destinationChannel = null;
    private volatile SocketAddress sourceAddress = null;
    private volatile ByteBuffer destinationBB;
    private volatile ByteBuffer sourceBB;


    final private IPAddress remoteAddress;

    public NIOTunnel(IPAddress remoteAddress) {
        super(true);
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String getName() {
        return "NIOTunnel";
    }

    @Override
    public String getDescription() {
        return "NIO Tunnel";
    }

    @Override
    protected void close_internal() throws IOException {

        IOUtil.close(destinationChannel);
        IOUtil.close(phSChannel);
        ByteBufferUtil.cache(sourceBB, destinationBB);
        log.getLogger().info("closed: " + sourceAddress + " - " + remoteAddress);

    }


    @Override
    public void accept(SelectionKey key) {
        try {

            if (destinationChannel == null) {
                synchronized (this) {
                    if (destinationChannel == null) {
                        sourceAddress = phSChannel.getRemoteAddress();
                        destinationChannel = SocketChannel.open((new InetSocketAddress(remoteAddress.getInetAddress(), remoteAddress.getPort())));
                        destinationBB = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT);
                        sourceBB = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT);
                        getSelectorController().register(destinationChannel, SelectionKey.OP_READ, this, false);
                    }
                }

            }

            SocketChannel readChannel;
            SocketChannel writeChannel;
            ByteBuffer currentBB;
            int read;

            // based on the source set
            // the destination
            if (key.channel() == phSChannel) {
                readChannel = phSChannel;
                writeChannel = destinationChannel;
                currentBB = sourceBB;
            } else {
                readChannel = destinationChannel;
                writeChannel = phSChannel;
                currentBB = destinationBB;
            }

            do {

                ((Buffer) currentBB).clear();
                read = readChannel.read(currentBB);

                if (read > 0) {
                    ByteBufferUtil.write(writeChannel, currentBB);
                }
            }
            while (read > 0);


            if (read == -1) {
                if (log.isEnabled()) log.getLogger().info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);

                close();

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

    public static void main(String... args) {
        try {
            int index = 0;
            int port = Integer.parseInt(args[index++]);
            IPAddress remoteAddress = new IPAddress(args[index++]);
            TaskUtil.setThreadMultiplier(4);


            new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.simpleTaskScheduler()).
                    addServerSocket(new InetSocketAddress(port), 128, new NIOTunnelFactory(remoteAddress));
        } catch (Exception e) {
            e.printStackTrace();
            TaskUtil.defaultTaskScheduler().close();
            TaskUtil.defaultTaskProcessor().close();
        }
    }

}