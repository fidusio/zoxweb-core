package org.zoxweb.server.nio;

import org.zoxweb.server.http.HTTPNIOSocket;
import org.zoxweb.server.http.HTTPRawFormatter;
import org.zoxweb.server.http.HTTPRawMessage;
import org.zoxweb.server.http.HTTPURLCallback;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
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
        //private UByteArrayOutputStream result = new UByteArrayOutputStream();
        private final long timeStamp = System.currentTimeMillis();
        HTTPRawMessage hrm = new HTTPRawMessage(true);

        //private final String url;
        private final URLInfo urlInfo;


        public URISession(String url) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

            urlInfo = URLInfo.parse(url);
            System.out.println(urlInfo.toBasicURL());

            if (URIScheme.isMatching(url, URIScheme.HTTPS, URIScheme.WSS)) {
                setSSLContextInfo(new SSLContextInfo(urlInfo.ipAddress, true));
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
                if (hrm.parseResponse(urlInfo.scheme, byteBuffer)) {
                    HTTPMessageConfigInterface hmci = hrm.parse();
                    hmci.setContent(hrm.getDataStream().toByteArray());
                    System.out.println(hmci.getHTTPStatusCode());
                    System.out.println(GSONUtil.toJSONDefault(hmci.getHeaders(), true));
                    System.out.println(SharedStringUtil.toString(hmci.getContent()));
                    close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                IOUtil.close(this);
            }

        }


        public void exception(Exception e) {
            e.printStackTrace();
            failCount.incrementAndGet();
            log.getLogger().info(urlInfo.ipAddress + " " + getRemoteAddress() + " " + e);
            IOUtil.close(this);


        }


        @Override
        protected void connectedFinished() throws IOException {
            successCount.incrementAndGet();
            SocketChannel channel = (SocketChannel) getChannel();
            System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());

            //IOUtil.close(this);

            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(urlInfo.toBasicURL(), urlInfo.toURI(), "GET");

            hmci.getHeaders().add(HTTPConst.CommonHeader.CONNECTION_KEEP_ALIVE);
            if(urlInfo.username != null)
            {
                hmci.setBasicAuthorization(urlInfo.username, urlInfo.password);
                hmci.getHeaders().add(hmci.getAuthorization().toHTTPHeader());
            }

            HTTPRawFormatter hrf = new HTTPRawFormatter(hmci);
            UByteArrayOutputStream ubaos = hrf.format();
            //System.out.println(ubaos);



            getOutputStream().write(ubaos, false);
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total() + " took: " + Const.TimeInMillis.toString(System.currentTimeMillis() - timeStamp));


        }
    }


    public static class PlainSessionCallback
            extends TCPSessionCallback {
        PlainSessionCallback(IPAddress address) {
            super(address);
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

        @Override
        public void exception(Throwable e) {
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

            HTTPNIOSocket httpNIOSocket = new HTTPNIOSocket(new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler()));
            for (int i = 0; i < args.length; i++) {
                IPAddress ipAddress = null;
                try {
                    ipAddress = IPAddress.URLDecoder.decode(args[i]);


                    //nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), new URISession(args[i]), 5, null);
                    HTTPURLCallback huc = new HTTPURLCallback(args[i], (r)->{
                        //System.out.println(r.getStatus() + " " + Const.TimeInMillis.toString(r.getDuration()));
                        log.getLogger().info(r.getCorrelationID() + " " + r.getStatus() + " " + Const.TimeInMillis.toString(r.getDuration()));
                        successCount.incrementAndGet();});

                    httpNIOSocket.send(huc);

//                    huc.send(nioSocket, (r)->{
//                        System.out.println(r.getStatus() + " " + Const.TimeInMillis.toString(r.getDuration()));
//                        successCount.incrementAndGet();
//
//                    });
//                    nioSocket.addClientSocket(new HTTPURLCallback(args[i]));
                    ipAddressesList.add(ipAddress);
                    System.out.println(ipAddress);
                } catch (Exception e) {
                    ipAddress = null;
                }

                if (ipAddress == null) {
                    try {

                        IPAddress[] ipAddresses = IPAddress.RangeDecoder.decode(args[i]);
                        for (int j = 0; j < ipAddresses.length; j++) {
                            try {


                                ipAddress = ipAddresses[j];
                                httpNIOSocket.getNIOSocket().addClientSocket(new PlainSessionCallback(ipAddress));
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
            TaskUtil.waitIfBusy(50, () -> total() == size);
            log.getLogger().info("IPAddresses size" + ipAddressesList.size() + " Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
            log.getLogger().info(GSONUtil.toJSONDefault(httpNIOSocket.getNIOSocket().getStats(), true));
            IOUtil.close(httpNIOSocket.getNIOSocket());
            log.getLogger().info(GSONUtil.toJSONDefault(TaskUtil.info()));


            TaskUtil.waitIfBusyThenClose(25);
            log.getLogger().info("******************************Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
