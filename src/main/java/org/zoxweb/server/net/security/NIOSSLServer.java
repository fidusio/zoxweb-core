package org.zoxweb.server.net.security;

import org.zoxweb.server.io.ByteBufferUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;


public class NIOSSLServer extends NIOSSLPeer {
	



    private int appDataBufferSize;

    public NIOSSLServer(Executor executor,  SSLContext context)  {
        super(executor);
        //this.context = context;
        //this.selector = sc;

        SSLSession dummySession = context.createSSLEngine().getSession();
        //myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        //peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());




        inNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        outNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
//        inNetData = ByteBuffer.allocate(4096*2);
//        outNetData = ByteBuffer.allocate(4096*2);

        appDataBufferSize = dummySession.getApplicationBufferSize();
        dummySession.invalidate();

       // active = true;
        
    }


    public int appDataBufferSize()
    {
        return appDataBufferSize;
    }

    /**
     * Should be called in order the server to start listening to new connections.
     * This method will run in a loop as long as the server is active. In order to stop the server
     * you should use {@link NIOSSLServer#stop()} which will set it to inactive state
     * and also wake up the listener, which may be in blocking select() state.
     *
     * @throws Exception
     */
//    public void start() throws Exception {
//
//    	log.info("Initialized and waiting for new connections...");
//
//        while (isActive()) {
//            selector.select();
//            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
//            while (selectedKeys.hasNext()) {
//                SelectionKey key = selectedKeys.next();
//                selectedKeys.remove();
//                if (!key.isValid()) {
//                    continue;
//                }
//                if (key.isAcceptable()) {
//                    acceptConnection(key);
//                } else if (key.isReadable()) {
//                    read((SocketChannel) key.channel(), (SSLEngine) key.attachment());
//                }
//            }
//        }
//
//        log.info("Goodbye!");
//
//    }
    
    /**
     * Sets the server to an inactive state, in order to exit the reading loop in {@link NIOSSLServer#start()}
     * and also wakes up the selector, which may be in select() blocking state.
     */
//    public void stop() {
//    	log.info("Will now close server...");
//    	active = false;
//
//    	selector.wakeup();
//    }

    /**
     * Will be called after a new connection request arrives to the server. Creates the {@link SocketChannel} that will
     * be used as the network layer link, and the {@link SSLEngine} that will encrypt and decrypt all the data
     * that will be exchanged during the session with this specific client.
     *
     * @param key - the key dedicated to the {@link ServerSocketChannel} used by the server to listen to new connection requests.
     * @throws Exception
     */
//    public void acceptConnection(SelectionKey key) throws Exception {

//    	log.info("New connection request!");
//        // this needs to retrofitted
//        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
//        socketChannel.configureBlocking(false);
//
//        SSLEngine engine = context.createSSLEngine();
//        engine.setUseClientMode(false);
//        engine.beginHandshake();
//
//        if (doHandshake(socketChannel, engine)) {
//            socketChannel.register(selector.getSelector(), SelectionKey.OP_READ, engine);
//        } else {
//            socketChannel.close();
//            log.info("Connection closed due to handshake failure.");
//        }
//
//        return engine;
//        }


    /**
     * Will be called by the selector when the specific socket channel has data to be read.
     * As soon as the server reads these data, it will call {@link NIOSSLServer#write(SocketChannel, SSLEngine, ByteBuffer)}
     * to send back a trivial response.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Override
    public ByteBuffer read(SocketChannel socketChannel, SSLEngine engine, ByteBuffer peerAppData) throws IOException {

        log.info("About to read from a client...");

        inNetData.clear();

        int bytesRead = socketChannel.read(inNetData);
        if (bytesRead > 0) {
            inNetData.flip();
            peerAppData.clear();
            while (inNetData.hasRemaining()) {

                SSLEngineResult result = engine.unwrap(inNetData, peerAppData);
                log.info(Thread.currentThread()+ " READ-UNWRAP result: " + result  + " hasRemaining: " + inNetData.hasRemaining() + " " + peerAppData.position());
                switch (result.getStatus()) {
                case OK:
                    log.info(Thread.currentThread() + " READ-OK: " + result + " peerAppData pos: " + peerAppData.position());
                    break;

                case BUFFER_OVERFLOW:
                    peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                    break;
                case BUFFER_UNDERFLOW:
                    inNetData = handleBufferUnderflow(engine, inNetData);
                    break;
                case CLOSED:
                    log.info("Client wants to close connection...");
                    if(result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                    {
                        //peerAppData.flip();
                        outNetData.clear();
                        result = engine.wrap(ByteBufferUtil.EMPTY, outNetData);
                        log.info(Thread.currentThread() + " READ-CLOSED-NEED_WRAP: " + result);
                        ByteBufferUtil.write(socketChannel, outNetData);
                    }
                    //closeConnection(socketChannel, engine);

                    log.info("Goodbye client!");
                    return null;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

            //write(socketChannel, engine, "Hello! I am your server!");

            return peerAppData;

        } else if (bytesRead < 0) {
            log.info("Received end of stream. Will try to close connection with client...");
            handleEndOfStream(socketChannel, engine);
            log.info("Goodbye client!");
        }


        return null;
    }

    /**
     * Will send a message back to a client.
     *
     * @param socketChannel - the key dedicated to the socket channel that will be used to write to the client.
     * @param myAppData - the message to be sent.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Override
    public void write(SocketChannel socketChannel, SSLEngine engine, ByteBuffer myAppData) throws IOException {

        log.info("About to write to a client...");

//        myAppData.clear();
//        myAppData.put(message.getBytes());
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            outNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, outNetData);
            log.info(Thread.currentThread()+ " WRITE-WRAP result: " + result  + " hasRemaining: " + myAppData.hasRemaining() + " " + outNetData.position());
            switch (result.getStatus()) {
            case OK:
                ByteBufferUtil.write(socketChannel, outNetData);
                log.info(Thread.currentThread() + " WRITE-OK: " + result);
                break;
            case BUFFER_OVERFLOW:
                outNetData = enlargePacketBuffer(engine, outNetData);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occur after a wrap. I don't think we should ever get here.");
            case CLOSED:
                log.info(Thread.currentThread() +" WRITE-CLOSED: " + result);
                closeConnection(socketChannel, engine);
                throw new IOException("SSL connection closed.");
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }
    }

    /**
     * Determines if the  server is active or not.
     *
     * @return if the server is active or not.
     */
//    private boolean isActive() {
//        return active;
//    }
    
}
