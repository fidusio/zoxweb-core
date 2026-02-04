package org.zoxweb.server.net.common;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class TCPPortScan {


    public static final LogWrapper log = new LogWrapper(TCPPortScan.class).setEnabled(true);

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
            System.out.println(getRemoteAddress() + " " + channel.isConnected() + " total: " + total());
            IOUtil.close(this);
        }
    }


    public static long total() {
        return successCount.get() + failCount.get();
    }

    public static void main(String[] args) {

        try {

            IPAddress[] ipAddresses = IPAddress.parseList(args);
            if(SUS.isEmpty(ipAddresses)) {
                throw new IllegalArgumentException("Empty IP addresses");
            }
            long ts = System.currentTimeMillis();
            List<IPAddress> ipAddressesList = new ArrayList<IPAddress>();
            NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());

            try {


                for (int j = 0; j < ipAddresses.length; j++) {
                    try {
                        IPAddress ipAddress = ipAddresses[j];

                        nioSocket.addClientSocket(new PlainSessionCallback(ipAddress).timeoutInSec(2));
                        ipAddressesList.add(ipAddress);
                        //System.out.println(ipAddress);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(ipAddresses[j] + " ConnectFailed");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

            }

            int size = ipAddressesList.size();
            TaskUtil.waitIfBusy(500, () -> total() < size);
            log.getLogger().info("IPAddresses size: " + ipAddressesList.size() + " Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
            log.getLogger().info(GSONUtil.toJSONDefault(nioSocket.getStats(), true));
            IOUtil.close(nioSocket);

            log.getLogger().info(GSONUtil.toJSONDefault(TaskUtil.info()));
            TaskUtil.waitIfBusyThenClose(25);
            log.getLogger().info("******************************Total: " + total() + " Success: " + successCount.get() + " Failed: " + failCount.get() + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("TCPPortScan usage: host:port host:(0,1024]");
        }

    }
}
