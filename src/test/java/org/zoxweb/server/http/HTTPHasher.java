package org.zoxweb.server.http;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.MinMax;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.ParamUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class HTTPHasher {


    public static final LogWrapper log = new LogWrapper(HTTPHasher.class).setEnabled(true);

    static AtomicLong successCount = new AtomicLong(0);
    static AtomicLong failCount = new AtomicLong(0);


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
            SocketChannel channel = getChannel();
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            SharedIOUtil.close(this);

        }

        @Override
        public void exception(Throwable e) {
            failCount.incrementAndGet();
            //log.getLogger().info(getRemoteAddress() + " " + e);
            SharedIOUtil.close(this);


        }


        @Override
        protected void connectedFinished() throws IOException {
            successCount.incrementAndGet();
            SocketChannel channel = getChannel();
            //System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            log.getLogger().info(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            SharedIOUtil.close(this);
        }
    }


    public static long total() {
        return successCount.get() + failCount.get();
    }


    public static HTTPMessageConfigInterface copy(HTTPMessageConfigInterface config, UByteArrayOutputStream content) throws IOException {
        HTTPMessageConfigInterface ret = new HTTPMessageConfig();
        ret.setURL(config.getURL());
        ret.setMethod(config.getMethod());
        ret.setURI(config.getURI());
        ret.setAuthorization(config.getAuthorization());
        NVGenericMap.copy(config.getHeaders(), ret.getHeaders(), true);
        ret.setSecureCheckEnabled(config.isSecureCheckEnabled());
        ret.setContentAsIS(content.unsafeInputStream(null), true);
        return ret;
    }
    public static void main(String[] args) {

        try {
            long ts = System.currentTimeMillis();
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            params.hide("password");
            log.getLogger().info("" + params);
            String url = params.stringValue("url", false);
            String user = params.stringValue("user", false);
            String password = params.stringValue("password", false);
            int repeat = params.intValue("repeat", 5);
            //String hash = params.stringValue("hash", false);
            String filename = params.stringValue("file", false);
            UByteArrayOutputStream content = IOUtil.inputStreamToByteArray(filename, true);
            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, HTTPMethod.POST, false);
            hmci.setContentType(HTTPMediaType.APPLICATION_OCTET_STREAM);
            hmci.setBasicAuthorization(user, password);

            HTTPNIOSocket httpNIOSocket = new HTTPNIOSocket(new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler()));


            MinMax urlHTTP = new MinMax("ulr-http");
            MinMax okHTTP = new MinMax("ok-http");


            AtomicInteger counter = new AtomicInteger(0);
            for (int i = 0; i < repeat; i++) {


                TaskUtil.defaultTaskProcessor().execute(() -> {
                    try {
                        HTTPResponse hr = OkHTTPCall.send(copy(hmci, content));
                        okHTTP.update(hr.getDuration());

                        log.getLogger().info("OkHTTPCall" + hr);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                HTTPURLCallback httpurlCallback = new HTTPURLCallback(copy(hmci, content), new ConsumerCallback<HTTPResponse>() {
                    @Override
                    public void accept(HTTPResponse hr) {
                        log.getLogger().info("HTTPURLCallback: " + hr);
                        urlHTTP.update(hr.getDuration());
                        counter.incrementAndGet();
                    }

                    @Override
                    public void exception(Throwable e) {
                        e.printStackTrace();
                    }
                }, true);
                httpurlCallback.setID(UUID.randomUUID().toString());


                httpNIOSocket.send(httpurlCallback);

//                TaskUtil.defaultTaskProcessor().execute(() -> {
//                    try {
//                        HTTPResponse hr = OkHTTPCall.send(hmci);
//                        log.getLogger().info("" + hr);
//                        counter.incrementAndGet();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                });
            }

            log.getLogger().info("All request sent " + repeat*2);
            TaskUtil.waitIfBusyThenClose(50, () -> repeat * 2 == counter.get());
            httpNIOSocket.getNIOSocket().close();


            log.getLogger().info("******************************Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
            log.getLogger().info( okHTTP + " "  + urlHTTP);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
