package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import java.util.concurrent.atomic.AtomicLong;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class SSLHandshakingState extends State {

    private final static AtomicLong counter = new AtomicLong(0);

    static class NeedWrap extends TriggerConsumer<SSLSessionCallback>
    {
        static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
        NeedWrap()
        {
            super(NEED_WRAP);
        }
        @Override
        public void accept(SSLSessionCallback callback)
        {
            long ts = System.currentTimeMillis();
            SSLSessionConfig config = (SSLSessionConfig)getStateMachine().getConfig();
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
                          publishSync(result.getHandshakeStatus(), callback);
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

    }

    static class NeedUnwrap extends TriggerConsumer<SSLSessionCallback>
    {
        static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");

        NeedUnwrap()
        {
            super("NEED_UNWRAP", "NEED_UNWRAP_AGAIN");
        }

    @Override
    public void accept(SSLSessionCallback callback)
    {
        long ts = System.currentTimeMillis();
        SSLSessionConfig config = (SSLSessionConfig)getStateMachine().getConfig();
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
                          publishSync(result.getHandshakeStatus(), callback);
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

    }





    static class NeedTask extends TriggerConsumer<SSLSessionCallback>
    {
        static RateCounter rcNeedTask = new RateCounter("NeedTask");
        NeedTask() {
            super(NEED_TASK);
        }

        @Override
        public void accept(SSLSessionCallback callback) {
            long ts = System.currentTimeMillis();
            SSLSessionConfig config = (SSLSessionConfig)getStateMachine().getConfig();
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
            publishSync(status, callback);

        }
    }



    static class Finished extends TriggerConsumer<SSLSessionCallback>
    {
        static RateCounter rcFinished = new RateCounter("Finished");
        Finished() {
            super(FINISHED);
        }

        @Override
        public void accept(SSLSessionCallback callback) {
            long ts = System.currentTimeMillis();
            SSLSessionConfig config = (SSLSessionConfig)getStateMachine().getConfig();


            // ********************************************
            // Very crucial steps
            // ********************************************
            if(config.remoteAddress != null)
            {
                // we have a SSL tunnel
                //publishSync(POST_HANDSHAKE, config);
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
                publishSync(config.getHandshakeStatus(), callback);
            }

            ts = System.currentTimeMillis() - ts;
            rcFinished.register(ts);
        }
    }











    public SSLHandshakingState() {
        super(SSLStateMachine.SessionState.HANDSHAKING);
        counter.incrementAndGet();
        register(new NeedTask())
                .register(new NeedWrap())
                .register(new NeedUnwrap())
                .register(new Finished())
        ;

    }

}
