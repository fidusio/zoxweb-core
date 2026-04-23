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

    public static void main(String[] args) {

        try {
            long ts = System.currentTimeMillis();
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            params.hide("password");
            String url = params.stringValue("url", false);
            String user = params.stringValue("user", false);
            String password = params.stringValue("password", false);
            int repeat = params.intValue("repeat", 5);
            //String hash = params.stringValue("hash", false);
            String filename = params.stringValue("file", false);
            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, HTTPMethod.POST, false);
            UByteArrayOutputStream ubaos = IOUtil.inputStreamToByteArray(filename, true);
            hmci.setContent(ubaos.toByteArray());
            hmci.setContentType(HTTPMediaType.APPLICATION_OCTET_STREAM);
            hmci.setBasicAuthorization(user, password);

            HTTPNIOSocket httpNIOSocket = new HTTPNIOSocket(new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler()));


            AtomicInteger counter = new AtomicInteger(0);
            for (int i = 0; i < repeat; i++) {


                HTTPURLCallback httpurlCallback = new HTTPURLCallback(hmci, new ConsumerCallback<HTTPResponse>() {
                    @Override
                    public void accept(HTTPResponse httpResponse) {
                        System.out.println("HTTPURLCallback: " + httpResponse);
                        counter.incrementAndGet();
                    }

                    @Override
                    public void exception(Throwable e) {
                        e.printStackTrace();
                    }
                }, true);
                httpurlCallback.setID(UUID.randomUUID().toString());


                httpNIOSocket.send(httpurlCallback);

                TaskUtil.defaultTaskProcessor().execute(() -> {
                    try {
                        HTTPResponse hr = OkHTTPCall.send(hmci);
                        System.out.println(hr);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        TaskUtil.waitIfBusyThenClose(50, ()->repeat*2==counter.get());
        httpNIOSocket.getNIOSocket().close();


        log.getLogger().info("******************************Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
    } catch(
    Exception e)

    {
        e.printStackTrace();
    }

}
}
