package org.zoxweb.server.net.common;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.ProtocolHandler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class CommonProtocolHandler extends ProtocolHandler {

    private final static Logger log = Logger.getLogger(CommonProtocolHandler.class.getName());


    private AtomicBoolean closed = new AtomicBoolean(false);
    private final CommonSessionCallback skSessionCallback;

    public CommonProtocolHandler(CommonSessionCallback skSessionCallback) {
        super(false);
        this.skSessionCallback = skSessionCallback;
        skSessionCallback.setProtocolHandler(this);
    }

    @Override
    protected void close_internal() throws IOException {
        if (!closed.getAndSet(true)) {
            IOUtil.close(skSessionCallback);
        }
    }

    /**
     * Performs this operation on the given argument.
     *
     * @param selectionKey the input argument
     */
    @Override
    public void accept(SelectionKey selectionKey) {
        skSessionCallback.accept(selectionKey);
    }

    /**
     * Returns the property description.
     *
     * @return description
     */
    @Override
    public String getDescription() {
        return "SelectionKey based protocol handler";
    }

    /**
     * @return the name of the object
     */
    @Override
    public String getName() {
        return "SKProtocolHandler";
    }
}
