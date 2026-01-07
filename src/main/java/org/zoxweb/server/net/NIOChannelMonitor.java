package org.zoxweb.server.net;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.task.ScheduledAttachment;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class NIOChannelMonitor
        implements Runnable {
    public static final LogWrapper logger = new LogWrapper(NIOChannelMonitor.class).setEnabled(false);
    private final SelectionKey sk;
    private final long timestamp;
    private final SelectorController selectorController;

    public NIOChannelMonitor(SelectionKey sk, SelectorController selector) {
        timestamp = System.currentTimeMillis();
        selectorController = selector;
        this.sk = sk;
    }

    @Override
    public void run() {
        SocketChannel sc = (SocketChannel) sk.channel();
//        Object attachement = sk.attachment();
        try {
            if (!sc.isConnected() || sk.isConnectable()) {
                IOUtil.close(sc);
                selectorController.cancelSelectionKey(sk);
                // we are still waiting for connection
                // and the timeout expired we need to close the channel
                if (logger.isEnabled())
                    logger.getLogger().info("Connection timed out: " + sk + " it took " + Const.TimeInMillis.toString(System.currentTimeMillis() - timestamp));

                if (sk.attachment() instanceof ScheduledAttachment && ((ScheduledAttachment<?>) sk.attachment()).attachment() instanceof ConsumerCallback) {
                    ConsumerCallback<SocketChannel> callback = (ConsumerCallback<SocketChannel>) ((ScheduledAttachment<?>) sk.attachment()).attachment();
                    if (callback != null) {
                        callback.exception(new IOException("Connection timed out"));
                    }
                }
            }
        } catch (Exception e) {
            IOUtil.close(sc);
        }


    }
}
