package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.common.UDPSessionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.util.UUID7;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP <b>client</b> callback that bridges a connected datagram socket to a {@link StateMachineInt}:
 * it connects the channel to a fixed remote, then converts the exchange into {@link SMProtoUtil.BasicEvent}
 * publications the state machine consumes — the UDP analog of {@code TCPSMCallback} on the client
 * side. Register it with {@code NIOSocket.addDatagramSocket(sa, usc)} (bind {@code sa} to an
 * ephemeral local address, e.g. {@code new InetSocketAddress(0)}); the remote is this callback's
 * constructor argument, not the bind address.
 * <p>
 * Event contract:
 * <ul>
 * <li>{@link SMProtoUtil.BasicEvent#CONNECTED} once, when the channel is bound and connected to the remote,
 * payload the remote {@link InetSocketAddress}. Published from {@link #setChannel(Channel)} (UDP has
 * no connect handshake and NIOSocket does not call {@code connected()} on the datagram path), before
 * selector registration — so the machine's {@code CONNECTED} handler <b>sends the first datagram</b>
 * ({@link #send(ByteBuffer, boolean)}); any reply is buffered by the OS until the read loop drains
 * it. This is the client kickoff.</li>
 * <li>{@link SMProtoUtil.BasicEvent#DATAGRAM} per received datagram from the remote, payload the {@link DataPacket}.
 * <b>The buffer is a pooled receive buffer the dispatcher recaches immediately after this returns</b>
 * — read it inline during the synchronous dispatch or snapshot via {@link DataPacket#asBytesArray()}.</li>
 * <li>{@link SMProtoUtil.BasicEvent#CLOSED} exactly once from {@link #close()}, payload the causing Throwable when
 * {@link #exception(Throwable)} stashed one under {@link Params#EXCEPTION}, null otherwise.</li>
 * </ul>
 * The channel is <b>connected</b>, so datagrams flow only with the remote (stray-source packets are
 * dropped and ICMP port-unreachable surfaces as a read error); a single malformed datagram is logged
 * and skipped by the receive loop, never fatal.
 * <p>
 * Ownership mirrors TCP: <b>one state machine per callback</b> (enforced via a {@link Params#BOUND}
 * marker), the machine owned by the session and closed by teardown as the last act after
 * {@link SMProtoUtil.BasicEvent#CLOSED} is delivered. Dispatch runs on the selector thread with no executor
 * (serialized), or on a pool if one is supplied (concurrent — only for a thread-safe machine).
 */
public class UDPSMCallback
        extends UDPSessionCallback
        implements Identifier<String> {

    public static final LogWrapper log = new LogWrapper(UDPSMCallback.class).setEnabled(false);



    /**
     * Keys of the session entries stored in the state machine's properties.
     */
    public enum Params {
        EXCEPTION,
        BOUND,
    }

    private final StateMachineInt<?> stateMachine;
    private final String id;
    private final InetSocketAddress remote;
    private final AtomicBoolean connectedPublished = new AtomicBoolean(false);
    private final AtomicBoolean closedPublished = new AtomicBoolean(false);

    /**
     * Creates a selector-thread (synchronous) UDP client with a UUID7 id, ephemeral local bind and
     * default buffer size, targeting {@code remote}.
     *
     * @param remote       the peer to connect to
     * @param stateMachine the state machine that consumes the datagram events
     */
    public UDPSMCallback(InetSocketAddress remote, StateMachineInt<?> stateMachine) {
        this(UUID7.randomUUID().toString(), null, 0, 0, remote, stateMachine);
    }

    /**
     * @param id           the session id
     * @param executor     per-datagram dispatch executor, or null to run on the selector thread
     * @param localPort    the local UDP port to bind (0 = ephemeral); this is the bind, not the remote
     * @param bufferSize   receive buffer size (see {@link UDPSessionCallback})
     * @param remote       the peer to connect to
     * @param stateMachine the state machine that consumes the datagram events
     * @throws IllegalArgumentException if the state machine is already bound to a UDP callback
     */
    public UDPSMCallback(String id, Executor executor, int localPort, int bufferSize,
                         InetSocketAddress remote, StateMachineInt<?> stateMachine) {
        super(executor, localPort, bufferSize);
        SUS.checkIfNulls("remote or stateMachine null", remote, stateMachine);
        if (stateMachine.getProperties().getNV(SUS.enumName(Params.BOUND)) != null)
            throw new IllegalArgumentException("state machine already bound to a UDP callback");
        stateMachine.getProperties().add(new NamedValue<Boolean>(Params.BOUND, Boolean.TRUE));
        this.remote = remote;
        this.stateMachine = stateMachine;
        this.id = id;
    }

    /**
     * @return the session id, caller supplied or UUID7 generated
     */
    @Override
    public String getID() {
        return id;
    }

    /**
     * @return the connected remote peer
     */
    public InetSocketAddress getRemote() {
        return remote;
    }

    /**
     * @return the bound state machine driven by this callback's datagrams
     */
    public StateMachineInt<?> getStateMachine() {
        return stateMachine;
    }

    /**
     * Binds and connects the datagram channel to the remote, then publishes {@link SMProtoUtil.BasicEvent#CONNECTED}
     * once so the machine sends the first datagram. Called by NIOSocket during
     * {@code addDatagramSocket} (after the local bind, before selector registration). The base
     * {@code setChannel} is not declared to throw, so a connect or handler failure surfaces as an
     * unchecked {@link IllegalStateException} (after closing the half-initialized channel).
     *
     * @param channel the freshly bound datagram channel
     */
    @Override
    public void setChannel(Channel channel) {
        super.setChannel(channel);
        DatagramChannel dc = (DatagramChannel) channel;
        try {
            if (!dc.isConnected())
                dc.connect(remote);
            if (connectedPublished.compareAndSet(false, true))
                stateMachine.publishSync(SMProtoUtil.BasicEvent.CONNECTED, remote);
        } catch (IOException | RuntimeException e) {
            // don't leave a half-initialized client registered
            SharedIOUtil.close(dc);
            throw new IllegalStateException("UDP client init failed for " + remote, e);
        }
    }

    /**
     * Publishes one received datagram to the state machine as {@link SMProtoUtil.BasicEvent#DATAGRAM}; the
     * dispatch is synchronous, so consumers run before the base recaches the pooled buffer.
     *
     * @param dataPacket the received datagram (remote address + pooled payload buffer)
     */
    @Override
    public void accept(DataPacket<?> dataPacket) {
        stateMachine.publishSync(SMProtoUtil.BasicEvent.DATAGRAM, dataPacket);
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
        return send(byteBuffer, remote, flip);
    }

    /**
     * Error signal: stashes the cause under {@link Params#EXCEPTION} so {@link #close()} publishes it
     * as the {@link SMProtoUtil.BasicEvent#CLOSED} payload, then closes the socket.
     *
     * @param e the error terminating the session
     */
    @Override
    public void exception(Throwable e) {
        if (!isClosed())
            stateMachine.getProperties().add(new NamedValue<Throwable>(Params.EXCEPTION, e));
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
     * Closes the session exactly once: publishes {@link SMProtoUtil.BasicEvent#CLOSED} (payload = the stashed
     * cause or null) then closes the state machine — a closed machine rejects publishes, so CLOSED
     * must go out first — before the base closes the datagram channel.
     *
     * @throws IOException on a channel close error
     */
    @Override
    public void close() throws IOException {
        if (closedPublished.compareAndSet(false, true)) {
            NamedValue<Throwable> exception = stateMachine.getProperties().getNV(SUS.enumName(Params.EXCEPTION));
            try {
                stateMachine.publishSync(SMProtoUtil.BasicEvent.CLOSED, exception != null ? exception.getValue() : null);
            } catch (IllegalStateException alreadyClosed) {
                if (log.isEnabled()) log.getLogger().info("machine already closed: " + alreadyClosed);
            }
            stateMachine.close();
        }
        super.close();
    }
}
