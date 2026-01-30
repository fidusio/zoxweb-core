package org.zoxweb.server.net;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.common.ConnectionCallback;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;


public class NIOChannelMonitor
        implements Runnable {
    public static final LogWrapper logger = new LogWrapper(NIOChannelMonitor.class).setEnabled(false);
    private final Channel channel;
    private final long timestamp;
    private final SelectorController selectorController;
    private final ConnectionCallback<?> connectionCallback;
    ;

    public NIOChannelMonitor(Channel channel, SelectorController selector, ConnectionCallback<?> connectionCallback) {
        timestamp = System.currentTimeMillis();
        selectorController = selector;
        this.channel = channel;
        this.connectionCallback = connectionCallback;
    }

    @Override
    public void run() {
        SocketChannel sc = (SocketChannel) channel;
        try {
            if (!sc.isConnected()) {
                IOUtil.close(sc);
                selectorController.cancelSelectionKey(sc);
                // we are still waiting for connection
                // and the timeout expired we need to close the channel
                if (logger.isEnabled())
                    logger.getLogger().info("Connection timed out: " + sc + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - timestamp));

                if (connectionCallback != null) {
                    connectionCallback.exception(new IOException("Connection timed out"));
                }
            }
        } catch (Exception e) {
            IOUtil.close(sc);
        }


    }
}
