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

//import okhttp3.OkHttpClient;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public final class HTTPCallTool {
    private static final LogWrapper log = new LogWrapper(HTTPCallTool.class);
    private static final AtomicLong failCounter = new AtomicLong();
    private static final AtomicLong successCounter = new AtomicLong();


    private static boolean printResult;


    private static final HTTPCallback<Void, byte[]> callback = new HTTPCallback<Void, byte[]>(null) {
        @Override
        public void exception(Throwable e) {
            failCounter.incrementAndGet();
            e.printStackTrace();
            if (e instanceof HTTPCallException) {
                HTTPResponseData rd = ((HTTPCallException) e).getResponseData();
            }
        }

        @Override
        public void accept(HTTPAPIResult<byte[]> hrd) {
            if (hrd.getStatus() != HTTPStatusCode.OK.CODE) {
                failCounter.incrementAndGet();
            } else
                successCounter.incrementAndGet();

            if (printResult) {
                log.getLogger().info("Total: " + HTTPCall.HTTP_CALLS.getCounts() + " Fail: " + failCounter + " status: " + hrd.getStatus());
                log.getLogger().info(SharedStringUtil.toString(hrd.getData()));
            }
        }
    };


    private static long totalCount() {
        return successCounter.get() + failCounter.get();
    }

    public static void main(String... args) {
        try {
            TaskUtil.setThreadMultiplier(11);
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
            boolean disableKeepAlive = params.nameExists("-noKA");
            String proxy = params.stringValue("-p", true);
            int cap = params.intValue("-cap", 0);
            String user = params.stringValue("-user", true);
            String password = params.stringValue("-password", true);
            boolean errorAsException = params.nameExists("-eae");
            boolean certCheckEnabled = params.nameExists("-certCheckEnabled");
            boolean singleThread = params.nameExists("-singleThread");
            log.getLogger().info("ErrorAsException: " + errorAsException);

            log.getLogger().info("proxy: " + proxy);
            IPAddress proxyAddress = proxy != null ? IPAddress.parse(proxy, ProxyType.HTTP) : null;

            List<HTTPAPIEndPoint<Void, byte[]>> endpoints = new ArrayList<>();
            HTTPMessageConfigInterface[] hmcis = new HTTPMessageConfigInterface[urls.size()];
            int index = 0;
            for (String url : urls) {
                HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, httpMethod);
                hmcis[index++] = hmci;
                hmci.setProxyAddress(proxyAddress);
                if (user != null && password != null)
                    hmci.setBasicAuthorization(user, password);
                hmci.setHTTPErrorAsException(errorAsException);


                hmci.setSecureCheckEnabled(certCheckEnabled);
                hmci.setHTTPParameterFormatter(null);

                hmci.setContentType(HTTPMediaType.APPLICATION_JSON, HTTPConst.CHARSET_UTF_8);
                hmci.setHeader(HTTPHeader.ACCEPT, HTTPMediaType.APPLICATION_JSON, HTTPConst.CHARSET_UTF_8);
                if (disableKeepAlive)
                    hmci.setHeader(HTTPHeader.CONNECTION, "Close");
                else
                    hmci.setHeader(HTTPHeader.CONNECTION, "Keep-Alive");

                if (content != null)
                    hmci.setContent(content);
                log.getLogger().info("HMCI: " + GSONUtil.toJSONDefault(hmci, true));
                HTTPAPIEndPoint<Void, byte[]> endpoint = new HTTPAPIEndPoint<Void, byte[]>(hmci)
                        .setExecutor(TaskUtil.defaultTaskProcessor());

                endpoints.add(endpoint);
                // vm warmup
                log.getLogger().info("Test Call: " + new OkHTTPCall(hmci).sendRequest());
            }


            log.getLogger().info("HTTP request counts: " + repeat + " per url");


            //OkHttpClient client = OkHTTPCall.createOkHttpBuilder(TaskUtil.defaultTaskProcessor(), null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND,false, 20, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND).build();
            OkHTTPCall[] httpCalls = new OkHTTPCall[hmcis.length];
            for (int i = 0; i < hmcis.length; i++) {
                httpCalls[i] = new OkHTTPCall(hmcis[i]);
            }
            ConsumerCallback<HTTPResponse> cc = new ConsumerCallback<HTTPResponse>() {
                @Override
                public void exception(Throwable e) {
                    failCounter.incrementAndGet();
                    e.printStackTrace();
                    if (e instanceof HTTPCallException) {
                        HTTPResponseData rd = ((HTTPCallException) e).getResponseData();
                    }

                }

                @Override
                public void accept(HTTPResponse hrd) {

//                    if(hrd.getStatus() != HTTPStatusCode.OK.CODE)
//                        failCounter.incrementAndGet();
//                    else
                    successCounter.incrementAndGet();


//                    if(printResult) {
//                        log.getLogger().info("Total: " + totalCount()+ " Fail: " + failCounter + " status: " + hrd.getStatus());
//                        log.getLogger().info(SharedStringUtil.toString(((HTTPResponseData)hrd).getData()));
//                    }

                }
            };


            RateCounter rc = new RateCounter("OverAll");
            rc.start();
            for (int i = 0; i < repeat; i++) {

                for (HTTPAPIEndPoint<Void, byte[]> endPoint : endpoints) {
//                    if(singleThread)
//                        endPoint.syncCall(callback);
//                    else
                    endPoint.asyncCall(callback);
                }
            }

            TaskUtil.waitIfBusy(25);

            rc.stop(totalCount());


            log.getLogger().info("ENDPOINT OkHTTPCall stat: " + Const.TimeInMillis.toString(rc.lastDeltaInMillis()) + " to send: " + rc.getCounts() + " failed: " + failCounter +
                    " rate: " + rc.rate(Const.TimeInMillis.SECOND.MILLIS) + " per/second" + " average call duration: " + rc.average() + " millis");

            rc.reset().start();
            successCounter.set(0);
            failCounter.set(0);

            for (int i = 0; i < repeat; i++) {
                for (HTTPMessageConfigInterface request : hmcis) {
                    if (singleThread) {
                        try {
                            //request.sendRequest();
                            OkHTTPCall.send(request);
                            successCounter.incrementAndGet();
                        } catch (Exception e) {
                            failCounter.incrementAndGet();
                        }
                    } else {
                        TaskUtil.defaultTaskProcessor().execute(() ->
                                {
                                    try {
                                        //request.sendRequest();
                                        OkHTTPCall.send(request);
                                        successCounter.incrementAndGet();
                                    } catch (Exception e) {
                                        failCounter.incrementAndGet();
                                    }
                                }
                        );
                    }
                }
                //httpCall.asyncRequest(cc);

            }
            TaskUtil.waitIfBusy(25);
            rc.stop(totalCount());


            log.getLogger().info("RAW OkHTTPCall stat: " + Const.TimeInMillis.toString(rc.lastDeltaInMillis()) + " to send: " + rc.getCounts() + " failed: " + failCounter +
                    " rate: " + rc.rate(Const.TimeInMillis.SECOND.MILLIS) + " per/second" + " average call duration: " + rc.average() + " millis");


//            log.getLogger().info(GSONUtil.toJSONDefault(TaskUtil.info(), true));
//
            // shutdown the default executor and scheduler
            TaskUtil.waitIfBusyThenClose(25);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("usage: -url url-value  [-r repeat-count] [-m http method default get] [-c content file name] [-pr true(print result)] [-noKA(disable keep alive)]");
            TaskUtil.waitIfBusyThenClose(25);
        }
    }

}
