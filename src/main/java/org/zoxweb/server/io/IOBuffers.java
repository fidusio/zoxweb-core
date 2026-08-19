package org.zoxweb.server.io;

import org.zoxweb.shared.io.CloseableTypeDelegate;

import java.nio.ByteBuffer;

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
 * <b>Concurrency.</b> The buffer fields are {@code volatile} for cross-thread
 * visibility and the close is delegated to a {@link CloseableTypeDelegate}
 * (atomic, one-shot, exception-swallowing), but the accessors do not
 * synchronize — callers coordinate access to the buffers themselves. The
 * recache reads the fields at close time, so late {@code set*Buffer} swaps are
 * honored; a buffer replaced <em>before</em> close is not recached by this
 * class — the replacer is responsible for the old buffer's fate.
 * </p>
 */
public class IOBuffers implements AutoCloseable {
    private volatile ByteBuffer inBuffer;
    private volatile ByteBuffer outBuffer;
    private final CloseableTypeDelegate delegate;



    public IOBuffers()
    {
        delegate= new CloseableTypeDelegate( ()->{
            ByteBufferUtil.cache(inBuffer, outBuffer);
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
}
