package org.zoxweb.server.net.ssl;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

class CustomSSLStateMachine
    implements SSLDispatcher, Closeable, Identifier<Long>
{
    public static final LogWrapper log = new LogWrapper(CustomSSLStateMachine.class).setEnabled(false);

    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcFinished = new RateCounter("Finished");

//    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private static final AtomicLong counter = new AtomicLong();
    private final SSLSessionConfig config;
    private final SSLNIOSocket sslns;
    private final long id;



    public static long getIDCount()
    {
        return counter.get();
    }
    private final Map<Enum<?>, Consumer<SSLSessionCallback>> statesCallback = new LinkedHashMap<Enum<?>, Consumer<SSLSessionCallback>>();
    CustomSSLStateMachine(SSLNIOSocket sslns)
    {
        this.sslns = sslns;
        this.config = sslns.getConfig();
        statesCallback.put(NEED_WRAP, new NeedWrap());
        statesCallback.put(NEED_UNWRAP, new NeedUnwrap());
        statesCallback.put(FINISHED, new Finished());
        statesCallback.put(NEED_TASK, new NeedTask());
        statesCallback.put(NOT_HANDSHAKING, new NotHandshaking());
        this.config.sslDispatcher = this;
        id = counter.incrementAndGet();
    }

    @Override
    public void close() throws IOException {
        config.close();
    }

    public Long getID()
    {
        return id;
    }
//    @Override
//    public void close()
//    {
//        if (!isClosed.getAndSet(true))
//        {
//            if(config.sslEngine != null)
//            {
//
//                try
//                {
//                    config.sslEngine.closeOutbound();
//                    while (!config.forcedClose && config.hasBegan.get() && !config.sslEngine.isOutboundDone() && config.sslChannel.isOpen())
//                    {
//                        SSLEngineResult.HandshakeStatus hs = config.getHandshakeStatus();
//                        switch (hs)
//                        {
//                            case NEED_WRAP:
//                            case NEED_UNWRAP:
//                                dispatch(hs, null);
//                                break;
//                            default:
//                                IOUtil.close(config.sslChannel);
//                        }
//                    }
//
//                }
//                catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//            }
//
//
//            IOUtil.close(config.sslChannel);
//            IOUtil.close(config.remoteChannel);
//            config.selectorController.cancelSelectionKey(config.sslChannel);
//            config.selectorController.cancelSelectionKey(config.remoteChannel);
//            IOUtil.close(config.stateMachine);
//            ByteBufferUtil.cache(config.inSSLNetData, config.inAppData, config.outSSLNetData, config.inRemoteData);
//            IOUtil.close(config.sslOutputStream);
//
//            if (log.isEnabled()) log.getLogger().info("SSLSessionConfig-CLOSED " +Thread.currentThread() + " " +
//                    config.sslChannel);// + " Address: " + connectionRemoteAddress);
////            TaskUtil.getDefaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS, ()->
////                log.getLogger().info(SSLStateMachine.rates()));
//        }
//    }

    class NeedWrap implements Consumer<SSLSessionCallback> {
        @Override
        public void accept(SSLSessionCallback callback) {
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
                            dispatch(result.getHandshakeStatus(), callback);
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
    }


     class NeedUnwrap implements Consumer<SSLSessionCallback> {
        @Override
        public void accept(SSLSessionCallback callback) {

            long ts = System.currentTimeMillis();
            if (log.isEnabled()) log.getLogger().info("Entry: " + config.getHandshakeStatus());

            if (config.getHandshakeStatus() == NEED_UNWRAP || SharedUtil.enumName(config.getHandshakeStatus()).equals("NEED_UNWRAP_AGAIN")) {
                try {

                    int bytesRead = config.sslChannel.read(config.inSSLNetData);
                    if (bytesRead == -1) {
                        if (log.isEnabled())
                            log.getLogger().info("SSLCHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytes read: " + bytesRead);
                        config.close();
                    } else //if (bytesRead > 0)
                    {

                        // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                        // data
                        if (log.isEnabled())
                            log.getLogger().info("BEFORE-UNWRAP: " + config.inSSLNetData + " bytes read " + bytesRead);
                        SSLEngineResult result = config.smartUnwrap(config.inSSLNetData, ByteBufferUtil.EMPTY);


                        if (log.isEnabled())
                            log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING: " + result + " bytes read: " + bytesRead);
                        if (log.isEnabled())
                            log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING inNetData: " + config.inSSLNetData + " inAppData: " + config.inAppData);

                        switch (result.getStatus()) {
                            case BUFFER_UNDERFLOW:
                                // no incoming data available we need to wait for more socket data
                                // return and let the NIOSocket or the data handler call back
                                // config.sslChannelSelectableStatus.set(true);
                                // config.sslRead.set(true);
                                return;
                            case BUFFER_OVERFLOW:
                                throw new IllegalStateException("NEED_UNWRAP should never happen: " + result.getStatus());
                                // this should never happen
                            case OK:
                                dispatch(result.getHandshakeStatus(), callback);
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
    }

     class NeedTask implements Consumer<SSLSessionCallback>
    {
        @Override
        public void accept(SSLSessionCallback callback)
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
            dispatch(status, callback);

        }
    }





     class Finished implements Consumer<SSLSessionCallback>
    {
        @Override
        public void accept(SSLSessionCallback callback)
        {
            long ts = System.currentTimeMillis();

            // ********************************************
            // Very crucial steps
            // ********************************************
        if(config.remoteAddress != null)
        {
            // we have a SSL tunnel
            sslns.createRemoteConnection();
        }

            if (config.inSSLNetData.position() > 0)
            {
                //**************************************************
                // ||-----DATA BUFFER------ ||
                // ||Handshake data|App data||
                // ||-----------------------||
                // The buffer has app data that needs to be decrypted
                //**************************************************
                dispatch(config.getHandshakeStatus(), callback);
            }

            ts = System.currentTimeMillis() - ts;
            rcFinished.register(ts);

        }
    }

     class NotHandshaking implements Consumer<SSLSessionCallback>
    {
        @Override
        public void accept(SSLSessionCallback callback)
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
                            log.getLogger().info("SSLCHANNEL-CLOSED-NOT_HANDSHAKING: " + config.getHandshakeStatus() + " bytesread: " + bytesRead);
                            config.close();
                        }
                        else
                        {
                            // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                            // data

                            SSLEngineResult result = config.smartUnwrap(config.inSSLNetData, config.inAppData);


                            if (log.isEnabled())
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesread: " + bytesRead + " callback: " + callback);
                            switch (result.getStatus())
                            {
                                case BUFFER_UNDERFLOW:
                                    // no incoming data available we need to wait for more socket data
                                    // return and let the NIOSocket or the data handler call back
                                    return;

                                case BUFFER_OVERFLOW:
                                    throw new IllegalStateException("NOT_HANDSHAKING should never be " + result.getStatus());
                                    // this should never happen
                                case OK:

                                    if(callback != null) callback.accept(config.inAppData);
                                    // config.sslRead.set(true);
                                    break;
                                case CLOSED:
                                    // check result here
                                    if(log.isEnabled()) log.getLogger().info("CLOSED-DURING-NOT_HANDSHAKING: " + result + " bytesread: " + bytesRead);
                                    config.close();
                                    break;
                            }
                        }
                    } catch (Exception e) {

                        if(callback != null)
                            callback.exception(e);

                        config.close();
                    }
                }
                else
                    dispatch(config.getHandshakeStatus(), callback);

            }
            ts = System.currentTimeMillis() - ts;
            rcNotHandshaking.register(ts);
        }
    }


    public void dispatch(SSLEngineResult.HandshakeStatus status, SSLSessionCallback callback)
    {
        Consumer<SSLSessionCallback> function = statesCallback.get(status);
        if(function != null)
            function.accept(callback);
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
                return (T) rcNotHandshaking;
//            case "SSL_CONNECTION_COUNT":
//                return (T) Long.valueOf(counter.get());


        }
        return null;
    }

}