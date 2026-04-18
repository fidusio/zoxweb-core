package org.zoxweb.server.http;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Non-blocking HTTP/1.1 client session callback.
 * <p>
 * Drives a single HTTP request/response exchange over a
 * {@link org.zoxweb.server.net.common.TCPSessionCallback TCP session}
 * (plaintext or TLS). The request is built from an
 * {@link HTTPMessageConfigInterface} and written once the connection is
 * established ({@link #connectedFinished()}); incoming ciphertext-or-plaintext
 * bytes arrive through {@link #accept(ByteBuffer)} and are incrementally parsed
 * by an {@link HTTPRawMessage}. When the response is complete, the registered
 * {@link ConsumerCallback} is invoked with an {@link HTTPResponseData} carrying
 * status, headers, body, and round-trip duration.
 * </p>
 * <p>
 * Typical usage:
 * </p>
 * <pre>{@code
 * HTTPURLCallback cb = new HTTPURLCallback("https://example.com/", response -> {
 *     System.out.println(response.getStatusCode());
 * });
 * cb.updateTimeStamp();
 * // submit cb to the NIO dispatcher
 * }</pre>
 *
 * @see TCPSessionCallback
 * @see HTTPMessageConfigInterface
 * @see HTTPResponseData
 */
public class HTTPURLCallback extends TCPSessionCallback {

    /** Per-class logger; disabled by default. */
    public static final LogWrapper log = new LogWrapper(HTTPURLCallback.class).setEnabled(false);

    /** Request configuration (URL, method, headers, body, auth, SSL flag). */
    private final HTTPMessageConfigInterface hmci;

    /** Incremental response parser; instantiated in {@link #connectedFinished()} once the connection is up. */
    private volatile HTTPRawMessage hrm = null;

    /** Request-start timestamp for latency accounting; set via {@link #updateTimeStamp()}. */
    private final AtomicLong ts = new AtomicLong(0);

    /** Completion handler; may be swapped via {@link #setCallback(ConsumerCallback)}. */
    private volatile ConsumerCallback<HTTPResponse> callback;

    /** Whether incoming {@link ByteBuffer}s should be flipped by the parser before reading. */
    private final boolean flip;

    /**
     * GET request with certificate validation disabled and buffer-flip enabled.
     *
     * @param url      target URL (http or https)
     * @param callback receives the {@link HTTPResponse} when the exchange completes
     * @throws IOException if the URL cannot be parsed or the SSL context cannot be built
     */
    public HTTPURLCallback(String url, ConsumerCallback<HTTPResponse> callback) throws IOException {
        this(url, HTTPMethod.GET, false, callback, true);
    }

    /**
     * GET request with certificate validation disabled and caller-chosen buffer-flip behavior.
     *
     * @param url      target URL (http or https)
     * @param callback receives the {@link HTTPResponse} when the exchange completes
     * @param flip     if {@code true}, incoming buffers are flipped before parsing
     * @throws IOException if the URL cannot be parsed or the SSL context cannot be built
     */
    public HTTPURLCallback(String url, ConsumerCallback<HTTPResponse> callback, boolean flip) throws IOException {
        this(url, HTTPMethod.GET, false, callback, flip);
    }

    /**
     * Fully parameterized URL-based constructor.
     *
     * @param url                    target URL (http or https)
     * @param httpMethod             HTTP method (GET, POST, etc.)
     * @param certValidationEnabled  if {@code true}, validate the server certificate chain for https
     * @param callback               receives the {@link HTTPResponse} when the exchange completes
     * @param flip                   if {@code true}, incoming buffers are flipped before parsing
     * @throws IOException if the URL cannot be parsed or the SSL context cannot be built
     */
    public HTTPURLCallback(String url, HTTPMethod httpMethod, boolean certValidationEnabled, ConsumerCallback<HTTPResponse> callback, boolean flip) throws IOException {
        this(HTTPMessageConfig.buildHMCI(url, httpMethod, certValidationEnabled), callback, flip);
    }

    /**
     * Lowest-level constructor accepting a pre-built {@link HTTPMessageConfigInterface}.
     * <p>
     * Use this when you need to set custom headers, body, or auth before sending.
     * The HTTP version is forced to 1.1 and the {@code Host} header is populated
     * from the URL. If {@code hmci.isSSL()} is true, an {@link SSLContextInfo}
     * is attached.
     * </p>
     *
     * @param hmci     request configuration (must not be {@code null})
     * @param callback receives the {@link HTTPResponse} when the exchange completes
     * @param flip     if {@code true}, incoming buffers are flipped before parsing
     * @throws IOException        if the SSL context cannot be built ({@link NoSuchAlgorithmException}/{@link KeyManagementException} wrapped)
     * @throws NullPointerException if {@code hmci} is {@code null}
     */
    public HTTPURLCallback(HTTPMessageConfigInterface hmci, ConsumerCallback<HTTPResponse> callback, boolean flip)
            throws IOException {
        SUS.checkIfNulls("null HTTPMessageConfigInterface", hmci);
        this.callback = callback;
        this.flip = flip;
        this.hmci = hmci;
        try {
            init();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException(e);
        }
    }


    /**
     * Finalize request configuration and attach SSL context if needed.
     * Forces HTTP/1.1, sets the {@code Host} header from the URL, appends the
     * {@code Authorization} header when configured, and records the remote
     * address so the NIO dispatcher knows where to connect.
     */
    private void init() throws NoSuchAlgorithmException, KeyManagementException {
        hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);
        URLInfo urlInfo = hmci.toURLInfo();
        if (hmci.isSSL()) {
            SSLContextInfo sslContextInfo = new SSLContextInfo(urlInfo.ipAddress, hmci.isSecureCheckEnabled());
            setSSLContextInfo(sslContextInfo);
        }

        hmci.getHeaders().add("Host", urlInfo.ipAddress.getInetAddress());

        if (hmci.getAuthorization() != null)
            hmci.getHeaders().add(hmci.getAuthorization().toHTTPHeader());

        setRemoteAddress(urlInfo.ipAddress);

    }

    /**
     * Mark the request-start time as {@code System.currentTimeMillis()}.
     * Call this immediately before submitting the session for dispatch so that
     * {@link #getDuration()} and the {@link HTTPResponseData} duration field
     * reflect the true round-trip latency.
     *
     * @return this, for chaining
     */
    public HTTPURLCallback updateTimeStamp() {
        return updateTimeStamp(System.currentTimeMillis());
    }

    /**
     * Mark the request-start time as the given epoch millisecond value.
     *
     * @param timeStamp absolute start time in milliseconds since the epoch
     * @return this, for chaining
     */
    public HTTPURLCallback updateTimeStamp(long timeStamp) {
        ts.set(timeStamp);
        return this;
    }

    /**
     * Replace the completion callback. Safe to call after construction — the
     * field is volatile and writes through under synchronized to coordinate
     * with concurrent response delivery in {@link #accept(ByteBuffer)}.
     *
     * @param callback new completion handler (or {@code null} to discard the response)
     * @return this, for chaining
     */
    public synchronized HTTPURLCallback setCallback(ConsumerCallback<HTTPResponse> callback) {
        this.callback = callback;
        return this;

    }

    /**
     * @return the current completion callback, or {@code null} if none is set
     */
    public ConsumerCallback<HTTPResponse> getCallback() {
        return callback;
    }

    /**
     * @return milliseconds elapsed since the last {@link #updateTimeStamp()}; zero if no timestamp has been recorded
     */
    public long getDuration() {
        return System.currentTimeMillis() - ts.get();
    }

    /**
     * Incremental response-bytes handler, called by the NIO dispatcher as
     * plaintext bytes (post-TLS decryption if https) arrive.
     * <p>
     * Each call feeds {@code byteBuffer} to {@link HTTPRawMessage#parseResponse}.
     * When {@code parseResponse} returns {@code true} (full response received),
     * the parsed message is finalized, the session is closed, and the registered
     * {@link ConsumerCallback} is invoked with an {@link HTTPResponseData}
     * containing status, headers, body, and elapsed duration.
     * </p>
     * <p>
     * On any exception during parse or callback, the session is closed and
     * {@link ConsumerCallback#exception} is signalled (if a callback is set).
     * </p>
     *
     * @param byteBuffer next chunk of response bytes
     */
    @Override
    public void accept(ByteBuffer byteBuffer) {
        try {
            if (hrm.parseResponse(hmci.getURIScheme(), byteBuffer, flip)) {
                HTTPMessageConfigInterface respHMCI = hrm.parse();
                respHMCI.setContent(hrm.getDataStream().toByteArray());
                SharedIOUtil.close(this);
                if (callback != null) {
                    callback.accept(new HTTPResponseData(respHMCI.getHTTPStatusCode().CODE, respHMCI.getHeaders(), respHMCI.getContent(), System.currentTimeMillis() - ts.get()).setCorrelationID(getID()));
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            SharedIOUtil.close(this);
            if (callback != null)
                callback.exception(e);
        }
    }


    /**
     * Connection-established hook. Instantiates the incremental response parser
     * and serializes the configured request to the channel's output stream.
     * Invoked once the TCP (and TLS, if https) handshake has completed.
     *
     * @throws IOException if writing the request to the channel fails
     */
    @Override
    protected void connectedFinished() throws IOException {
        hrm = new HTTPRawMessage(true);
        HTTPRawFormatter hrf = new HTTPRawFormatter(hmci);
        getOutputStream().write(hrf.format(), true);
        if (log.isEnabled())
            log.getLogger().info(getRemoteAddress() + " " + ((SocketChannel) getChannel()).isConnected());

    }

    /**
     * Error hook invoked by the NIO dispatcher on session-level failures
     * (connect failure, I/O error, SSL failure). Forwards to the registered
     * {@link ConsumerCallback#exception}; falls back to printing the stack
     * trace if no callback is set.
     *
     * @param e the error
     */
    @Override
    public void exception(Throwable e) {
        if (callback != null)
            callback.exception(e);
        else
            e.printStackTrace();

    }
}
