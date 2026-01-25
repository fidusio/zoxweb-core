package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class UDPSessionCallback
        implements ConnectionCallback<DataPacket> {

    public static final LogWrapper log = new LogWrapper(UDPSessionCallback.class).setEnabled(false);
    private final int bufferSize;
    private volatile DatagramChannel channel;
    protected int port;
    private transient Executor executor = null;
    protected final Lock lock = new ReentrantLock();
    private final AtomicLong readCounter = new AtomicLong();
    private final AtomicLong sendCounter = new AtomicLong();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    protected UDPSessionCallback(int port) {
        this(null, port, 0);
    }


    protected UDPSessionCallback(Executor executor, int port, int bufferSize) {
        if (!SharedNetUtil.PORTS_RANGE.within(port))
            throw new IllegalArgumentException("Invalid port value: " + port);
        this.port = port;
        if (bufferSize < 512)
            this.bufferSize = Const.SizeInBytes.K.mult(2);
        else
            this.bufferSize = bufferSize;

        setExecutor(executor);
    }

    protected UDPSessionCallback(Executor executor, int port) {
        this(executor, port, 0);
    }

    public int getPort() {
        return port;
    }


    public int getBufferSize() {
        return bufferSize;
    }

    public synchronized UDPSessionCallback setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public Executor getExecutor() {
        return this.executor;
    }

    public void setChannel(Channel channel) {
        this.channel = (DatagramChannel) channel;
    }

    public <V extends Channel> V getChannel() {
        return (V) channel;
    }

    /**
     * Called when incoming data or something to do
     *
     * @param key the input argument
     */
    @Override
    public void accept(SelectionKey key) {
        InetSocketAddress clientAddr = null;
        DatagramChannel channel = (DatagramChannel) key.channel();
        ByteBuffer buffer = null;
        if (key.channel().isOpen()) {
            do {
                try {
                    // allocate a data buffer from cache
                    buffer = ByteBufferUtil.allocateByteBuffer(bufferSize);
                    clientAddr = (InetSocketAddress) channel.receive(buffer);
                    if (clientAddr != null) {
                        readCounter.incrementAndGet();
                        DataPacket<Long> dataPacket = new DataPacket<Long>(readCounter.incrementAndGet(), clientAddr, buffer);
                        if (executor != null) {
                            // lambda bypass

                            executor.execute(() -> {
                                try {
                                    recacheBufferAccept(dataPacket);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        } else
                            recacheBufferAccept(dataPacket);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    // recache data buffer
                    ByteBufferUtil.cache(buffer);
                }
            } while (clientAddr != null && key.channel().isOpen());
        }
    }

    private void recacheBufferAccept(DataPacket<?> dataPacket) throws IOException {
        try {
            dataPacket.getBuffer().flip();
            accept(dataPacket);
        } finally {
            // recache data buffer
            ByteBufferUtil.cache(dataPacket.getBuffer());
        }

    }


    @Override
    public void close() throws IOException {
        if (!isClosed.getAndSet(true)) {
            IOUtil.close(channel);
        }
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }


    public void send(DataPacket<?> dataPacket) throws IOException {
       send(dataPacket.getBuffer(), dataPacket.getAddress());
    }


    public void send(ByteBuffer byteBuffer, InetSocketAddress sa) throws IOException {
        lock.lock();
        try {
            ((DatagramChannel)getChannel()).send(byteBuffer, sa);
            sendCounter.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    public long getReadCount() {return readCounter.get();}

    public long getSendCount() {return sendCounter.get();}


}
