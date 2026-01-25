package org.zoxweb.server.nio;

import org.zoxweb.server.http.HTTPRawFormatter;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPVersion;
import org.zoxweb.shared.http.URIScheme;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class NIOSSLClientConnectionTest {


    public static final LogWrapper log = new LogWrapper(NIOSSLClientConnectionTest.class).setEnabled(true);

    static AtomicLong successCount = new AtomicLong(0);
    static AtomicLong failCount = new AtomicLong(0);


    public static class URISession
            extends TCPSessionCallback {
        private UByteArrayOutputStream result = new UByteArrayOutputStream();
        private final long timeStamp = System.currentTimeMillis();
        private final URI uri;
        private final String url;


        public URISession(String url) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
            this.url = url;
            this.uri = new URI(url);
            if (URIScheme.match(url, URIScheme.HTTPS, URIScheme.WSS) != null) {
                setSSLContextInfo(new SSLContextInfo(IPAddress.URLDecoder.decode(url), true));
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
            System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());

            //IOUtil.close(this);

            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(uri.getScheme() + uri.getHost() + ":" + uri.getPort(), uri.getRawPath(), "GET");
            hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);
            hmci.setHeader("Host", uri.getHost());
            HTTPRawFormatter hrf = new HTTPRawFormatter(hmci);
            UByteArrayOutputStream ubaos = hrf.format();
            //System.out.println(ubaos);


            getOutputStream().write(ubaos, false);
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total() + " took: " + Const.TimeInMillis.toString(System.currentTimeMillis() - timeStamp));


        }
    }


    public static class PlainSessionCallback
            extends TCPSessionCallback {


        PlainSessionCallback(IPAddress address) { super(address); }


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



        @Override
        protected void connectedFinished() throws IOException {
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
            List<IPAddress> ipAddressesList = new ArrayList<IPAddress>();
            NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            for (int i = 0; i < args.length; i++) {
                IPAddress ipAddress = null;
                try {
                    ipAddress = IPAddress.URLDecoder.decode(args[i]);

//                    nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new SSLNIOSocketHandler(new URISession(args[i]), true), 5);
//                    ipAddressesList.add(ipAddress);
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
//                                nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new PlainSessionCallback(ipAddress), 5);
                                nioSocket.addClientSocket(new PlainSessionCallback(ipAddress));
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
