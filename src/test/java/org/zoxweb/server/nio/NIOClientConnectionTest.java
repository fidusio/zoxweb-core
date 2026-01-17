package org.zoxweb.server.nio;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.NIOSocketHandler;
import org.zoxweb.server.net.PlainSessionCallback;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class NIOClientConnectionTest {


    public static final LogWrapper log = new LogWrapper(NIOClientConnectionTest.class).setEnabled(true);

    static AtomicLong successCount = new AtomicLong(0);
    static AtomicLong failCount = new AtomicLong(0);

    public static class ConnectionSession
            extends PlainSessionCallback {
        AtomicBoolean closed = new AtomicBoolean(false);



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
            //log.getLogger().info("Closing connection");
            if (!closed.getAndSet(true))
                IOUtil.close(getChannel());
        }

        /**
         * Performs this operation on the given argument.
         *
         * @param byteBuffer the input argument
         */
        @Override
        public void accept(ByteBuffer byteBuffer) {
            SocketChannel channel = (SocketChannel) getChannel();
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            IOUtil.close(this);

        }


        public void exception(Exception e) {
            failCount.incrementAndGet();
            //log.getLogger().info(total() + " " + e);
            IOUtil.close(this);
        }

        /**
         * Checks if closed.
         *
         * @return true if closed
         */
        @Override
        public boolean isClosed() {
            return closed.get();
        }
        @Override
        public void connected(SelectionKey key)
        {
            successCount.incrementAndGet();
            SocketChannel channel = (SocketChannel) getChannel();
            System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            IOUtil.close(this);
        }
    }

    static class ConnectionTracker
            implements ConsumerCallback<SocketChannel> {


        /**
         * Performs this operation on the given argument.
         *
         * @param channel the input argument
         */
        @Override
        public void accept(SocketChannel channel) {
            successCount.incrementAndGet();
            try {
                System.out.println(channel.getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtil.close(channel);

//                try {
//
//
//                    long ts = Const.TimeInMillis.SECOND.MILLIS * (total() % 100);
//                    //System.out.println(Const.TimeInMillis.toString(ts) + " " + channel.isConnected() + " tot: " + total());
//                    TaskUtil.defaultTaskScheduler().queue(ts, () ->
//                    {
//                        //System.out.println("Closing " + channel);
//                        IOUtil.close(channel);
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        }

        /**
         *
         * @param e
         */
        @Override
        public void exception(Exception e) {
            //e.printStackTrace();
            failCount.incrementAndGet();
            System.err.println(e +" " + total());
        }

        public String toString() {
            return successCount.toString() + ", " + failCount.toString();
        }

    }


    public static long total() {
        return successCount.get() + failCount.get();
    }

    public static void main(String[] args) {

        try {
            long ts = System.currentTimeMillis();
            IPAddress[] ipAddresses = IPAddress.RangeDecoder.decode(args[0]);
            boolean tracker = args.length > 1 && args[1].equals("tracker");
            TaskUtil.setMaxTasksQueue(2000);
            NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            ConnectionTracker connectionTracker = new ConnectionTracker();

            //NIOSocketHandlerFactory nshf = new NIOSocketHandlerFactory(ConnectionSession.class, false);
            for (IPAddress ipAddress : ipAddresses) {
                //log.getLogger().info("" + ipAddress);
                if (tracker)
                    nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), connectionTracker, 5);
                else
                    nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new NIOSocketHandler(new ConnectionSession(), false), 5);
            }

            TaskUtil.waitIfBusy(500, () -> total() == ipAddresses.length);
            log.getLogger().info("Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));

            IOUtil.close(nioSocket);

            log.getLogger().info(GSONUtil.toJSONDefault(TaskUtil.info()));
            TaskUtil.waitIfBusyThenClose(25);
            log.getLogger().info("******************************Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
