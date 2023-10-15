/*
 * Copyright (c) 2012-2019 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.http;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public final class HTTPCallTool //implements Runnable
{
    private static final LogWrapper log = new LogWrapper(HTTPCallTool.class);
    private static final AtomicLong failCounter = new AtomicLong();


//    private final HTTPMessageConfigInterface hmci;
    private static boolean printResult;



    private static final HTTPCallBack<Void, byte[]> callback = new HTTPCallBack<Void, byte[]>(null) {
        @Override
        public void exception(Exception e)
        {
            failCounter.incrementAndGet();
            e.printStackTrace();
            if(e instanceof HTTPCallException)
            {
                HTTPResponseData rd = ((HTTPCallException) e).getResponseData();
            }
        }

        @Override
        public void accept(HTTPAPIResult<byte[]> hrd)
        {
            if(hrd.getStatus() != HTTPStatusCode.OK.CODE)
            {
                failCounter.incrementAndGet();
            }

            if(printResult) {
                log.getLogger().info("Total: " + HTTPCall.HTTP_CALLS.getCounts() + " Fail: " + failCounter + " status: " + hrd.getStatus());
                log.getLogger().info(SharedStringUtil.toString(hrd.getData()));
            }
        }
    };
//    public HTTPCallTool(HTTPMessageConfigInterface hmci)
//    {
//        this.hmci = hmci;
//    }




//    public void run()
//    {
//        HTTPResponseData rd = null;
//        try
//        {
//            rd = HTTPCall.send(hmci);
//
//        }
//        catch(Exception e)
//        {
//            e.printStackTrace();
//            if(e instanceof HTTPCallException)
//            {
//                rd = ((HTTPCallException) e).getResponseData();
//            }
//        }
//        if(rd.getStatus() != HTTPStatusCode.OK.CODE)
//        {
//            failCounter.incrementAndGet();
//        }
//
//
//        if(printResult) {
//            log.getLogger().info("Total: " + HTTPCall.HTTP_CALLS.getCounts() + " Fail: " + failCounter + " status: " + rd.getStatus() + " length: " + rd.getData().length);
//            log.getLogger().info(rd.getDataAsString());
//        }
//    }

    public static void main(String ...args)
    {
        LoggerUtil.enableDefaultLogger("org.zoxweb");
        try
        {
            TaskUtil.setThreadMultiplier(8);
            TaskUtil.setMaxTasksQueue(2048);
            //System.setProperty("http.maxConnections", "100");


            ParamUtil.ParamMap params = ParamUtil.parse("-", args);
            //int index = 0;
            int repeat = params.intValue("-r", 1);
            List<String> urls = params.lookup("-url");
            //String uri = params.stringValue("-uri", true);
            HTTPMethod httpMethod = HTTPMethod.lookup(params.stringValue("-m", "GET"));
            String contentFilename = params.stringValue("-c", true);
            String content = contentFilename != null ? IOUtil.inputStreamToString(contentFilename) : null;
            printResult = params.nameExists("-pr");
            String proxy = params.stringValue("-p", true);
            int cap = params.intValue("-cap", 0);
            String user = params.stringValue("-user", true);
            String password = params.stringValue("-password", true);
            boolean errorAsException = params.nameExists("-eae");
            boolean certCheckEnabled = params.nameExists("-certCheckEnabled");
            log.getLogger().info("ErrorAsException: " + errorAsException);

            log.getLogger().info("proxy: " + proxy);
            InetSocketAddressDAO proxyAddress = proxy != null ? InetSocketAddressDAO.parse(proxy, ProxyType.HTTP) : null;

            List<HTTPAPIEndPoint<Void, byte[]>> endpoints = new ArrayList<>();
            for(String url : urls) {
                HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, httpMethod);
                hmci.setProxyAddress(proxyAddress);
                if (user != null && password !=null)
                    hmci.setBasicAuthorization(user, password);
                hmci.setHTTPErrorAsException(errorAsException);



                hmci.setSecureCheckEnabled(certCheckEnabled);
                hmci.setURLEncodingEnabled(false);
                hmci.setHTTPParameterFormatter(null);

//                hmci.setContentType(HTTPMediaType.APPLICATION_JSON);
//                hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
//                hmci.setUserAgent("TiziOuzou", "zib hmar");

                hmci.setContentType(HTTPMediaType.APPLICATION_JSON, HTTPConst.CHARSET_UTF_8);
                hmci.setHeader(HTTPHeader.ACCEPT, HTTPMediaType.APPLICATION_JSON, HTTPConst.CHARSET_UTF_8);
                        //.setHeader(HTTPHeader.USER_AGENT, "TiziOuzou", "zib hmar");

                if (content != null)
                    hmci.setContent(content);
                log.getLogger().info(GSONUtil.toJSON((HTTPMessageConfig) hmci, true, false, false));
                HTTPAPIEndPoint<Void, byte[]> endpoint = new HTTPAPIEndPoint<Void, byte[]>(hmci)
                        .setExecutor(TaskUtil.getDefaultTaskProcessor());

                endpoints.add(endpoint);
                // vm warmup
                new HTTPCall(hmci).sendRequest();
            }


            log.getLogger().info("HTTP request counts: " + repeat + " per url");


            long ts = System.currentTimeMillis();

            for(int i = 0; i < repeat; i++)
            {
                for(HTTPAPIEndPoint<Void, byte[]>   endPoint : endpoints)
                {
                    endPoint.asyncCall(callback);
                    //TaskUtil.getDefaultTaskProcessor().execute(new HTTPCallTool(hmci));
                }
            }
            ts = TaskUtil.waitIfBusyThenClose(25) - ts;
            RateCounter rc = new RateCounter("OverAll");
            rc.register(ts, HTTPCall.HTTP_CALLS.getCounts());

            log.getLogger().info("It took: " + Const.TimeInMillis.toString(ts) + " to send: " + HTTPCall.HTTP_CALLS.getCounts() + " failed: " + failCounter+
                    " rate: " + rc.rate(Const.TimeInMillis.SECOND.MILLIS) + " per/second" + " average call duration: " + HTTPCall.HTTP_CALLS.average() + " millis");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.err.println("usage: -url url-value  [-r repeat-count] [-m http method default get] [-c content file name] [-pr true(print result)]");
            TaskUtil.waitIfBusyThenClose(25);
        }
    }

}
