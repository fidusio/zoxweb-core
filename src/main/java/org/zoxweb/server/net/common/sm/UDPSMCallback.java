package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.SessionCallback;
import org.zoxweb.server.net.common.ConnectionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.util.UUID7;
import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.CollectionAsArray;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * UDP <b>client</b> callback that bridges a connected datagram socket to a {@link StateMachineInt}:
 * the UDP analog of {@link TCPSMCallback}, built the same way — directly on
 * {@link SessionCallback} with the machine as the session config, the machine owning all protocol
 * behavior and the callback owning only the transport bridge. Register it with
 * {@code NIOSocket.addDatagramSocket(sa, cb)} (bind {@code sa} to an ephemeral local address, e.g.
 * {@code new InetSocketAddress(0)}); the remote is this callback's constructor argument, not the
 * bind address. NIOSocket serializes dispatches per session by zeroing the key's interest ops —
 * the same threading model as the TCP path, so a synchronous machine is safe.
 * <p>
 * Event contract:
 * <ul>
 * <li>{@link CommonTrigger#CONNECTED} once, published from {@link #connected(SelectionKey)} —
 * invoked by NIOSocket after selector registration, always before any read dispatch (same lifecycle
 * as the TCP path) — with the remote {@link InetSocketAddress} as payload. This is the machine
 * kickoff: the state machine's {@code CONNECTED} actions <b>send the first datagram</b>
 * ({@link #send(ByteBuffer, boolean)}); nothing happens on the machine before {@code connected()}
 * is invoked. {@link #setChannel(Channel)} is channel setup only (bind, connect, teardown
 * registration) and publishes no events.</li>
 * <li>{@link CommonTrigger#DATAGRAM} per received datagram, payload the {@link DataPacket}.
 * <b>The packet buffer is a detached copy owned by the consumer</b> — same ownership contract as the
 * TCP {@code RAW_IN_DATA} packets: safe for async handling, the consumer recaches it via
 * {@code ByteBufferUtil.cache} when done.</li>
 * <li>{@link CommonTrigger#CLOSED} exactly once per session from the close delegate, whatever the
 * termination path (receive error, {@link #exception(Throwable)}, external close); the payload is
 * the causing Throwable when the error path stashed one under {@link Params#EXCEPTION}, null
 * otherwise.</li>
 * </ul>
 * The channel is <b>connected</b>, so datagrams flow only with the remote (stray-source packets are
 * dropped by the OS) and an ICMP port-unreachable surfaces as a receive error — for a client that is
 * a meaningful failure, so it terminates the session with the cause ({@code CLOSED} carries it).
 * <p>
 * Ownership contract mirrors TCP exactly: <b>one state machine per callback</b> (the
 * {@link Params#AUTO_CLOSEABLE} marker, fail-fast in the constructor), per-session resources
 * registered in the {@link Params#AUTO_CLOSEABLE} collection and closed by teardown in registration
 * order (the datagram channel is registered by {@link #setChannel(Channel)}), and the machine closed
 * by session teardown as the last act after {@link CommonTrigger#CLOSED} is delivered.
 */
public class UDPSMCallback
        extends SessionCallback<StateMachineInt<?>, DataPacket<Long>, DatagramChannel>
        implements CloseableType, Identifier<String>, ConnectionCallback<DataPacket<Long>> {

    public static final LogWrapper log = new LogWrapper(UDPSMCallback.class).setEnabled(false);

    /**
     * Keys of the session entries stored in the state machine's properties.
     */
    public enum Params {
        AUTO_CLOSEABLE,
        EXCEPTION,
    }

    private final AtomicLong packetsCounter = new AtomicLong(0);
    private final String id;
    private final AtomicBoolean connectedPublished = new AtomicBoolean(false);
    private volatile DatagramChannel channel;
    private final Lock sendLock = new ReentrantLock();

    // 16k covers every practical datagram (EDNS, SNMP bulk); cached on teardown
    private final ByteBuffer rawReadBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, SharedIOUtil.K_16);
    // guards rawReadBuffer handoff between the read loop and the close delegate's recache,
    // same discipline as TCPSMCallback
    private final Object readLock = new Object();

    /**
     * Creates a UDP client callback with a UUID7 generated id, targeting {@code remote}.
     *
     * @param remote       the peer to connect to
     * @param stateMachine the state machine that consumes the session events
     */
    public UDPSMCallback(InetSocketAddress remote, StateMachineInt<?> stateMachine) {
        this(UUID7.randomUUID().toString(), remote, stateMachine);
    }

    /**
     * Creates a UDP client callback; registers the {@link Params#AUTO_CLOSEABLE} collection in the
     * state machine's properties and arms the close delegate that performs the one-time session
     * teardown, see {@link #close()}. One state machine per callback: the machine is owned by the
     * session and closed by teardown after {@link CommonTrigger#CLOSED} is delivered.
     *
     * @param id           the session id
     * @param remote       the peer to connect to
     * @param stateMachine the state machine that consumes the session events
     * @throws IllegalArgumentException if the state machine is already bound to a session callback
     */
    public UDPSMCallback(String id, InetSocketAddress remote, StateMachineInt<?> stateMachine) {
        SUS.checkIfNulls("remote or stateMachine null", remote, stateMachine);
        if (stateMachine.getProperties().getNV(SUS.enumName(Params.AUTO_CLOSEABLE)) != null)
            throw new IllegalArgumentException("state machine already bound to a session callback");
        setConfig(stateMachine);
        NamedValue<CollectionAsArray<AutoCloseable>> ncClosable = new NamedValue<CollectionAsArray<AutoCloseable>>(Params.AUTO_CLOSEABLE, new CollectionAsArray<AutoCloseable>(new LinkedHashSet<>(), new AutoCloseable[0]));
        stateMachine.getProperties().add(ncClosable);

        closeableDelegate.setDelegate(() ->
        {
            NamedValue<CollectionAsArray<AutoCloseable>> autoCloseables = getConfig().getProperties().getNV(SUS.enumName(Params.AUTO_CLOSEABLE));
            SharedIOUtil.close(autoCloseables.getValue().asArray());
            // synchronized: an external close must not recache the buffer while a read
            // dispatch still owns it — the channel is already closed above, so the in-flight
            // read loop exits promptly and releases the lock
            synchronized (readLock) {
                ByteBufferUtil.cache(rawReadBuffer);
            }
            NamedValue<Throwable> exception = getConfig().getProperties().getNV(SUS.enumName(Params.EXCEPTION));
            getConfig().publishSync(CommonTrigger.CLOSED, exception != null ? exception.getValue() : null);
            // last act: a closed machine rejects publishes, so CLOSED must go out first
            getConfig().close();
        });
        setRemoteAddress(remote);
        this.id = id;
    }

    /**
     * Closes the session exactly once via the closeable delegate: closes the state machine's
     * {@link Params#AUTO_CLOSEABLE} resources (the datagram channel), recaches the read buffer,
     * publishes the {@link CommonTrigger#CLOSED} event and finally closes the state
     * machine itself; subsequent calls are no-ops.
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
     * Channel setup only — <b>no events</b>: binds the datagram channel, registers it in the
     * {@link Params#AUTO_CLOSEABLE} collection so teardown closes it, and connects it to the
     * remote. The machine kickoff is {@link #connected(SelectionKey)}, invoked by NIOSocket after
     * selector registration — nothing happens on the machine before {@code connected()} runs.
     *
     * @param channel the freshly bound datagram channel
     * @throws IOException on a connect failure — the session is failed first, so teardown
     *                     publishes {@code CLOSED} with the cause and the channel is closed
     */
    @Override
    public void setChannel(Channel channel) throws IOException {
        this.channel = (DatagramChannel) channel;
        registerAutoCloseable(this.channel);
        try {
            if (!this.channel.isConnected())
                this.channel.connect(getRemoteAddress());
        } catch (IOException | RuntimeException e) {
            // don't leave a half-initialized client behind: route through the session error
            // path (CLOSED with cause, channel closed by teardown), then surface to the caller
            exception(e);
            throw e instanceof IOException ? (IOException) e : new IOException("UDP client init failed for " + getRemoteAddress(), e);
        }
    }

    /**
     * @return the session's datagram channel
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends Channel> V getChannel() {
        return (V) channel;
    }

    /**
     * Registers a per-session resource in the {@link Params#AUTO_CLOSEABLE} collection; session
     * teardown closes the registered resources in registration order, before
     * {@link CommonTrigger#CLOSED} is published.
     *
     * @param closeable the resource to close during session teardown
     * @return this
     */
    @SuppressWarnings("unchecked")
    public UDPSMCallback registerAutoCloseable(AutoCloseable closeable) {
        ((CollectionAsArray<AutoCloseable>) getConfig().getProperties().getNV(SUS.enumName(Params.AUTO_CLOSEABLE)).getValue()).add(closeable);
        return this;
    }

    /**
     * Read dispatch: drains the channel, publishing one {@link CommonTrigger#DATAGRAM}
     * packet per received datagram via {@link #accept(DataPacket)}; a receive error terminates the
     * session with the cause. Serialized per session by NIOSocket.
     *
     * @param key the session's selection key
     */
    @Override
    public void accept(SelectionKey key) {
        IOException error = null;
        synchronized (readLock) {
            try {
                InetSocketAddress from;
                do {
                    // a publish below may close the session inline; the delegate has recached
                    // rawReadBuffer at that point — it must not be touched again
                    if (isClosed())
                        return;
                    rawReadBuffer.clear();
                    from = channel.isOpen() ? (InetSocketAddress) channel.receive(rawReadBuffer) : null;
                    if (from != null) {
                        rawReadBuffer.flip();
                        // the packet buffer is a detached copy owned by the consumer, safe for
                        // async handling; the consumer recaches it via ByteBufferUtil.cache
                        ByteBuffer packetBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, rawReadBuffer.array(), 0, rawReadBuffer.remaining(), true);
                        accept(new DataPacket<>(packetsCounter.incrementAndGet(), from, packetBuffer));
                    }
                } while (from != null);
            } catch (IOException e) {
                // connected channel: ICMP port-unreachable and channel errors land here —
                // fatal for a client session
                error = e;
                if (log.isEnabled()) log.getLogger().info("" + e);
            }
        }

        if (error != null)
            exception(error);
    }

    /**
     * Publishes one received datagram to the state machine as
     * {@link CommonTrigger#DATAGRAM}; the packet holds a detached copy of the received
     * data, see {@link #accept(SelectionKey)}.
     *
     * @param dataPacket the received datagram (remote address + detached payload buffer)
     */
    @Override
    public void accept(DataPacket<Long> dataPacket) {
        getConfig().publishSync(CommonTrigger.DATAGRAM, dataPacket);
    }

    /**
     * Sends a datagram to the connected remote.
     *
     * @param byteBuffer the payload
     * @param flip       true if {@code byteBuffer} is in write-mode and must be flipped first
     * @return bytes sent
     * @throws IOException on channel error
     */
    public int send(ByteBuffer byteBuffer, boolean flip) throws IOException {
        return ByteBufferUtil.send(sendLock, channel, byteBuffer, getRemoteAddress(), flip);
    }

    /**
     * The machine kickoff — the {@code ConnectionCallback} lifecycle point NIOSocket invokes
     * after selector registration, strictly before any read dispatch: ensures the channel is set
     * ({@link #setChannel(Channel)} is idempotent, its connect guarded by {@code isConnected()}),
     * publishes {@link CommonTrigger#CONNECTED} exactly once with the remote address as
     * payload, and returns the interest ops to install. From that publish on, the state machine's
     * definition drives the session — its {@code CONNECTED} actions send the first datagram, the
     * read dispatches feed it the replies.
     *
     * @param key the session's selection key
     * @return the selection interest ops
     * @throws IOException on a connect failure, see {@link #setChannel(Channel)}
     */
    @Override
    public int connected(SelectionKey key) throws IOException {
        setChannel(key.channel());
        if (connectedPublished.compareAndSet(false, true))
            getConfig().publishSync(CommonTrigger.CONNECTED, getRemoteAddress());
        return interestOps();
    }

    /**
     * Selection key interested ops
     *
     * @return READ
     */
    @Override
    public int interestOps() {
        return SelectionKey.OP_READ;
    }

    /**
     * Error termination: stashes the cause under {@link Params#EXCEPTION} so the close delegate
     * publishes it as the {@link CommonTrigger#CLOSED} payload, then closes the session.
     *
     * @param e the error that terminated the session
     */
    @Override
    public void exception(Throwable e) {
        if (!isClosed())
            getConfig().getProperties().add(new NamedValue<Throwable>(Params.EXCEPTION, e));
        SharedIOUtil.close(this);
        if (log.isEnabled()) log.getLogger().info("" + e);
    }

    /**
     * UDP has no TLS handshake in this stack.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void sslHandshakeSuccessful(SSLConfigInt sci) {
        throw new UnsupportedOperationException("UDP callback has no TLS handshake");
    }

    /**
     * @return the session's datagram channel, available once {@link #setChannel(Channel)} ran
     */
    @Override
    public DatagramChannel get() {
        return channel;
    }
}
