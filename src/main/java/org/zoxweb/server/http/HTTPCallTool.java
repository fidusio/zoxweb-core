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
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;

import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.RateCounter;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class HTTPCallTool implements Runnable
{
    private static LogWrapper log = new LogWrapper(HTTPCallTool.class);
    //private static AtomicLong counter = new AtomicLong();
    private static AtomicLong failCounter = new AtomicLong();
    //private static RateCounter callsCounter = new RateCounter("CallToolsCounter");

    private final HTTPMessageConfigInterface hmci;
    private boolean printResult;

    public HTTPCallTool(HTTPMessageConfigInterface hmci, boolean printResult)
    {
        this.hmci = hmci;
        this.printResult = printResult;
    }

    public void run()
    {
        HTTPResponseData rd = null;
        try
        {
            rd = HTTPCall.send(hmci);

        }
        catch(Exception e)
        {
            e.printStackTrace();
            if(e instanceof HTTPCallException)
            {
                rd = ((HTTPCallException) e).getResponseData();
            }

        }
        if(rd.getStatus() != HTTPStatusCode.OK.CODE)
        {
            failCounter.incrementAndGet();
        }
        //counter.incrementAndGet();

        if(printResult) {
            log.info("Total: " + HTTPCall.HTTP_CALLS.getCounts() + " Fail: " + failCounter + " status: " + rd.getStatus() + " length: " + rd.getData().length);
            log.info(rd.getDataAsString());
        }
    }

    public static void main(String ...args)
    {
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
            boolean printResult = params.nameExists("-pr");
            String proxy = params.stringValue("-p", true);
            int cap = params.intValue("-cap", 0);
            String user = params.stringValue("-user", true);
            String password = params.stringValue("-password", true);
            boolean errorAsException = params.nameExists("-eae");
            System.out.println("ErrorAsException: " + errorAsException);

            log.info("proxy: " + proxy);
            InetSocketAddressDAO proxyAddress = proxy != null ? InetSocketAddressDAO.parse(proxy, ProxyType.HTTP) : null;

            List<HTTPMessageConfigInterface> hmcis = new ArrayList<>();
            for(String url : urls) {
                HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, httpMethod);
                hmci.setProxyAddress(proxyAddress);
                hmci.setUser(user);
                hmci.setPassword(password);
                hmci.setHTTPErrorAsException(errorAsException);

                hmci.setContentType(HTTPMimeType.APPLICATION_JSON);
                hmci.setSecureCheckEnabled(false);
                if (content != null)
                    hmci.setContent(content);
                log.info(GSONUtil.toJSON((HTTPMessageConfig) hmci, true, false, false));
                hmcis.add(hmci);
            }
            long ts = System.currentTimeMillis();
            int messages = 0;
            for(int i = 0; i < repeat; i++)
            {
//                while(TaskUtil.getDefaultTaskProcessor().availableExecutorThreads() < 10)
//                {
//                    TaskUtil.sleep(50);
//                    log.info("After sleep:" + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());
//                }
                for(HTTPMessageConfigInterface hmci : hmcis) {
                    TaskUtil.getDefaultTaskScheduler().queue(0, new HTTPCallTool(hmci, printResult));
//                    log.info("PendingTask: " +TaskUtil.getDefaultTaskScheduler().pendingTasks() );
                }

            }



           

            ts = TaskUtil.waitIfBusyThenClose(25) - ts;

            RateCounter rc = new RateCounter("OverAll");
            rc.register(ts, HTTPCall.HTTP_CALLS.getCounts());
            //float rate = ((float)counter.get()/(float)ts)*1000;

            log.info("It took:" + Const.TimeInMillis.toString(ts) + " to send:" + HTTPCall.HTTP_CALLS.getCounts() + " failed:" + failCounter+
                    " rate:" + rc.rate(Const.TimeInMillis.SECOND.MILLIS) + " per/second" + " average: " + HTTPCall.HTTP_CALLS.average() + " millis");
            //log.info(""+System.getProperties());

        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.err.println("usage: -url url-value  [-r repeat-count] [-m http method default get] [-c content file name] [-pr true(print result)]");
        }
    }

}
