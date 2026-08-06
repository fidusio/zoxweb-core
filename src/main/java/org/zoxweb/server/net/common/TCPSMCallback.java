package org.zoxweb.server.net.common;

import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.CollectionAsArray;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;

public class TCPSMCallback
        extends BaseSessionCallback<StateMachineInt<?>>
        implements ConnectionCallback<ByteBuffer> {

    public static final LogWrapper log = new LogWrapper(TCPSMCallback.class).setEnabled(false);

    private boolean flipMode = true;
    public enum BasicEvent
    {
        CONNECTED,
        CLOSED,
        RAW_IN_DATA,
    }

    // 16k is a bit too big but it will be cached + plus it will support SSL
    private final ByteBuffer rawReadBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, SharedIOUtil.K_16);
    public TCPSMCallback(StateMachineInt<?> stateMachine) {
        SUS.checkIfNull("stateMachine can't be null", stateMachine);
        setConfig(stateMachine);
        rawReadBuffer.clear();
        stateMachine.getProperties().add(new NamedValue<CollectionAsArray<AutoCloseable>>("auto_closeable", new CollectionAsArray<AutoCloseable>(new HashSet<AutoCloseable>(), new AutoCloseable[0])));
        closeableDelegate.setDelegate(()->
        {
            NamedValue<CollectionAsArray<AutoCloseable> >autoCloseables = getConfig().getProperties().getNV("auto_closeable");
            SharedIOUtil.close(autoCloseables.getValue().asArray());
            ByteBufferUtil.cache(rawReadBuffer);
        });
    }






    /**
     * Called when incoming data or something to do
     *
     * @param key the input argument
     */
    @Override
    public void accept(SelectionKey key) {
        int read = 0;
        try {

            do {
                read = ((SocketChannel) key.channel()).isConnected() ? ((SocketChannel) key.channel()).read(rawReadBuffer) : -1;
                if(read > 0) {
                    if(flipMode)
                        rawReadBuffer.flip();
                    accept(rawReadBuffer);
                    rawReadBuffer.compact();
                }
            }while (read > 0);


        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if(read == -1)
            SharedIOUtil.close(this);
    }

    /**
     * Selection key interested ops
     *
     * @return READ, WRITE etc
     */
    @Override
    public int interestOps() {
        return SelectionKey.OP_READ;
    }


    public int connected(SelectionKey key) throws IOException {
        // need to fie onConnection event
        getConfig().publishSync(BasicEvent.CONNECTED, key);
        return interestOps();
    }



    @Override
    public void accept(ByteBuffer t) {

        getConfig().publishSync(BasicEvent.RAW_IN_DATA, t);

    }

    @Override
    public void sslHandshakeSuccessful() throws IOException {

    }

    @Override
    public void exception(Throwable e) {
        getConfig().publishSync(BasicEvent.CLOSED, e);
    }
}
