package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.MonoStateMachine;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.*;

import javax.net.ssl.SSLEngineResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

class CustomSSLStateMachine extends MonoStateMachine<SSLEngineResult.HandshakeStatus, SSLSessionCallback>
    implements SSLConnectionHelper, Closeable, Identifier<Long>
{
    public static final LogWrapper log = new LogWrapper(CustomSSLStateMachine.class).setEnabled(false);

    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcFinished = new RateCounter("Finished");

    private static final AtomicLong counter = new AtomicLong();
    private final SSLSessionConfig config;
    private final SSLNIOSocketHandler sslns;
    private final long id;

    public static long getIDCount()
    {
        return counter.get();
    }

    CustomSSLStateMachine(SSLNIOSocketHandler sslns)
    {
        super(false);
        this.sslns = sslns;
        this.config = sslns.getConfig();
        this.config.sslConnectionHelper = this;
        id = counter.incrementAndGet();
        register(NOT_HANDSHAKING, this::notHandshaking)
                .register(NEED_WRAP, this::needWrap)
                .register(NEED_UNWRAP, this::needUnwrap)
                .register(FINISHED, this::finished)
                .register(NEED_TASK, this::needTask)
        ;
    }

    @Override
    public void close() throws IOException
    {
        config.close();
    }

    public Long getID()
    {
        return id;
    }





    public void needWrap(SSLSessionCallback callback) {
        long ts = System.currentTimeMillis();

        if (config.getHandshakeStatus() == NEED_WRAP) {
            try {
                SSLEngineResult result = config.smartWrap(ByteBufferUtil.EMPTY, config.outSSLNetData);
                // at handshake stage, data in appOut won't be
                // processed hence dummy buffer
                if (log.isEnabled())
                    log.getLogger().info("AFTER-NEED_WRAP-HANDSHAKING: " + result);

                switch (result.getStatus()) {
                    case BUFFER_UNDERFLOW:
                    case BUFFER_OVERFLOW:
                        config.forcedClose = true;
                        throw new IllegalStateException(result + " invalid state context " + config.outSSLNetData + " " + config.sslChannel.getRemoteAddress());
                    case OK:
                        int written = ByteBufferUtil.smartWrite(null, config.sslChannel, config.outSSLNetData);
                        if (log.isEnabled())
                            log.getLogger().info(result.getHandshakeStatus() + " After writing data HANDSHAKING-NEED_WRAP: " + config.outSSLNetData + " written:" + written);
                        config.sslConnectionHelper.publish(result.getHandshakeStatus(), callback);
                        break;
                    case CLOSED:
                        config.close();
                        break;
                }
            } catch (Exception e) {
                if (log.isEnabled())
                    e.printStackTrace();

                config.close();
            }
        }
        ts = System.currentTimeMillis() - ts;
        rcNeedWrap.register(ts);
    }


    public void needUnwrap(SSLSessionCallback callback) {

        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("Entry: " + config.getHandshakeStatus());

        if (config.getHandshakeStatus() == NEED_UNWRAP || SUS.enumName(config.getHandshakeStatus()).equals("NEED_UNWRAP_AGAIN")) {
            try {

                int bytesRead = config.sslChannel.read(config.inSSLNetData);
                if (bytesRead == -1)
                {
                    if (log.isEnabled())
                        log.getLogger().info("SSLCHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytes read: " + bytesRead);
                    config.close();
                }
                else //if (bytesRead > 0)
                {
                    // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                    // data
                    if (log.isEnabled())
                        log.getLogger().info("BEFORE-UNWRAP: " + config.inSSLNetData + " bytes read " + bytesRead);
                    SSLEngineResult result = config.smartUnwrap(config.inSSLNetData, ByteBufferUtil.EMPTY);


                    if (log.isEnabled())
                    {
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING: " + result + " bytes read: " + bytesRead);
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING inNetData: " + config.inSSLNetData + " inAppData: " + config.inAppData);
                    }

                    switch (result.getStatus()) {
                        case BUFFER_UNDERFLOW:
                            // no incoming data available we need to wait for more socket data
                            // return and let the NIOSocket or the data handler call back
                            // config.sslChannelSelectableStatus.set(true);
                            // config.sslChannelSelectableStatus.set(true);
                            // config.sslRead.set(true);
                            return;
                        case BUFFER_OVERFLOW:
                            throw new IllegalStateException("NEED_UNWRAP should never happen: " + result.getStatus());
                            // this should never happen
                        case OK:
                            config.sslConnectionHelper.publish(result.getHandshakeStatus(), callback);
                            break;
                        case CLOSED:
                            // check result here
                            if (log.isEnabled())
                                log.getLogger().info("CLOSED-DURING-NEED_UNWRAP: " + result + " bytes read: " + bytesRead);
                            config.close();
                            break;
                    }
                }
            } catch (Exception e) {
                if (log.isEnabled())
                    e.printStackTrace();
                config.close();
            }
        }
        ts = System.currentTimeMillis() - ts;
        rcNeedUnwrap.register(ts);
    }

    public void needTask(SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();

        Runnable toRun;
        while((toRun = config.getDelegatedTask()) != null)
        {
            toRun.run();

        }
        SSLEngineResult.HandshakeStatus status = config.getHandshakeStatus();

        ts = System.currentTimeMillis() - ts;
        if (log.isEnabled())
            log.getLogger().info("After run: " + status);
        rcNeedTask.register(ts);
        config.sslConnectionHelper.publish(status, callback);
    }





    public void finished(SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();

        // ********************************************
        // Very crucial steps
        // ********************************************
        if(config.remoteConnection != null)
        {
            // we have a SSL tunnel
            config.sslConnectionHelper.createRemoteConnection();
        }

        if (config.inSSLNetData.position() > 0)
        {
            //**************************************************
            // ||-----DATA BUFFER------ ||
            // ||Handshake data|App data||
            // ||-----------------------||
            // The buffer has app data that needs to be decrypted
            //**************************************************
            config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);
        }

        ts = System.currentTimeMillis() - ts;
        rcFinished.register(ts);
    }

    public void notHandshaking(SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();
        if(log.isEnabled()) log.getLogger().info("" + config.getHandshakeStatus());

        if(config.sslChannel.isOpen())
        {
            if(config.getHandshakeStatus() == NOT_HANDSHAKING)
            {
                try
                {
                    int bytesRead = config.sslChannel.read(config.inSSLNetData);
                    if (bytesRead == -1)
                    {
                        if (log.isEnabled()) log.getLogger().info("SSLCHANNEL-CLOSED-NOT_HANDSHAKING: " + config.getHandshakeStatus() + " bytesRead: " + bytesRead);
                        config.close();
                    }
                    else
                    {
                        // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                        // data

                        SSLEngineResult result;
                        do
                        {
                            result = config.smartUnwrap(config.inSSLNetData, config.inAppData);


                            if (log.isEnabled())
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                            switch (result.getStatus()) {
                                case BUFFER_UNDERFLOW:
                                    // no incoming data available we need to wait for more socket data
                                    // return and let the NIOSocket or the data handler call back
                                    //log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                                    return;

                                case BUFFER_OVERFLOW:
                                    throw new IllegalStateException("NOT_HANDSHAKING should never be " + result.getStatus());
                                    // this should never happen
                                case OK:

                                    if (callback != null && bytesRead > 0) {
                                        callback.accept(config.inAppData);
                                        if(log.isEnabled())log.getLogger().info("AFTER-NOT_HANDSHAKING-OK: " + result + " bytesRead: " + bytesRead + " callback: " + callback);

                                    }
                                    // config.sslRead.set(true);
                                    break;
                                case CLOSED:
                                    // check result here
                                    if (log.isEnabled())
                                        log.getLogger().info("CLOSED-DURING-NOT_HANDSHAKING: " + result + " bytesRead: " + bytesRead);
                                    config.close();
                                    break;
                            }
                        }while(config.inSSLNetData.hasRemaining() && result.getStatus() == SSLEngineResult.Status.OK && config.getHandshakeStatus() == NOT_HANDSHAKING && callback != null && bytesRead > 0);

                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if(callback != null)
                        callback.exception(e);

                    config.close();
                }
            }
            else
                config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);

        }
        ts = System.currentTimeMillis() - ts;
        rcNotHandshaking.register(ts);
    }


