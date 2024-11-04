package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.shared.util.RateCounter;

import javax.net.ssl.SSLEngineResult;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class SSLDataReadyState
        extends State
{



    static class NotHandshaking extends TriggerConsumer<SSLSessionCallback>
    {
        static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");
        NotHandshaking() {
            super(NOT_HANDSHAKING);
        }

        @Override
        public void accept(SSLSessionCallback callback)
        {
            long ts = System.currentTimeMillis();
            SSLSessionConfig config = (SSLSessionConfig)getStateMachine().getConfig();
           if(log.isEnabled())
                log.getLogger().info("" + config.getHandshakeStatus());

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

                        if(callback != null)
                            callback.exception(e);

                        config.close();
                    }
                }
                else
                    publishSync(config.getHandshakeStatus(), callback);

            }
            ts = System.currentTimeMillis() - ts;
            rcNotHandshaking.register(ts);
        }
    }




    public SSLDataReadyState()
    {
        super(SSLStateMachine.SessionState.DATA_READY);
        register(new NotHandshaking());
    }

}
