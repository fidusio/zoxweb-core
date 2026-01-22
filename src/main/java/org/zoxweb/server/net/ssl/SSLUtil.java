package org.zoxweb.server.net.ssl;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.common.ConnectionCallback;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLEngineResult;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public final class SSLUtil {
    public static final LogWrapper log = new LogWrapper(SSLUtil.class).setEnabled(false);

    private SSLUtil() {
    }

    public static long _notHandshaking(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("" + config.getHandshakeStatus());

        if (config.sslChannel.isOpen()) {
            if (config.getHandshakeStatus() == NOT_HANDSHAKING) {
                try {
                    int bytesRead = config.sslChannel.read(config.inSSLNetData);
                    if (bytesRead == -1) {
                        if (log.isEnabled())
                            log.getLogger().info("SSLCHANNEL-CLOSED-NOT_HANDSHAKING: " + config.getHandshakeStatus() + " bytesRead: " + bytesRead);
                        config.close();
                    } else {
                        SSLEngineResult result;
                        // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                        // data
                        do {
                            result = config.smartUnwrap(config.inSSLNetData, config.inAppData);
                            if (log.isEnabled())
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                            switch (result.getStatus()) {
                                case BUFFER_UNDERFLOW:
                                    // no incoming data available we need to wait for more socket data
                                    // return and let the NIOSocket or the data handler call back
                                    if (log.isEnabled())
                                        log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);

                                    return System.currentTimeMillis() - ts;

                                case BUFFER_OVERFLOW:
                                    throw new IllegalStateException("NOT_HANDSHAKING should never be " + result.getStatus());
                                    // this should never happen
                                case OK:
                                    // check if we have data to process
                                    if (callback != null && bytesRead >= 0 && result.bytesProduced() > 0) {
                                        // we have decrypted data to process
                                        callback.accept(config.inAppData);
                                    }
                                    break;
                                case CLOSED:
                                    // closed result here
                                    if (log.isEnabled())
                                        log.getLogger().info("CLOSED-DURING-NOT_HANDSHAKING: " + result + " bytesRead: " + bytesRead);
                                    config.close();
                                    break;
                            }
                        }// check if we still have encrypted data to process
                        while (config.inSSLNetData.hasRemaining() && !config.isClosed());


                    }
                } catch (Exception e) {
                    if (log.isEnabled())
                        e.printStackTrace();

                    if (callback != null)
                        callback.exception(e);

                    config.close();
                }
            } else
                config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);

        }
        return System.currentTimeMillis() - ts;
    }


    public static long _finished(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();

        // ********************************************
        // Very crucial steps
        // ********************************************
        if (config.remoteConnection != null) {
            // we have SSL tunnel
            config.sslConnectionHelper.createRemoteConnection();
        }

        if (config.clientMode && callback instanceof ConnectionCallback) {
            /*
             * special case if the connection is a client connection
             */
            ((ConnectionCallback) callback).sslHandshakeSuccessful();
        }

        if (config.inSSLNetData.position() > 0) {
            //**************************************************
            // ||-----DATA BUFFER------ ||
            // ||Handshake data|App data||
            // ||-----------------------||
            // The buffer has app data that needs to be decrypted
            //**************************************************
            config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);
        }

        return System.currentTimeMillis() - ts;
    }

    public static long _needTask(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();

        Runnable toRun;
        while ((toRun = config.getDelegatedTask()) != null)
            toRun.run();

        SSLEngineResult.HandshakeStatus status = config.getHandshakeStatus();

        ts = System.currentTimeMillis() - ts;

        if (log.isEnabled()) log.getLogger().info("After run: " + status);

        config.sslConnectionHelper.publish(status, callback);

        return ts;
    }


    public static long _needUnwrap(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {

        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("Entry: " + config.getHandshakeStatus());

        if (config.getHandshakeStatus() == NEED_UNWRAP || SUS.enumName(config.getHandshakeStatus()).equals("NEED_UNWRAP_AGAIN")) {
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


                    if (log.isEnabled()) {
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING: " + result + " bytes read: " + bytesRead);
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING inNetData: " + config.inSSLNetData + " inAppData: " + config.inAppData);
                    }

                    switch (result.getStatus()) {
                        case BUFFER_UNDERFLOW:
                            // no incoming data available we need to wait for more socket data
                            // return and let the NIOSocket or the data handler call back
                            // config.sslChannelSelectableStatus.set(true);
                            // config.sslRead.set(true);
                            return System.currentTimeMillis() - ts;
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
        return System.currentTimeMillis() - ts;
    }


    public static long _needWrap(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
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
        return System.currentTimeMillis() - ts;
    }
}
