package org.zoxweb.server.net.common;

import org.zoxweb.shared.task.ExceptionCallback;
import org.zoxweb.shared.io.CloseableType;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

/**
 * Contract between NIOSocket and a connection's protocol handler: NIOSocket owns the selector
 * loop and channel plumbing, the callback owns everything protocol-specific.
 * <p>
 * Lifecycle, in NIOSocket invocation order:
 * <ol>
 * <li>{@link #setChannel(Channel)} binds the channel to the callback, for client connections
 * before the connect completes (the remote address may not be resolvable yet).</li>
 * <li>{@link #connected(SelectionKey)} runs once the connection is fully established, strictly
 * before any read dispatch; the returned interest ops are installed on the key.</li>
 * <li>{@link #accept(SelectionKey)} per readiness dispatch; NIOSocket zeroes the key's interest
 * ops for the duration of the call, so dispatches are serialized per session.</li>
 * <li>{@link ExceptionCallback#exception(Throwable)} on failures (e.g. connect timeout or error);
 * NIOSocket may close the channel but not the callback — the callback is responsible for
 * releasing its own resources.</li>
 * <li>{@link CloseableType} close/isClosed terminate and report the session state.</li>
 * </ol>
 * Reference implementations: {@code TCPSessionCallback}, {@code TCPSMCallback} (D = DataPacket),
 * {@code UDPSessionCallback}.
 *
 * @param <D> the data unit the callback consumes, e.g. ByteBuffer or DataPacket
 */
public interface ConnectionCallback<D>
        extends ExceptionCallback, SKHandler, CloseableType {
    /**
     * Called when incoming data or something to do
     * @param key the input argument
     */
    void accept(SelectionKey key);

    /**
     * Consumes one data unit, typically invoked by {@link #accept(SelectionKey)} after framing
     * the raw bytes, or directly by a delivery layer (e.g. SSL decrypted data).
     *
     * @param dataUnit the data unit to consume
     * @throws IOException in case of error
     */
    void accept(D dataUnit) throws IOException;

    /**
     * When the connection is fully established this method will be invoked
     * @param key selection associated with the channel in question
     * @return the selection interested ops READ WRITE etc
     * @throws IOException in case of error
     */
    int connected(SelectionKey key) throws IOException;

    /**
     * @return the channel bound via {@link #setChannel(Channel)}
     */
    <V extends Channel> V getChannel();

    /**
     * Binds the connection's channel to the callback, called by NIOSocket at channel creation;
     * for client connections the channel may not be connected yet.
     *
     * @param channel the connection's channel
     * @throws IOException in case of error
     */
    void setChannel(Channel channel) throws IOException;

    /**
     * Invoked when the SSL handshake completed, gives the callback a chance to switch its
     * delivery path or output stream to the secured channel.
     *
     * @throws IOException in case of error
     */
    void sslHandshakeSuccessful() throws IOException;

}
