package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.*;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class SSLStateMachine extends StateMachine<SSLSessionConfig>
    implements SSLConnectionHelper
{


    @Override
    public void dispatch(SSLEngineResult.HandshakeStatus status, SSLSessionCallback callback)
    {
        if (!isClosed())
            publishSync(new Trigger<SSLSessionCallback>(this, status, null, callback));
    }



    //private final static AtomicLong HANDSHAKE_COUNTER = new AtomicLong();
    public enum SessionState
    implements GetName
    {
        DATA_READY("data-ready"),
        HANDSHAKING("handshaking"),
        POST_HANDSHAKE("post-handshake"),


        /**
         * Read data state will unwrap data via it trigger in the read state
         * and in the handshaking state will unwrap data for the handshake process
         * it is identified by checking the SSLEngine NOT_HANDSHAKING status
         */

        CLOSE("close"),

        ;




        private final String name;
        SessionState(String name)
        {
            this.name = name;
        }
        @Override
        public String getName() {
            return name;
        }
    }


    static final AtomicLong counter = new AtomicLong();


    private volatile SSLNIOSocket sslNIOSocket = null;


//    private SSLStateMachine(long id, TaskSchedulerProcessor tsp) {
//        super("SSLSessionStateMachine-" + id, tsp);
//    }
    private SSLStateMachine(long id, Executor executor) {
        super("SSLSessionStateMachine-" + id, executor);
    }

    @Override
    public void createRemoteConnection() {
        sslNIOSocket.createRemoteConnection();
    }

//    public void close()
//    {
//        if (!isClosed.getAndSet(true))
//        {
//            SSLSessionConfig config = getConfig();
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
//            ByteBufferUtil.cache(config.inSSLNetData, config.inAppData, config.outSSLNetData, config.inRemoteData);
//            IOUtil.close(config.sslOutputStream);
//
//            if (log.isEnabled()) log.getLogger().info("SSLSessionConfig-CLOSED " +Thread.currentThread() + " " +
//                    config.sslChannel);// + " Address: " + connectionRemoteAddress);
////            TaskUtil.getDefaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS, ()->
////                log.getLogger().info(SSLStateMachine.rates()));
//        }
//    }




//    public static SSLStateMachine create(SSLContextInfo sslContext, Executor e)
//    {
//        SSLSessionConfig sslSessionConfig = new SSLSessionConfig(sslContext);
//        return create(sslSessionConfig, e);
//    }

//    public static SSLStateMachine create(SSLNIOSocket sslnioSocket)
//    {
//        SSLStateMachine ret = create(sslnioSocket.getSSLContextInfo(), null);
//        ret.sslNIOSocket = sslnioSocket;
//        return ret;
//    }




    public static SSLStateMachine create(SSLNIOSocket sslnioSocket){
        SSLStateMachine sslSessionSM = new SSLStateMachine(counter.incrementAndGet(), null);
        sslSessionSM.sslNIOSocket = sslnioSocket;


        SSLSessionConfig config = new SSLSessionConfig(sslnioSocket.getSSLContextInfo());
        sslSessionSM.setConfig(config);
        config.sslConnectionHelper = sslSessionSM;

    TriggerConsumerInt<Void> init = new TriggerConsumer<Void>(StateInt.States.INIT) {
          @Override
          public void accept(Void o) {
              if(log.isEnabled()) log.getLogger().info(getState().getStateMachine().getName() + " CREATED");
              //SSLSessionConfig config = (SSLSessionConfig) getStateMachine().getConfig();
              //publish(new Trigger<SelectableChannel>(getState(), null, SessionState.WAIT_FOR_HANDSHAKING));
          }
        };

//    TriggerConsumerInt<SSLSessionCallback> closed =
//        new TriggerConsumer<SSLSessionCallback>(SessionState.CLOSE) {
//          @Override
//          public void accept(SSLSessionCallback callback)
//          {
//            SSLSessionConfig config = (SSLSessionConfig)getStateMachine().getConfig();
//            config.close();
//            if (log.isEnabled()) log.getLogger().info(getStateMachine().getName() + " " + callback + " closed");
//          }
//        };

        sslSessionSM.setConfig(config)
                .register(new State(StateInt.States.INIT).register(init))
                .register(new SSLHandshakingState())
                .register(new SSLDataReadyState())
                //.register(new State(SessionState.CLOSE).register(closed))
        ;


        return sslSessionSM;
    }



    public static String rates()
    {
        return SharedUtil.toCanonicalID(',', SSLHandshakingState.NeedWrap.rcNeedWrap, SSLHandshakingState.NeedUnwrap.rcNeedUnwrap, SSLHandshakingState.NeedTask.rcNeedTask, SSLHandshakingState.Finished.rcFinished, SSLDataReadyState.NotHandshaking.rcNotHandshaking);
    }

    public static <T> T lookupType(String type)
    {
        type = SharedStringUtil.toUpperCase(type);
        switch(type)
        {
            case "NEED_WRAP":
                return (T) SSLHandshakingState.NeedWrap.rcNeedWrap;
            case "NEED_UNWRAP":
                return (T) SSLHandshakingState.NeedUnwrap.rcNeedUnwrap;
            case "NEED_TASK":
                return (T) SSLHandshakingState.NeedTask.rcNeedTask;
            case "FINISHED":
                return (T) SSLHandshakingState.Finished.rcFinished;
            case "NOT_HANDSHAKING":
                return (T) SSLDataReadyState.NotHandshaking.rcNotHandshaking;
            case "SSL_CONNECTION_COUNT":
                return (T) Long.valueOf(counter.get());


        }
        return null;
    }

}
