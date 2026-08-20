package org.zoxweb.server.io;

import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.io.CloseableTypeDelegate;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A closeable in/out {@link ByteBuffer} pair whose single job is to keep two
 * direction-paired buffers under one lifecycle: {@link #close()} recaches both
 * into the {@link ByteBufferUtil} pool, exactly once.
 * <p>
 * Primary use is the TLS net-buffer pair of
 * {@code org.zoxweb.server.net.ssl.SSLConfigInt}: {@code getInBuffer()} holds
 * inbound ciphertext (network &rarr; {@code SSLEngine.unwrap} source) and
 * {@code getOutBuffer()} outbound ciphertext ({@code SSLEngine.wrap}
 * destination &rarr; network). A caller may pre-populate a pair and donate it
 * via {@code beginHandshake(IOBuffers)} for reuse across sessions; buffers left
 * unset (or undersized, per the receiver's rules) are filled in by the receiver.
 * The type itself is protocol-agnostic — nothing here is SSL-specific.
 * </p>
 * <p>
 * <b>Ownership.</b> Exactly one owner closes the pair (for SSL, the session's
 * {@code close()}); everyone else borrows. After close both buffers are back in
 * the pool and must not be read, written, or retained.
 * </p>
 * <p>
 * <b>Single-use.</b> The pair object itself is one-shot: the first recache —
 * via {@link #close()} or {@link ByteBufferUtil#cache(IOBuffers)} — consumes
 * its {@link #canCache()} token, and every later recache attempt is a no-op,
 * whichever order the two are called in. Reuse across sessions happens at the
 * {@link ByteBuffer} level through the pool, never by reusing an
 * {@code IOBuffers} instance; a buffer set after the token is consumed is the
 * setter's responsibility to recache.
 * </p>
 * <p>
 * <b>Concurrency.</b> The buffer fields are {@code volatile} for cross-thread
 * visibility and the close is delegated to a {@link CloseableTypeDelegate}
 * (atomic, one-shot, exception-swallowing), but the accessors do not
 * synchronize — callers coordinate access to the buffers themselves. The
 * recache reads the fields at close time, so late {@code set*Buffer} swaps are
 * honored; a buffer replaced <em>before</em> close is not recached by this
 * class — the replacer is responsible for the old buffer's fate.
 * </p>
 */
public class IOBuffers implements CloseableType {
    private volatile ByteBuffer inBuffer;
    private volatile ByteBuffer outBuffer;
    private final CloseableTypeDelegate delegate;
    private final AtomicBoolean canCache = new AtomicBoolean(true);

    /**
     * Creates a pair with both buffers pool-allocated (HEAP) at the given size.
     *
     * @param bufferSize capacity of each buffer
     */
    public IOBuffers(int bufferSize) {
        this(bufferSize, false);

    }

    /**
     * Creates a pair with the in-buffer pool-allocated (HEAP) at the given size, and the
     * out-buffer either allocated at the same size or left null — receive-only paths
     * (e.g. the UDP read dispatch) pass {@code inBufferOnly=true} so no idle out-buffer
     * is drawn from the pool.
     *
     * @param size         capacity of each allocated buffer
     * @param inBufferOnly true to leave the out-buffer null (recache skips nulls)
     */
    public IOBuffers(int size, boolean inBufferOnly) {
        this();
        inBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, size);
        if (!inBufferOnly)
            outBuffer = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, size);
    }

    /**
     * Creates an empty pair; buffers are installed later via {@link #setInBuffer} /
     * {@link #setOutBuffer} (e.g. wrapping an existing buffer, or filled in by
     * {@code beginHandshake}). {@link #close()} recaches through
     * {@link ByteBufferUtil#cache(IOBuffers)}, so it shares the one-shot
     * {@link #canCache()} token with direct cache calls.
     */
    public IOBuffers() {
        delegate = new CloseableTypeDelegate(() -> {
            ByteBufferUtil.cache(this);
        }, false);
    }

    /** Sets the inbound buffer; {@code null} allowed (recache skips nulls). Fluent. */
    public IOBuffers setInBuffer(ByteBuffer inBuffer) {
        this.inBuffer = inBuffer;
        return this;
    }

    /** The inbound buffer; {@code null} until set. Invalid after {@link #close()}. */
    public ByteBuffer getInBuffer() {
        return inBuffer;
    }

    /** The outbound buffer; {@code null} until set. Invalid after {@link #close()}. */
    public ByteBuffer getOutBuffer() {
        return outBuffer;
    }

    /**
     * Consumes the pair's one-shot recache token: the first call returns {@code true},
     * every later call returns {@code false} (atomic, thread-safe). <b>Not a query</b> —
     * calling it "to check" permanently disables the pair's recache and strands both
     * buffers. It exists for {@link ByteBufferUtil#cache(IOBuffers)}, which gates on it
     * so that direct cache calls and {@link #close()} are exactly-once in any order or
     * combination; do not call it from anywhere else.
     *
     * @return true exactly once, on the first call
     */
    public boolean canCache() {
        return canCache.getAndSet(false);
    }

    /** Sets the outbound buffer; {@code null} allowed (recache skips nulls). Fluent. */
    public IOBuffers setOutBuffer(ByteBuffer outBuffer) {
        this.outBuffer = outBuffer;
        return this;
    }

    /**
     * Recaches the current in/out buffers into the {@link ByteBufferUtil} pool.
     * One-shot and thread-safe via {@link CloseableTypeDelegate}: only the first
     * call acts, subsequent calls are no-ops, and recache failures are swallowed
     * rather than thrown ({@code throwIOException=false}). After this call the
     * pair — and both buffers — must be considered invalid.
     *
     * @throws Exception never in practice (signature inherited from {@link AutoCloseable})
     */
    @Override
    public void close() throws Exception {
        delegate.close();
    }

    /**
     * @return true once {@link #close()} has run. Tracks only close(): a pair recached
     * via {@link ByteBufferUtil#cache(IOBuffers)} directly has its {@link #canCache()}
     * token consumed but still reports false here until close() is called.
     */
    public boolean isClosed() {
        return delegate.isClosed();
    }
}
