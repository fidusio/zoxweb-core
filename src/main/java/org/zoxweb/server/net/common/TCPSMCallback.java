package org.zoxweb.server.net.common;

import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.SessionCallback;
import org.zoxweb.server.util.UUID7;
import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.CollectionAsArray;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TCP session callback that bridges the NIO layer to a {@link StateMachineInt}: it converts the
 * socket's byte stream into {@link BasicEvent} publications the state machine consumes.
 * <p>
 * Event contract:
 * <ul>
 * <li>{@link BasicEvent#CONNECTED} once, published from {@link #connected(SelectionKey)} with the
 * SelectionKey as payload, always before any read dispatch (NIOSocket ordering guarantee).</li>
 * <li>{@link BasicEvent#RAW_IN_DATA} per read, the payload buffer is a detached copy owned by the
 * consumer, safe for async handling; the consumer may recache it via ByteBufferUtil when done.</li>
 * <li>{@link BasicEvent#CLOSED} exactly once per session from the close delegate, whatever the
 * termination path (EOF, read error, {@link #exception(Throwable)}, external close); the payload
 * is the causing Throwable when the error path stashed one under {@link Params#EXCEPTION},
 * null otherwise.</li>
 * </ul>
 * The state machine can register per-session resources in its {@link Params#AUTO_CLOSEABLE}
 * property collection, they are closed during session teardown; the socket channel itself is
 * registered there by {@link #setChannel(Channel)}.
 */
public class TCPSMCallback
        extends SessionCallback<StateMachineInt<?>, DataPacket<Long>, OutputStream>
        implements CloseableType, Identifier<String>, ConnectionCallback<DataPacket<Long>> {

    public static final LogWrapper log = new LogWrapper(TCPSMCallback.class).setEnabled(false);
    private final AtomicLong packetsCounter = new AtomicLong(0);
    private volatile BaseChannelOutputStream bcos;
    private volatile SocketChannel socket;
    private volatile InetSocketAddress remoteAddress;
    private final String id;

    /**
     * Closes the session exactly once via the closeable delegate: closes the state machine's
     * {@link Params#AUTO_CLOSEABLE} resources, recaches the read buffer and publishes the
     * {@link BasicEvent#CLOSED} event, subsequent calls are no-ops.
     *
     * @throws Exception in case of error
     */
    @Override
    public void close() throws Exception {
        closeableDelegate.close();
    }

    /**
     * @return the session id, caller supplied or UUID7 generated
     */
    @Override
    public String getID() {
        return id;
    }

    /**
     * Checks if closed.
     *
     * @return true if closed
     */
    @Override
    public boolean isClosed() {
        return closeableDelegate.isClosed();
    }

    /**
     * Events published to the state machine, see the class javadoc for the contract.
     */
    public enum BasicEvent {
        CONNECTED,
        CLOSED,
        RAW_IN_DATA,
    }

    /**
     * Keys of the session entries stored in the state machine's properties.
     */
    public enum Params
    {
        AUTO_CLOSEABLE,
        EXCEPTION,
    }

    // 16k is a bit too big but it will be cached + plus it will support SSL
    private final ByteBuffer rawReadBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, SharedIOUtil.K_16);


    /**
     * Creates a session callback with a UUID7 generated id.
     *
     * @param stateMachine the state machine that consumes the session events
     */
    public TCPSMCallback(StateMachineInt<?> stateMachine) {
        this(UUID7.randomUUID().toString(), stateMachine);
    }

    /**
     * Creates a session callback; registers the {@link Params#AUTO_CLOSEABLE} collection in the
     * state machine's properties and arms the close delegate that performs the one-time session
     * teardown, see {@link #close()}.
     *
     * @param id           the session id
     * @param stateMachine the state machine that consumes the session events
     */
    public TCPSMCallback(String id, StateMachineInt<?> stateMachine) {
        SUS.checkIfNull("stateMachine can't be null", stateMachine);
        setConfig(stateMachine);
        stateMachine.getProperties().add(new NamedValue<CollectionAsArray<AutoCloseable>>(Params.AUTO_CLOSEABLE, new CollectionAsArray<AutoCloseable>(new HashSet<AutoCloseable>(), new AutoCloseable[0])));
        closeableDelegate.setDelegate(() ->
        {
            NamedValue<CollectionAsArray<AutoCloseable>> autoCloseables = getConfig().getProperties().getNV(SUS.enumName(Params.AUTO_CLOSEABLE));
            SharedIOUtil.close(autoCloseables.getValue().asArray());
            ByteBufferUtil.cache(rawReadBuffer);
            NamedValue<Throwable> exception = getConfig().getProperties().getNV(SUS.enumName(Params.EXCEPTION));
            getConfig().publishSync(BasicEvent.CLOSED, exception != null ? exception.getValue() : null);
        });
        this.id = id;
    }


    /**
     * Read dispatch: drains the socket, publishing one {@link BasicEvent#RAW_IN_DATA} packet per
     * read via {@link #accept(DataPacket)}; on EOF or read error the session is closed. Never
     * invoked before {@link #connected(SelectionKey)}, and serialized per session by NIOSocket.
     *
     * @param key the session's selection key
     */
    @Override
    public void accept(SelectionKey key) {
        int read = 0;
        try {

            do {
                rawReadBuffer.clear();
                read = socket.isConnected() ? socket.read(rawReadBuffer) : -1;
                if (read > 0) {

                    rawReadBuffer.flip();

                    // the packet buffer is a detached copy owned by the consumer, safe for async handling,
                    // the consumer may ByteBufferUtil.cache() it when done processing
                    ByteBuffer packetBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, rawReadBuffer.array(), 0, rawReadBuffer.remaining(), true);
                    accept(new DataPacket<>(packetsCounter.incrementAndGet(), remoteAddress,  packetBuffer));

                }
            } while (read > 0);


        } catch (IOException e) {
            read = -1;
            e.printStackTrace();
        }

        if (read == -1)
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


    /**
     * Connection establishment: binds the channel, creates the session output stream and fires
     * the {@link BasicEvent#CONNECTED} event with the SelectionKey as payload.
     *
     * @param key the session's selection key
     * @return the selection interest ops to install
     * @throws IOException in case of error
     */
    public int connected(SelectionKey key) throws IOException {
        setChannel(key.channel());
        bcos = new CommonChannelOutputStream(getChannel());
        getConfig().publishSync(BasicEvent.CONNECTED, key);
        return interestOps();
    }

    /**
     * @return the session's socket channel
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends Channel> V getChannel() {
        return (V)socket;
    }

    /**
     * Binds the session's socket channel: registers it in the {@link Params#AUTO_CLOSEABLE}
     * collection so teardown closes it, and resolves the remote address. Called by NIOSocket at
     * channel creation and again from {@link #connected(SelectionKey)} once connected.
     *
     * @param channel the session's SocketChannel
     * @throws IOException in case of error
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setChannel(Channel channel) throws IOException{
        socket = (SocketChannel) channel;
        ((CollectionAsArray<AutoCloseable> )getConfig().getProperties().getNV(SUS.enumName(Params.AUTO_CLOSEABLE)).getValue()).add(socket);
        remoteAddress = ((InetSocketAddress) socket.getRemoteAddress());

    }


    /**
     * Publishes the packet's payload to the state machine; the packet holds a detached copy of the
     * read data, see {@link #accept(SelectionKey)}.
     *
     * @param t the data packet to publish
     */
    public void accept(DataPacket<Long> t) {

        getConfig().publishSync(BasicEvent.RAW_IN_DATA, t.getBuffer());

    }

    /**
     * Convenience overload for buffer-based delivery (e.g. decrypted SSL data); publishes the
     * buffer as is — the caller keeps ownership, unlike the packet path no copy is made.
     *
     * @param t the incoming data buffer
     */
    public void accept(ByteBuffer t) {

        getConfig().publishSync(BasicEvent.RAW_IN_DATA, t);

    }

    @Override
    public void sslHandshakeSuccessful() throws IOException {
        // switch ba
    }

    /**
     * Error termination: stashes the cause under {@link Params#EXCEPTION} so the close delegate
     * publishes it as the {@link BasicEvent#CLOSED} payload, then closes the session.
     *
     * @param e the error that terminated the session
     */
    @Override
    public void exception(Throwable e) {
        if(!isClosed())
            getConfig().getProperties().add(new NamedValue<Throwable>(Params.EXCEPTION, e));
        SharedIOUtil.close(this);
        if (log.isEnabled()) log.getLogger().info("" + e);

    }


    /**
     * @return the session output stream, available once {@link #connected(SelectionKey)} ran
     */
    public BaseChannelOutputStream get() {
        return bcos;
    }
}
