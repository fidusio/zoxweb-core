package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOBuffers;
import org.zoxweb.shared.io.CloseableTypeDelegate;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class UDPSessionCallback
        implements ConnectionCallback<DataPacket<?>> {

    public static final LogWrapper log = new LogWrapper(UDPSessionCallback.class).setEnabled(false);
    private final int bufferSize;
    private volatile DatagramChannel channel;
    protected int port;
    private volatile Executor executor = null;
    protected volatile int interestOps = SelectionKey.OP_READ;
    protected final Lock lock = new ReentrantLock();
    private final AtomicLong readCounter = new AtomicLong();
    private final AtomicLong sendCounter = new AtomicLong();

    private final CloseableTypeDelegate closeableDelegate;

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
        closeableDelegate = new CloseableTypeDelegate(() -> SharedIOUtil.close(channel), true);
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
        IOBuffers ioBuffers = null;
        if (key.channel().isOpen()) {
            do {
                try {
                    // allocate a data buffer from cache
                    ioBuffers = new IOBuffers(bufferSize, true);
                    clientAddr = (InetSocketAddress) channel.receive(ioBuffers.getInBuffer());
                    if (clientAddr != null) {
                        // flip the buffer for reading
                        ioBuffers.getInBuffer().flip();
                        DataPacket<Long> dataPacket = new DataPacket<Long>(readCounter.incrementAndGet(), channel, clientAddr, ioBuffers);
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
                    } else {
                        // clientAddr is null no more data to read
                        ByteBufferUtil.cache(ioBuffers);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    ByteBufferUtil.cache(ioBuffers);
                    // recache data buffer
                }
            } while (clientAddr != null && key.channel().isOpen());
        }
    }

    private void recacheBufferAccept(DataPacket<?> dataPacket) throws IOException {
        try {
            accept(dataPacket);
        } finally {
            // recache data buffer
            ByteBufferUtil.cache(dataPacket.getIOBuffers());
        }

    }


    @Override
    public void close() throws IOException {
        closeableDelegate.close();
    }

    @Override
    public boolean isClosed() {
        return closeableDelegate.isClosed();
    }


    public int send(DataPacket<?> dataPacket, boolean flip) throws IOException {
        return send(dataPacket.getIOBuffers().getInBuffer(), dataPacket.getAddress(), flip);
    }


    public int send(ByteBuffer byteBuffer, InetSocketAddress sa, boolean flip) throws IOException {
//        lock.lock();
//        int ret = 0;
//        try {
//            ret = ((DatagramChannel) getChannel()).send(byteBuffer, sa);
//            sendCounter.incrementAndGet();
//        } finally {
//            lock.unlock();
//        }
//
//        return ret;


        int ret = ByteBufferUtil.send(lock, getChannel(), byteBuffer, sa, flip);
        sendCounter.incrementAndGet();
        return ret;
    }

    public long getReadCount() {
        return readCounter.get();
    }

    public long getSendCount() {
        return sendCounter.get();
    }

    @Override
    public int interestOps() {
        return interestOps;
    }

    @Override
    public int connected(SelectionKey key) {
        setChannel(key.channel());
        return interestOps();
    }


}
