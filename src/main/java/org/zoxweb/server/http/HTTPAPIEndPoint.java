package org.zoxweb.server.http;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.RateController;

import java.io.IOException;
import java.util.concurrent.Executor;

public class HTTPAPIEndPoint<I,O>
{
    private class ToRun
            implements Runnable
    {
        private final HTTPCallBack<I,O> callback;
        private final HTTPAuthorization authorization;
        ToRun(HTTPCallBack<I,O> callback, HTTPAuthorization authorization)
        {
            this.callback = callback;
            this.authorization = authorization;
        }

        public void run()
        {
            try
            {
                HTTPResponseData hrd = HTTPCall.send(merge(dataEncoder != null ? dataEncoder.encode(callback.get()) : null, authorization));
                if (dataDecoder != null) {
                    HTTPAPIResult<O> hapir = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), dataDecoder.decode(hrd));
                    callback.accept(hapir);
                }
                else
                {
                    HTTPAPIResult<byte[]> hapir = new HTTPAPIResult<byte[]>(hrd.getStatus(), hrd.getHeaders(), hrd.getData());
                    callback.accept((HTTPAPIResult<O>) hapir);
                }
            } catch (Exception e) {
                if (callback != null)
                    callback.exception(e);
            }
        }
    }

    private HTTPMessageConfigInterface config;
    private RateController rateController;
    private DataEncoder<I, HTTPMessageConfigInterface> dataEncoder;
    private DataDecoder<HTTPResponseData, O> dataDecoder;
    private Executor executor;
    private TaskSchedulerProcessor tsp;



    public HTTPAPIEndPoint(HTTPMessageConfigInterface config)
    {
        this.config = config;
    }



    public HTTPAPIEndPoint<I,O> setConfig(HTTPMessageConfigInterface hmci)
    {
        this.config = hmci;
        return this;
    }

    public HTTPAPIEndPoint<I,O> setRateController(RateController rateController)
    {
        this.rateController = rateController;
        return this;
    }

    public HTTPAPIEndPoint<I,O> setDataDecoder(DataDecoder<HTTPResponseData, O> dataDecoder)
    {
        this.dataDecoder = dataDecoder;
        return this;
    }

    public HTTPAPIEndPoint<I,O> setDataEncoder(DataEncoder<I, HTTPMessageConfigInterface>  dataEncoder)
    {
        this.dataEncoder = dataEncoder;
        return this;
    }



    public HTTPAPIEndPoint<I,O> setExecutor(Executor exec)
    {
        this.executor = exec;
        return this;
    }

    public HTTPAPIEndPoint<I,O> setScheduler(TaskSchedulerProcessor scheduler)
    {
        this.tsp = scheduler;
        return this;
    }




    public O syncCall(I input)
            throws IOException
    {
        return syncCall(input, null);
    }


    public O syncCall(I input, HTTPAuthorization authorization) throws IOException
    {
        HTTPResponseData hrd = HTTPCall.send(merge(dataEncoder != null ? dataEncoder.encode(input) : null, authorization));
        if(dataDecoder != null)
            return dataDecoder.decode(hrd);
        else
            return (O) hrd.getData();
    }


    private HTTPMessageConfigInterface merge(HTTPMessageConfigInterface hmci, HTTPAuthorization authorization)
    {

        if(hmci == null)
            return config;

        if (config != null) {
            hmci.setURL(config.getURL());
            hmci.setURI(config.getURI());
            hmci.setSecureCheckEnabled(config.isSecureCheckEnabled());
            hmci.setMethod(config.getMethod());
            NVGenericMap.merge(config.getHeaders(), hmci.getHeaders());
            NVGenericMap.merge(config.getParameters(), hmci.getParameters());
        }

        if (authorization != null)
            hmci.setAuthorization(authorization);

        return hmci;
    }


    public void asyncCall(HTTPCallBack<I,O> callback)
    {
        asyncCall(callback, null, rateController != null ? rateController.nextDelay() : 0);
    }

    public void asyncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization)
    {
        asyncCall(callback, authorization, rateController != null ? rateController.nextDelay() : 0);
    }


    public void asyncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization, long delayInMillis)
    {

        ToRun toRun = new ToRun(callback, authorization);

        if(tsp != null)
            tsp.queue(delayInMillis, toRun);
        else if(executor != null)
            executor.execute(toRun);
        else
            throw new IllegalArgumentException("No executor or scheduler found can't execute");

    }










}
