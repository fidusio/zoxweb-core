package org.zoxweb.server.net.ssl;


import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.SelectorController;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;


public class SSLSessionConfig
    implements CloseableType

{
    public final static LogWrapper log = new LogWrapper(SSLSessionConfig.class.getName()).setEnabled(false);

    // Incoming encrypted data
    volatile ByteBuffer inSSLNetData = null;
    // Outgoing encrypted data
    volatile ByteBuffer outSSLNetData = null;
    // clear text application data
    volatile ByteBuffer inAppData = null;
    // the encrypted channel
    volatile SocketChannel sslChannel = null;
    volatile SSLChannelOutputStream sslOutputStream = null;
    volatile SelectorController selectorController = null;

    volatile SocketChannel remoteChannel = null;
    volatile ByteBuffer inRemoteData = null;
    volatile SSLConnectionHelper sslConnectionHelper = null;
    volatile boolean forcedClose = false;
    volatile IPAddress remoteConnection = null;

    // used for remote connection creation only




    //final Lock ioLock = null;//new ReentrantLock();
    final SSLEngine sslEngine; // the crypto engine


    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    final AtomicBoolean hasBegan = new AtomicBoolean(false);

    public SSLSessionConfig(SSLContextInfo sslContext)
    {
        SUS.checkIfNulls("sslContext null", sslContext);
        this.sslEngine = sslContext.newInstance();
    }

    @Override
    public void close()
    {


        if (!isClosed.getAndSet(true))
        {
//            log.getLogger().info("SSLSessionConfig-NOT-CLOSED-YET " +Thread.currentThread() + " " + sslChannel);
//            try
//            {
//                connectionRemoteAddress = sslChannel.getRemoteAddress();
//            }
//            catch (Exception e){}

            if(sslEngine != null)
            {

                try
                {
                    sslEngine.closeOutbound();
                    while (!forcedClose && hasBegan.get() && !sslEngine.isOutboundDone() && sslChannel.isOpen())
                    {
                      SSLEngineResult.HandshakeStatus hs = getHandshakeStatus();
                      switch (hs)
                      {
                        case NEED_WRAP:
                        case NEED_UNWRAP:
                          //stateMachine.publishSync(new Trigger<SSLSessionCallback>(this, hs,null,null));
                          sslConnectionHelper.publish(hs, null);
                          //stateMachine.publishSync(null, hs, null);
                          //staticSSLStateMachine.dispatch(hs, null);
                          break;
                        default:
                          IOUtil.close(sslChannel);
                      }
                    }

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }


            IOUtil.close(sslChannel);
            IOUtil.close(remoteChannel);
            selectorController.cancelSelectionKey(sslChannel);
            selectorController.cancelSelectionKey(remoteChannel);
            IOUtil.close((AutoCloseable) sslConnectionHelper);
            ByteBufferUtil.cache(inSSLNetData, inAppData, outSSLNetData, inRemoteData);
            IOUtil.close(sslOutputStream);

            if (log.isEnabled()) log.getLogger().info("SSLSessionConfig-CLOSED " +Thread.currentThread() + " " +
                    sslChannel);// + " Address: " + connectionRemoteAddress);
//            TaskUtil.getDefaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS, ()->
//                log.getLogger().info(SSLStateMachine.rates()));
        }


    }

    public boolean isClosed()
    {
        return isClosed.get();
    }


    public synchronized SSLEngineResult smartWrap(ByteBuffer source, ByteBuffer destination) throws SSLException {
        ((Buffer)source).flip();
        SSLEngineResult ret = sslEngine.wrap(source, destination);
        //if(ret.getStatus() == SSLEngineResult.Status.OK)
        source.compact();
        return ret;
    }

    public synchronized SSLEngineResult smartUnwrap(ByteBuffer source, ByteBuffer destination) throws SSLException
    {
        ((Buffer)source).flip();
        SSLEngineResult ret = sslEngine.unwrap(source, destination);
        //if(ret.getStatus() == SSLEngineResult.Status.OK)
        source.compact();
        return ret;
    }



    public void beginHandshake(boolean clientMode) throws SSLException {
        if (!hasBegan.get())
        {
            if(!hasBegan.getAndSet(true))
            {
                // set the ssl engine mode client or sever
                sslEngine.setUseClientMode(clientMode);
                // start the handshake
                sslEngine.beginHandshake();
                // create the necessary byte buffer with the proper length
                inSSLNetData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, getPacketBufferSize());
                outSSLNetData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, getPacketBufferSize());
                inAppData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, getApplicationBufferSize());
            }
        }
    }

    public int getPacketBufferSize()
    {
        return sslEngine.getSession().getPacketBufferSize();
    }

    public int getApplicationBufferSize()
    {
        return sslEngine.getSession().getApplicationBufferSize();
    }
    public SSLEngineResult.HandshakeStatus getHandshakeStatus()
    {
        return sslEngine.getHandshakeStatus();
    }


    public Runnable getDelegatedTask()
    {
        return sslEngine.getDelegatedTask();
    }

}
