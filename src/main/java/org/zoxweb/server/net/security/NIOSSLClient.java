package org.zoxweb.server.net.security;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * An SSL/TLS client that connects to a server using its IP address and port.
 * <p/>
 * After initialization of a {@link NIOSSLClient} object, {@link NIOSSLClient#connect()} should be called,
 * in order to establish connection with the server.
 * <p/>
 * When the connection between the client and the object is established, {@link NIOSSLClient} provides
 * a public write and read method, in order to communicate with its peer. 
 *
 * @author <a href="mailto:alex.a.karnezis@gmail.com">Alex Karnezis</a>
 */
public class NIOSSLClient extends NIOSSLPeer {
	
    /**
     * The remote address of the server this client is configured to connect to.
     */
	private String remoteAddress;

	/**
	 * The port of the server this client is configured to connect to.
	 */
	private int port;

	/**
	 * The engine that will be used to encrypt/decrypt data between this client and the server.
	 */
    private SSLEngine engine;

    /**
     * The socket channel that will be used as the transport link between this client and the server.
     */
    private SocketChannel socketChannel;


    /**
     * Initiates the engine to run as a client using peer information, and allocates space for the
     * buffers that will be used by the engine.
     *
     * @param context The SSL/TLS protocol to be used. Java 1.6 will only run with up to TLSv1 protocol. Java 1.7 or higher also supports TLSv1.1 and TLSv1.2 protocols.
     * @param remoteAddress The IP address of the peer.
     * @param port The peer's port that will be used.
     * @throws Exception
     */
    public NIOSSLClient(Executor executor, SSLContext context, String remoteAddress, int port) throws Exception  {
        super(executor);
    	this.remoteAddress = remoteAddress;
    	this.port = port;


        engine = context.createSSLEngine(remoteAddress, port);
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        //myAppData = ByteBuffer.allocate(1024);
        outNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        //peerAppData = ByteBuffer.allocate(1024);
        inNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    /**
     * Opens a socket channel to communicate with the configured server and tries to complete the handshake protocol.
     *
     * @return True if client established a connection with the server, false otherwise.
     * @throws Exception
     */
    public boolean connect() throws Exception {
    	socketChannel = SocketChannel.open();
    	socketChannel.configureBlocking(false);
    	socketChannel.connect(new InetSocketAddress(remoteAddress, port));
    	while (!socketChannel.finishConnect()) {
    		// can do something here...
    	}

    	engine.beginHandshake();
    	return doHandshake(socketChannel, engine);
    }

    /**
     * Public method to send a message to the server.
     *
     * @param message - message to be sent to the server.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
//    public void write(String message) throws IOException {
//        write(socketChannel, engine, new ByteBuffer());
//    }

    /**
     * Implements the write method that sends a message to the server the client is connected to,
     * but should not be called by the user, since socket channel and engine are inner class' variables.
     *
     * @param myAppData - message to be sent to the server.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, ByteBuffer myAppData) throws IOException {

        log.info("About to write to the server...");

//        myAppData.clear();
//
//        myAppData.put(message);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            outNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, outNetData);
            switch (result.getStatus()) {
            case OK:
                outNetData.flip();
                while (outNetData.hasRemaining()) {
                    socketChannel.write(outNetData);
                }

                break;
            case BUFFER_OVERFLOW:
                outNetData = enlargePacketBuffer(engine, outNetData);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
            case CLOSED:
                closeConnection(socketChannel, engine);
                return;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

    }

    /**
     * Public method to try to read from the server.
     *
     * @throws Exception
     */
    public ByteBuffer  read(ByteBuffer buffer) throws Exception {
        return read(socketChannel, engine, buffer);
    }

    /**
     * Will wait for response from the remote peer, until it actually gets something.
     * Uses {@link SocketChannel#read(ByteBuffer)}, which is non-blocking, and if
     * it gets nothing from the peer, waits for {@code waitToReadMillis} and tries again.
     * <p/>
     * Just like {@link NIOSSLClient#read(SocketChannel, SSLEngine, ByteBuffer)} it uses inner class' socket channel
     * and engine and should not be used by the client. {@link NIOSSLClient#read(ByteBuffer)} should be called instead.
     * 
     * @param socketChannel - message to be sent to the server.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws Exception
     */
    @Override
    protected ByteBuffer read(SocketChannel socketChannel, SSLEngine engine, ByteBuffer peerAppData) throws Exception  {

        log.info("About to read from the server...");

        inNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = socketChannel.read(inNetData);
            if (bytesRead > 0) {
                inNetData.flip();
                while (inNetData.hasRemaining()) {
                    peerAppData.clear();
                    SSLEngineResult result = engine.unwrap(inNetData, peerAppData);
                    switch (result.getStatus()) {
                    case OK:
                        //peerAppData.flip();


                        return peerAppData;

                    case BUFFER_OVERFLOW:
                        peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        inNetData = handleBufferUnderflow(engine, inNetData);
                        break;
                    case CLOSED:
                        closeConnection(socketChannel, engine);
                        return null;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(socketChannel, engine);
                return null;
            }
            Thread.sleep(waitToReadMillis);
        }
        return null;
    }

    /**
     * Should be called when the client wants to explicitly close the connection to the server.
     *
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void shutdown() throws IOException {
        log.info("About to close connection with the server...");
        closeConnection(socketChannel, engine);

        log.info("Goodbye!");
    }

}
