package org.zoxweb.server.net.ssl;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class StaticSSLStateMachine {
    public static final LogWrapper log = new LogWrapper(StaticSSLStateMachine.class).setEnabled(false);
    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcFinished = new RateCounter("Finished");
    public static void needWrap(SSLSessionConfig config, SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();

        if (config.getHandshakeStatus() == NEED_WRAP)
        {
            try
            {
                SSLEngineResult result = config.smartWrap(ByteBufferUtil.EMPTY, config.outSSLNetData);
                // at handshake stage, data in appOut won't be
                // processed hence dummy buffer
                if (log.isEnabled())
                    log.getLogger().info("AFTER-NEED_WRAP-HANDSHAKING: " + result);

                switch (result.getStatus())
                {
                    case BUFFER_UNDERFLOW:
                    case BUFFER_OVERFLOW:
                        config.forcedClose = true;
                        throw new IllegalStateException(result + " invalid state context " + config.outSSLNetData + " " + config.sslChannel.getRemoteAddress());
                    case OK:
                        int written = ByteBufferUtil.smartWrite(null, config.sslChannel, config.outSSLNetData);
                        if (log.isEnabled())
                            log.getLogger().info(result.getHandshakeStatus() + " After writing data HANDSHAKING-NEED_WRAP: " + config.outSSLNetData + " written:" + written);
                        dispatch(result.getHandshakeStatus(), config, callback);
                        break;
                    case CLOSED:
                        config.close();
                        break;
                }
            }
            catch (Exception e)
            {
                if(log.isEnabled())
                    e.printStackTrace();

                config.close();
            }
        }
        ts = System.currentTimeMillis() - ts;
        rcNeedWrap.register(ts);
    }
    public static void needUnwrap(SSLSessionConfig config, SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();
        if(log.isEnabled()) log.getLogger().info("Entry: " + config.getHandshakeStatus());

        if (config.getHandshakeStatus() == NEED_UNWRAP || SharedUtil.enumName(config.getHandshakeStatus()).equals("NEED_UNWRAP_AGAIN"))
        {
            try {

                int bytesRead = config.sslChannel.read(config.inSSLNetData);
                if (bytesRead == -1)
                {
                    if (log.isEnabled()) log.getLogger().info("SSLCHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytes read: " + bytesRead);
                    config.close();
                }
                else //if (bytesRead > 0)
                {

                    // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                    // data
                    if (log.isEnabled()) log.getLogger().info("BEFORE-UNWRAP: " + config.inSSLNetData + " bytes read " + bytesRead);
                    SSLEngineResult result = config.smartUnwrap(config.inSSLNetData, ByteBufferUtil.EMPTY);


                    if (log.isEnabled()) log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING: " + result + " bytes read: " + bytesRead);
                    if (log.isEnabled()) log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING inNetData: " + config.inSSLNetData + " inAppData: " +  config.inAppData);

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
                            dispatch(result.getHandshakeStatus(), config, callback);
                            break;
                        case CLOSED:
                            // check result here
                            if (log.isEnabled()) log.getLogger().info("CLOSED-DURING-NEED_UNWRAP: " + result + " bytes read: " + bytesRead);
                            config.close();
                            break;
                    }
                }
            }
            catch (Exception e)
            {
                if(log.isEnabled())
                    e.printStackTrace();
                config.close();
            }
        }
        ts = System.currentTimeMillis() - ts;
        rcNeedUnwrap.register(ts);
    }

    public static void needTask(SSLSessionConfig config, SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();

        Runnable toRun;
        while((toRun = config.getDelegatedTask()) != null)
        {
            toRun.run();

        }
        SSLEngineResult.HandshakeStatus status = config.getHandshakeStatus();
        if (log.isEnabled())
            log.getLogger().info("After run: " + status);
        ts = System.currentTimeMillis() - ts;
        rcNeedTask.register(ts);
        dispatch(status, config, callback);
    }

    public static void finished(SSLSessionConfig config, SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();

        // ********************************************
        // Very crucial steps
        // ********************************************
//        if(config.remoteAddress != null)
//        {
//            // we have a SSL tunnel
//            publishSync(POST_HANDSHAKE, config);
//        }

        if (config.inSSLNetData.position() > 0)
        {
            //**************************************************
            // ||-----DATA BUFFER------ ||
            // ||Handshake data|App data||
            // ||-----------------------||
            // The buffer has app data that needs to be decrypted
            //**************************************************
            dispatch(config.getHandshakeStatus(), config, callback);
        }

        ts = System.currentTimeMillis() - ts;
        rcFinished.register(ts);
    }

    public static void notHandshaking(SSLSessionConfig config, SSLSessionCallback callback)
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
                dispatch(config.getHandshakeStatus(), config, callback);

        }
        ts = System.currentTimeMillis() - ts;
        rcNotHandshaking.register(ts);
    }

    public static void dispatch(SSLEngineResult.HandshakeStatus status, SSLSessionConfig config, SSLSessionCallback callback)
    {
        switch (status)
        {
            case NOT_HANDSHAKING:
                notHandshaking(config, callback);
                break;
            case FINISHED:
                finished(config, callback);
                break;
            case NEED_TASK:
                needTask(config, callback);
                break;
            case NEED_WRAP:
                needWrap(config, callback);
                break;
            case NEED_UNWRAP:
                needUnwrap(config, callback);
                break;
        }
    }

}