//    public void dispatch(SSLEngineResult.HandshakeStatus status, SSLSessionCallback callback)
//    {
//        //stateMap.get(status).accept(callback);
//        switch(status)
//        {
//
//            case NOT_HANDSHAKING:
//                notHandshaking(callback);
//                break;
//            case FINISHED:
//                finished(callback);
//                break;
//            case NEED_TASK:
//                needTask(callback);
//                break;
//            case NEED_WRAP:
//                needWrap(callback);
//                break;
//            case NEED_UNWRAP:
//                needUnwrap(callback);
//                break;
//        }
//    }

    @Override
    public void createRemoteConnection()
    {
        sslns.createRemoteConnection();
    }

    public SSLSessionConfig getConfig()
    {
        return sslns.getConfig();
    }


    public static String rates()
    {
        return SharedUtil.toCanonicalID(',', rcNeedWrap, rcNeedUnwrap, rcNeedTask, rcFinished, rcNotHandshaking);
    }

    public static <T> T lookupType(String type)
    {
        type = SharedStringUtil.toUpperCase(type);
        switch(type)
        {
            case "NEED_WRAP":
                return (T) rcNeedWrap;
            case "NEED_UNWRAP":
                return (T) rcNeedUnwrap;
            case "NEED_TASK":
                return (T) rcNeedTask;
            case "FINISHED":
                return (T) rcFinished;
            case "NOT_HANDSHAKING":
                return (T)rcNotHandshaking;
            default:
                System.out.println("***************************************** SHIT: "  + type);
        }
        return null;
    }

}
