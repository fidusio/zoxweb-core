package org.zoxweb.server.net.ssl;

import org.zoxweb.server.logging.LogWrapper;

import javax.net.ssl.SSLEngineResult;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class SSLUtil
{
    public static final LogWrapper log = new LogWrapper(SSLUtil.class).setEnabled(false);
    private SSLUtil(){}

    protected static void _notHandshaking(SSLSessionConfig config, SSLSessionCallback callback)
    {



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
                        SSLEngineResult result;
                        // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                        // data
                        do {
                            result = config.smartUnwrap(config.inSSLNetData, config.inAppData);
                            if (log.isEnabled()) {
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-OK: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-OK: ssl buffer" + config.inSSLNetData);
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-OK: data buffer" + config.inSSLNetData);
                            }

                            if (log.isEnabled())
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                            switch (result.getStatus()) {
                                case BUFFER_UNDERFLOW:
                                    // no incoming data available we need to wait for more socket data
                                    // return and let the NIOSocket or the data handler call back
                                    if (log.isEnabled()) log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                                    return;

                                case BUFFER_OVERFLOW:
                                    throw new IllegalStateException("NOT_HANDSHAKING should never be " + result.getStatus());
                                    // this should never happen
                                case OK:

                                    if (callback != null && bytesRead >= 0) {
                                        if (result.bytesProduced() > 0)
                                            callback.accept(config.inAppData);
//                                        if (config.inSSLNetData.hasRemaining() && config.sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
//                                            System.out.println(count + " need to check again " + Thread.currentThread() + " bytesRead: " + bytesRead);
//                                            _notHandshaking(callback, count);
//                                        }
//                                    else
//                                        config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);
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
                        }while(config.inSSLNetData.hasRemaining() && !config.isClosed());


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

    }
}
