package org.zoxweb.server.nio;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;


public class NIOClientConnectionTest {

    static class ConnectionTracker
        implements ConsumerCallback<SocketChannel>
    {
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        /**
         * Performs this operation on the given argument.
         *
         * @param channel the input argument
         */
        @Override
        public void accept(SocketChannel channel) {
            successCount.incrementAndGet();
            try {
                System.out.println(channel.getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                IOUtil.close(channel);
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
            //System.err.println(e +" " + total());
        }

        public String toString()
        {
            return successCount.toString() + ", " + failCount.toString();
        }

        public long total()
        {
            return successCount.get() + failCount.get();
        }
    }
    public static void main(String[] args)  {

        try {
            long ts = System.currentTimeMillis();
            IPAddress[] ipAddresses = IPAddress.parseRange(args[0]);
            NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            ConnectionTracker connectionTracker = new ConnectionTracker();

            for (IPAddress ipAddress : ipAddresses) {
                //System.out.println(ipAddress);
                nioSocket.addClientSocket(new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort()), 10,connectionTracker);
            }

            TaskUtil.waitIfBusy(500, ()->connectionTracker.total() == ipAddresses.length);



            IOUtil.close(nioSocket);

            System.out.println(GSONUtil.toJSONDefault(TaskUtil.info()));
            TaskUtil.waitIfBusyThenClose(25);
            System.out.println(connectionTracker + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
