package org.zoxweb.server.http;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.RateController;

import java.util.concurrent.atomic.AtomicLong;

public class AsyncHTTPCallTest
{

    private static final LogWrapper log = new LogWrapper(AsyncHTTPCallTest.class);
    static final AtomicLong error = new AtomicLong();
    static final AtomicLong success = new AtomicLong();
    private static final ConsumerCallback<NVGenericMap> callback = new ConsumerCallback<NVGenericMap>()
    {

        @Override
        public void exception(Exception e)
        {
            log.getLogger().info("Error: " + error.incrementAndGet() + e);
        }

        @Override
        public void accept(NVGenericMap nvGenericMap)
        {
            log.getLogger().info("*[" + success.incrementAndGet() + "] NVGenericMap Success " + nvGenericMap);
        }
    };


    private static final ConsumerCallback<HTTPResponseData> callbackOnly = new ConsumerCallback<HTTPResponseData>()
    {
        @Override
        public void exception(Exception e)
        {
            log.getLogger().info("Error: " + error.incrementAndGet() + e);
        }

        @Override
        public void accept(HTTPResponseData hrd)
        {
            log.getLogger().info("**[" + success.incrementAndGet() + "] HTTPResponseData Success " + hrd.getDataAsString());
        }
    };

    public static void main(String ...args)
    {
        try
        {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            String url = params.stringValue("url");
            String rate = params.stringValue("rate");
            int repeat = params.intValue("repeat");
            RateController rc = new RateController("test", rate);
            LoggerUtil.enableDefaultLogger("org.zoxweb");
            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, HTTPMethod.GET, false);
            AsyncHTTPCall<NVGenericMap> asyncHC = new AsyncHTTPCall<>(callback, GSONUtil.NVGenericMapDecoder);
            for(int i =0; i < repeat; i++)
            {
                asyncHC.asyncSend(hmci, rc);
            }
            AsyncHTTPCall<HTTPResponseData> asyncHRD = new AsyncHTTPCall<>(callbackOnly);
            ///log.getLogger().info("**************************************************************************************");
            for(int i =0; i < repeat; i++)
            {
                asyncHRD.asyncSend(hmci, rc);
            }

            TaskUtil.waitIfBusyThenClose(100);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
