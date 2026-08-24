package org.zoxweb.server.net.protocols;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.DataPacket;
import org.zoxweb.server.net.common.UDPSessionCallback;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The UDP protocol validator (META-PROTOCOL.md §1, §4.4): a client-shaped
 * {@link UDPSessionCallback} on a <b>connected</b> datagram channel, whose whole protocol brain
 * is a JSON-compiled {@link ExchangeScript}. Constructed with the peer address; the caller binds
 * it via {@code NIOSocket.addDatagramSocket(new InetSocketAddress(0), validator)} and reads the
 * verdict after {@link #waitForClose(long)}.
 * <p>
 * The connected channel makes the OS drop stray-source datagrams, and surfaces ICMP
 * port-unreachable as a receive {@code IOException} — <b>fatal for a client</b>: the session
 * closes with that cause (the fast "nothing listening" verdict). {@code connected(SelectionKey)}
 * starts the script (first datagram out) strictly before any read dispatch — NIOSocket registers
 * the channel with zero interest ops, invokes {@code connected}, then installs the returned ops.
 * No TLS over UDP (no DTLS in this stack) — such definitions fail at compile.
 */
public class UDPMetaProtocol extends UDPSessionCallback implements ExchangeScript.Host {

    public static final LogWrapper log = new LogWrapper(UDPMetaProtocol.class).setEnabled(false);

    private final String id;
    private final ExchangeScript script;
    private final InetSocketAddress remote;
    private final ByteBuffer rawReadBuffer;
    private volatile Throwable closeCause;
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private final AtomicBoolean connectedOnce = new AtomicBoolean(false);
    private final AtomicBoolean bufferRecached = new AtomicBoolean(false);

    public UDPMetaProtocol(String id, String jsonConfig, InetSocketAddress remote) {
        this(id, GSONUtil.fromJSONDefault(jsonConfig, NVGenericMap.class), remote);
    }

    public UDPMetaProtocol(String id, NVGenericMap protocolConfig, InetSocketAddress remote) {
        super(remote != null ? remote.getPort() : -1);
        SUS.checkIfNulls("remote can't be null", remote);
        this.id = id;
        this.remote = remote;
        script = new ExchangeScript(protocolConfig, this);
        if (!script.isUDP())
            throw new IllegalArgumentException("tcp definition on the UDP validator — use TCPMetaProtocol");
        rawReadBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, getBufferSize());
    }

    public String getID() {
        return id;
    }

    /** @return the compiled script — pre-connect {@code setVar} injection goes through it */
    public ExchangeScript getScript() {
        return script;
    }

    /** @return the verdict bag (final once the session is closed) */
    public NVGenericMap getResults() {
        return script.getResults();
    }

    /** @return the session's terminating cause, or null (clean close — or a session still running) */
    public Throwable getCloseCause() {
        return closeCause;
    }

    /**
     * Pull-style completion wait: true once the session is closed (the verdict is final), false
     * on timeout. {@code timeoutMillis <= 0} polls without waiting.
     */
    public boolean waitForClose(long timeoutMillis) {
        if (isClosed() || timeoutMillis <= 0)
            return isClosed();
        try {
            return closeLatch.await(timeoutMillis, TimeUnit.MILLISECONDS) || isClosed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return isClosed();
        }
    }

    // ---- transport lifecycle ----

    /**
     * Channel setup only — no script activity: binds the channel and <b>connects</b> it to the
     * peer. A connect failure fails the session first (verdict + cause), then surfaces.
     */
    @Override
    public void setChannel(Channel channel) {
        super.setChannel(channel);
        DatagramChannel dc = (DatagramChannel) channel;
        try {
            if (!dc.isConnected())
                dc.connect(remote);
        } catch (IOException | RuntimeException e) {
            exception(e);
            throw e instanceof RuntimeException ? (RuntimeException) e
                    : new IllegalStateException("UDP connect failed for " + remote, e);
        }
    }

    /**
     * The kickoff, strictly before any read dispatch: starts the script exactly once — its
     * initial sends put the first datagram on the wire.
     */
    @Override
    public int connected(SelectionKey key) {
        setChannel(key.channel());
        if (connectedOnce.compareAndSet(false, true))
            script.start();
        return interestOps();
    }

    /**
     * Client receive loop, replacing the base server-shaped loop (which tolerates receive
     * errors): drains the channel into the session read buffer, feeding the engine a detached
     * copy per datagram; any receive {@code IOException} — ICMP port-unreachable included — is
     * fatal and closes the session with the cause.
     */
    @Override
    public void accept(SelectionKey key) {
        DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            InetSocketAddress from;
            do {
                // a feed below may close the session inline (close_on_ready); the buffer is
                // recached at that point and must not be touched again
                if (isClosed())
                    return;
                ((Buffer) rawReadBuffer).clear();
                from = channel.isOpen() ? (InetSocketAddress) channel.receive(rawReadBuffer) : null;
                if (from != null) {
                    ((Buffer) rawReadBuffer).flip();
                    byte[] chunk = new byte[rawReadBuffer.remaining()];
                    rawReadBuffer.get(chunk);
                    script.feed(chunk);
                }
            } while (from != null);
        } catch (IOException e) {
            // connected channel: port-unreachable and channel errors land here — the fast
            // "nothing listening" verdict
            exception(e);
        }
    }

    /** Base-contract data unit (used only when driven through the base dispatch path). */
    @Override
    public void accept(DataPacket<?> dataPacket) {
        ByteBuffer bb = dataPacket.getIOBuffers().getInBuffer();
        byte[] chunk = new byte[bb.remaining()];
        bb.get(chunk);
        script.feed(chunk);
    }

    /** Failure path: stash the close cause, record the verdict, close once. */
    @Override
    public void exception(Throwable e) {
        if (log.isEnabled()) log.getLogger().info("exception: " + e);
        if (!isClosed()) {
            closeCause = e;
            script.recordFailure(e);
            SharedIOUtil.close(this);
        }
    }

    /** Recaches the session read buffer once and releases the completion latch after teardown. */
    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (bufferRecached.compareAndSet(false, true))
                ByteBufferUtil.cache(rawReadBuffer);
            closeLatch.countDown();
        }
    }

    // ---- ExchangeScript.Host (the engine's transport seam) ----

    @Override
    public void write(byte[] data) throws IOException {
        send(ByteBuffer.wrap(data), remote, false);
    }

    /** Never reached: a UDP definition with TLS fails at compile. */
    @Override
    public void startTLS() throws IOException {
        throw new UnsupportedOperationException("no TLS over UDP (no DTLS in this stack)");
    }

    /** Never reached: no TLS over UDP (no DTLS in this stack). */
    @Override
    public void sslHandshakeSuccessful(org.zoxweb.server.net.ssl.SSLConfigInt sci) {
        throw new UnsupportedOperationException("no TLS over UDP (no DTLS in this stack)");
    }

    @Override
    public void fail(Throwable cause) {
        exception(cause);
    }

    @Override
    public void complete() {
        if (script.isCloseOnReady())
            SharedIOUtil.close(this);
    }
}
