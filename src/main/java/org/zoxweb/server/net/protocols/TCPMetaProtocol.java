package org.zoxweb.server.net.protocols;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVGenericMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The TCP protocol validator (META-PROTOCOL.md §1, §4): a {@link TCPSessionCallback} whose
 * whole protocol brain is a JSON-compiled {@link ExchangeScript}. The caller builds it from a
 * definition, hands endpoint + validator to
 * {@code NIOSocket.addClientSocket(InetSocketAddress, ConnectionCallback, timeoutInSec, resolver)},
 * and reads the verdict after {@link #waitForClose(long)} from {@link #getResults()} +
 * {@link #getCloseCause()}.
 * <p>
 * TLS: {@code tls.mode == immediate} starts the inherited {@link #startTLS(boolean)} upgrade from
 * {@link #connectedFinished()} — before any read dispatch; a scripted {@code start_tls} step
 * drives the same upgrade mid-session through the {@link ExchangeScript.Host} seam. Both resume
 * the script from {@link #sslUpgraded(SSLConfigInt)} with the negotiated
 * {@code tls_protocol}/{@code tls_cipher} recorded in the results.
 */
public class TCPMetaProtocol extends TCPSessionCallback implements ExchangeScript.Host {

    public static final LogWrapper log = new LogWrapper(TCPMetaProtocol.class).setEnabled(false);

    private final ExchangeScript script;
    private volatile Throwable closeCause;
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private final AtomicReference<Consumer<TCPMetaProtocol>> closeHook = new AtomicReference<Consumer<TCPMetaProtocol>>();
    private final UByteArrayOutputStream dataAssembler = ByteBufferUtil.allocateUBAOS(SharedIOUtil.K_1);
    private final AtomicBoolean assemblerRecached = new AtomicBoolean(false);

    public TCPMetaProtocol(String id, String jsonConfig) {
        this(id, GSONUtil.fromJSONDefault(jsonConfig, NVGenericMap.class));
    }

    public TCPMetaProtocol(String id, NVGenericMap protocolConfig) {
        super(id);
        implWillFlipBuffer = true;
        // the session's pooled assembly buffer is shared with the engine: accept() appends the
        // received bytes directly, the engine consumes tokens off it via shiftLeft, and close()
        // recaches it
        script = new ExchangeScript(protocolConfig, this, dataAssembler);
        if (script.isUDP())
            throw new IllegalArgumentException("udp definition on the TCP validator — use UDPMetaProtocol");
        if (script.getTimeoutSec() > 0)
            timeoutInSec(script.getTimeoutSec());
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
     * Registers a one-shot completion hook, fired exactly once when the session closes — the
     * verdict bag and close cause are final when it fires. The event-driven twin of
     * {@link #waitForClose(long)} (the {@code PQCCheck.onClose} idiom): a consumer that
     * registers before {@code NIOSocket.addClientSocket} never has to park a thread on the
     * session. A session already closed runs the hook immediately on the registering thread.
     * The hook is invoked from inside the session's close path, so it should hand any real
     * work to an executor rather than block or open sockets in place.
     *
     * @return this
     */
    public TCPMetaProtocol onClose(Consumer<TCPMetaProtocol> hook) {
        closeHook.set(hook);
        if (isClosed())
            fireCloseHook();
        return this;
    }

    private void fireCloseHook() {
        Consumer<TCPMetaProtocol> hook = closeHook.getAndSet(null);
        if (hook != null) {
            try {
                hook.accept(this);
            } catch (Throwable t) {
                // the session is already closed; a failing hook must not unwind the closer
                if (log.isEnabled()) log.getLogger().info("close hook failed: " + t);
            }
        }
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

    // ---- TCPSessionCallback hooks ----

    /**
     * Plain connection established: an IMMEDIATE definition upgrades right here — the remote
     * address is known and no read dispatch has happened yet — otherwise the script starts.
     */
    @Override
    protected void connectedFinished() throws IOException {
        script.markOpen(); // latency clock from connect, so an immediate handshake is measured
        if (script.getTLSMode() == ExchangeScript.TLSMode.IMMEDIATE) {
            try {
                startTLS(script.isCertValidation());
            } catch (GeneralSecurityException e) {
                throw new IOException("TLS context creation failed", e);
            }
        } else {
            script.start();
        }
    }

    /** Handshake finished: record the negotiated session, start or resume the script. */
    @Override
    protected void sslUpgraded(SSLConfigInt sci) throws IOException {
        try {
            // best-effort session introspection — never fails the run
            script.getResults()
                    .build("tls_protocol", sci.getSSLEngine().getSession().getProtocol())
                    .build("tls_cipher", sci.getSSLEngine().getSession().getCipherSuite());
        } catch (RuntimeException e) {
            if (log.isEnabled()) log.getLogger().info("tls introspection failed: " + e);
        }
        script.secured();
    }

    /**
     * Normalizes the two delivery modes (META-PROTOCOL.md §5), assembles the received
     * bytes straight into the session's {@code dataAssembler}, and lets the engine parse the
     * accumulated tokens — consumed via {@code shiftLeft} — with no intermediate chunk copy:
     * the plain path delivers the read loop's flipped {@code dataBuffer} (append only — the
     * loop owns and clears it); the SSL path delivers the session's reused write-mode decrypted
     * buffer (flip, append, clear so the next unwrap appends from position 0).
     */
    @Override
    public void accept(ByteBuffer byteBuffer) {
        if (byteBuffer == null)
            return;

        try {
            ByteBufferUtil.write(byteBuffer, dataAssembler, implWillFlipBuffer);
            script.parse();
        }
        catch (IOException e) {
            // never expected from a UBAOS write — but a stall must not be silent
            exception(e);
        }
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

    /**
     * Recaches the session's assembly buffer once, releases the completion latch, and fires the
     * {@link #onClose(Consumer)} hook after the inherited teardown — the verdict is final first.
     */
    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (assemblerRecached.compareAndSet(false, true))
                ByteBufferUtil.cache(dataAssembler);
            closeLatch.countDown();
            fireCloseHook();
        }
    }

    // ---- ExchangeScript.Host (the engine's transport seam) ----

    @Override
    public void write(byte[] data) throws IOException {
        BaseChannelOutputStream out = getOutputStream();
        if (out == null)
            throw new IOException("session output stream not ready");
        out.write(data);
    }

    @Override
    public void startTLS() throws IOException {
        try {
            startTLS(script.isCertValidation());
        } catch (GeneralSecurityException e) {
            throw new IOException("TLS context creation failed", e);
        }
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
