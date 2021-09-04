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
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPMimeType;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.net.InetAddressDAO;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.ParamUtil;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class HTTPCallTool implements Runnable
{
    private static Logger log = Logger.getLogger(HTTPCallTool.class.getName());
    private static AtomicLong counter = new AtomicLong();
    private static AtomicLong failCounter = new AtomicLong();

    private HTTPMessageConfigInterface hmci;
    private boolean printResult;

    public HTTPCallTool(HTTPMessageConfigInterface hmci, boolean printResult)
    {
        this.hmci = hmci;
        this.printResult = printResult;
    }

    public void run()
    {
        try
        {
            HTTPCall hc = new HTTPCall(hmci);
            HTTPResponseData rd = hc.sendRequest();
            if(printResult)
                log.info("" + rd);

        }
        catch(Exception e)
        {
            e.printStackTrace();
            failCounter.incrementAndGet();
        }
        counter.incrementAndGet();
    }

    public static void main(String ...args)
    {
        try
        {
            TaskUtil.setMaxTasksQueue(2048);
            TaskUtil.setThreadMultiplier(8);

            ParamUtil.ParamMap params = ParamUtil.parse("-", args);
            //int index = 0;
            int repeat = params.intValue("-r", 1);
            List<String> urls = params.lookup("-url");
            //String uri = params.stringValue("-uri", true);
            HTTPMethod httpMethod = HTTPMethod.lookup(params.stringValue("-m", "GET"));
            String contentFilename = params.stringValue("-c", true);
            String content = contentFilename != null ? IOUtil.inputStreamToString(contentFilename) : null;
            boolean printResult = params.booleanValue("-pr", true);
            String proxy = params.stringValue("-p", true);
            log.info("proxy: " + proxy);
            InetSocketAddressDAO proxyAddress = proxy != null ? InetSocketAddressDAO.parse(proxy, ProxyType.HTTP) : null;

            List<HTTPMessageConfigInterface> hmcis = new ArrayList<>();
            for(String url : urls) {
                HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, httpMethod);
                hmci.setProxyAddress(proxyAddress);

                hmci.setContentType(HTTPMimeType.APPLICATION_JSON);
                hmci.setSecureCheckEnabled(false);
                if (content != null)
                    hmci.setContent(content);
                System.out.println(GSONUtil.toJSON((HTTPMessageConfig) hmci, true, false, false));
                hmcis.add(hmci);
            }
            long ts = System.currentTimeMillis();
            int messages = 0;
            for(int i = 0; i < repeat; i++)
            {
                for(HTTPMessageConfigInterface hmci : hmcis) {
                    TaskUtil.getDefaultTaskScheduler().queue(0, new HTTPCallTool(hmci, printResult));
                }
            }



           

            ts = TaskUtil.waitIfBusyThenClose(25) - ts;

            
            float rate = ((float)counter.get()/(float)ts)*1000;

            log.info("It took:" + Const.TimeInMillis.toString(ts) + " to send:" + counter.get() + " failed:" + failCounter+ " rate:" + rate + " per/second");

        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.err.println("usage: -url url-value  <-r repeat-count> <-m http method default get> <-c content file name> <-pr true(print result)");
        }
    }

}
