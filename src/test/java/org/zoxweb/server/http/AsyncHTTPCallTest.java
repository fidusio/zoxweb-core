package org.zoxweb.server.http;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.RateController;

import java.util.concurrent.atomic.AtomicLong;

public class AsyncHTTPCallTest
{

    private static final LogWrapper log = new LogWrapper(AsyncHTTPCallTest.class);
    static final AtomicLong error = new AtomicLong();
    static final AtomicLong success = new AtomicLong();
    private static final HTTPCallBack<Void, NVGenericMap> callback = new HTTPCallBack<Void, NVGenericMap>() {
        @Override
        public void exception(Exception e)
        {
            e.printStackTrace();
            log.getLogger().info("Error: " + error.incrementAndGet() + e);
        }

        @Override
        public void accept(HTTPAPIResult<NVGenericMap> nvGenericMap)
        {
            log.getLogger().info("*[" + success.incrementAndGet() + "] NVGenericMap Success " + nvGenericMap.getData());
        }
    };






    public static void main(String ...args)
    {
        try
        {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            String url = params.stringValue("url");
            String uri = params.stringValue("uri", null);
            String rate = params.stringValue("rate");
            int repeat = params.intValue("repeat");
            RateController rc = new RateController("test", rate);
            LoggerUtil.enableDefaultLogger("org.zoxweb");
            HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, uri, HTTPMethod.GET, false);
            BiDataEncoder<HTTPMessageConfigInterface, Void, HTTPMessageConfigInterface> dataEncoder = new BiDataEncoder<HTTPMessageConfigInterface, Void, HTTPMessageConfigInterface>() {
                @Override
                public HTTPMessageConfigInterface encode(HTTPMessageConfigInterface hmci, Void unused) {

                    if("ping".equalsIgnoreCase(hmci.getURI()))
                        hmci.setURI("ping/detailed");

                    return hmci;
                }
            };


            HTTPAPIEndPoint<Void, NVGenericMap> testEndPoint = new HTTPAPIEndPoint<Void, NVGenericMap>(hmci)
                    .setDataDecoder(HTTPUtil.NVGM_DECODER)
                    .setDataEncoder(dataEncoder)
                    .setExecutor(TaskUtil.getDefaultTaskProcessor())
                    .setRateController(rc)
                    .setScheduler(TaskUtil.getDefaultTaskScheduler());





            ///log.getLogger().info("**************************************************************************************");
            for(int i =0; i < repeat; i++)
            {
                testEndPoint.asyncCall(callback);
            }


            for(int i =0; i < repeat; i++)
            {
                HTTPAPIResult<NVGenericMap> result = testEndPoint.syncCall(null);
                System.out.println(result);
            }



            TaskUtil.waitIfBusyThenClose(100);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
