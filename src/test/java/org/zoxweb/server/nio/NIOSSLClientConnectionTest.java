package org.zoxweb.server.nio;

import org.zoxweb.server.http.HTTPRawFormatter;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ssl.SSLNIOSocketHandler;
import org.zoxweb.server.net.ssl.SSLSessionCallback;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPVersion;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class NIOSSLClientConnectionTest {


    public static final LogWrapper log = new LogWrapper(NIOSSLClientConnectionTest.class).setEnabled(true);

    static AtomicLong successCount = new AtomicLong(0);
    static AtomicLong failCount = new AtomicLong(0);



    public static class TimeStampSession
            extends SSLSessionCallback {
        AtomicBoolean closed = new AtomicBoolean(false);
        private UByteArrayOutputStream result = new UByteArrayOutputStream();
        private long timeStamp = System.currentTimeMillis();



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

            if (!closed.getAndSet(true)) {
                IOUtil.close(getChannel());
                log.getLogger().info("Closing connection: " + getRemoteAddress() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - timeStamp) );
            }
        }

        /**
         * Performs this operation on the given argument.
         *
         * @param byteBuffer the input argument
         */
        @Override
        public void accept(ByteBuffer byteBuffer) {

            try {
                ByteBufferUtil.write(byteBuffer, result, true);
                log.getLogger().info(""+result);


                close();
            } catch (IOException e) {
               e.printStackTrace();
            }

        }


        public void exception(Exception e) {
            failCount.incrementAndGet();
            log.getLogger().info(getRemoteAddress()  + " " + e);
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
            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());

            //IOUtil.close(this);

            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://xlogistx.io", "timestamp", "GET");
            hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);
            HTTPRawFormatter hrf = new  HTTPRawFormatter(hmci);


            try {
                get().write(hrf.format(), false);
                log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total() + " it took" + Const.TimeInMillis.toString(System.currentTimeMillis() - timeStamp));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





    public static class SSLConnectionSession
            extends SSLSessionCallback {
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

            if (!closed.getAndSet(true)) {
                log.getLogger().info("Closing connection: " + getRemoteAddress());
                IOUtil.close(getChannel());
            }
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
            log.getLogger().info(getRemoteAddress()  + " " + e);
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
            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            IOUtil.close(this);
        }
    }






    public static long total() {
        return successCount.get() + failCount.get();
    }

    public static void main(String[] args) {

        try {
            long ts = System.currentTimeMillis();
            IPAddress[] ipAddresses = IPAddress.parse(args);
            NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());

            for (IPAddress ipAddress : ipAddresses) {
                log.getLogger().info("" + ipAddress);

                    nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new SSLNIOSocketHandler(new SSLConnectionSession(), true), 5);
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
