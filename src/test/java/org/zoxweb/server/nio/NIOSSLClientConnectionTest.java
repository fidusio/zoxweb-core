package org.zoxweb.server.nio;

import org.zoxweb.server.http.HTTPRawFormatter;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.CommonSessionCallback;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLNIOSocketHandler;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPVersion;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class NIOSSLClientConnectionTest {


    public static final LogWrapper log = new LogWrapper(NIOSSLClientConnectionTest.class).setEnabled(true);

    static AtomicLong successCount = new AtomicLong(0);
    static AtomicLong failCount = new AtomicLong(0);


    public static class URISession
            extends CommonSessionCallback {
        private UByteArrayOutputStream result = new UByteArrayOutputStream();
        private long timeStamp = System.currentTimeMillis();
        private final URI uri;
        private final String url;


        public URISession(String url) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
            this.url = url;
            this.uri = new URI(url);
            setClient(true);
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                setSSLContextInfo(new SSLContextInfo(IPAddress.URLDecoder.decode(url), true));
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

            if (!isClosed.getAndSet(true)) {
                IOUtil.close(getOutputStream());
                log.getLogger().info("Closing connection: " + getRemoteAddress() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - timeStamp));
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
                log.getLogger().info("" + result);


                close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        public void exception(Exception e) {
            e.printStackTrace();
            failCount.incrementAndGet();
            log.getLogger().info(url + " " + getRemoteAddress() + " " + e);
            IOUtil.close(this);


        }


        @Override
        protected void connectedFinished() throws IOException {
            successCount.incrementAndGet();
            SocketChannel channel = (SocketChannel) getChannel();
            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());

            //IOUtil.close(this);

            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(uri.getScheme() + uri.getHost() + ":" + uri.getPort(), uri.getRawPath(), "GET");
            hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);
            HTTPRawFormatter hrf = new HTTPRawFormatter(hmci);


            getOutputStream().write(hrf.format(), false);
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total() + " it took" + Const.TimeInMillis.toString(System.currentTimeMillis() - timeStamp));


        }
    }


    public static class PlainSessionCallback
            extends CommonSessionCallback {


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

            if (!isClosed.getAndSet(true)) {
                //log.getLogger().info("Closing connection: " + getRemoteAddress());
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
            // log.getLogger().info(getRemoteAddress() + " " + e);
            IOUtil.close(this);


        }

//        /**
//         * Checks if closed.
//         *
//         * @return true if closed
//         */
//        @Override
//        public boolean isClosed() {
//            return closed.get();
//        }
//
//        @Override
//        public int connected(SelectionKey key) {
//            successCount.incrementAndGet();
//            SocketChannel channel = (SocketChannel) getChannel();
//            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
//            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
//            IOUtil.close(this);
//            return 0;
//        }

        @Override
        protected void connectedFinished() throws IOException {
            successCount.incrementAndGet();
            SocketChannel channel = (SocketChannel) getChannel();
            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            IOUtil.close(this);
        }
    }

    public static class PlainSession
            extends BaseSessionCallback<BaseChannelOutputStream> {
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
                //log.getLogger().info("Closing connection: " + getRemoteAddress());
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
            // log.getLogger().info(getRemoteAddress() + " " + e);
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
        public int connected(SelectionKey key) {
            successCount.incrementAndGet();
            SocketChannel channel = (SocketChannel) getChannel();
            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            IOUtil.close(this);
            return 0;
        }
    }


    public static long total() {
        return successCount.get() + failCount.get();
    }

    public static void main(String[] args) {

        try {
            long ts = System.currentTimeMillis();
            List<IPAddress> ipAddressesList = new ArrayList<IPAddress>();
            NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            for (int i = 0; i < args.length; i++) {
                IPAddress ipAddress = null;
                try {
                    ipAddress = IPAddress.URLDecoder.decode(args[i]);

                    nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new SSLNIOSocketHandler(new URISession(args[i]), true), 5);
                    ipAddressesList.add(ipAddress);
                    nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new URISession(args[i]), 5);
                    ipAddressesList.add(ipAddress);
                    System.out.println(args[i]);
                } catch (Exception e) {
                    ipAddress = null;
                }

                if (ipAddress == null) {
                    try {

                        IPAddress[] ipAddresses = IPAddress.RangeDecoder.decode(args[i]);
                        for (int j = 0; j < ipAddresses.length; j++) {
                            try {


                                ipAddress = ipAddresses[j];
//                                nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new NIOSocketHandler(new PlainSession(), false), 5);
                                nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new PlainSessionCallback().setClient(true), 5);
                                ipAddressesList.add(ipAddress);
                                //System.out.println(ipAddress);
                            } catch (Exception e) {
                                System.err.println(ipAddresses[j] + " FAILED!!!!");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println(args[i] + " FAILED!!!!");
                    }
                }
            }

            int size = ipAddressesList.size();
            TaskUtil.waitIfBusy(500, () -> total() == size);
            log.getLogger().info("IPAddresses size" + ipAddressesList.size() + " Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
            log.getLogger().info(GSONUtil.toJSONDefault(nioSocket.getStats(), true));
            IOUtil.close(nioSocket);

            log.getLogger().info(GSONUtil.toJSONDefault(TaskUtil.info()));
            TaskUtil.waitIfBusyThenClose(25);
            log.getLogger().info("******************************Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
