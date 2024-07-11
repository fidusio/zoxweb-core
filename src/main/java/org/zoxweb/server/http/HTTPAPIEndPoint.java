package org.zoxweb.server.http;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPAPIEndPoint<I,O>
    implements CanonicalID, GetNVProperties

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
                HTTPResponseData hrd = HTTPCall.send(createHMCI(callback.get(), authorization));
                HTTPAPIResult<?> hapir = new HTTPAPIResult<>(hrd.getStatus(),
                        hrd.getHeaders(),
                        dataDecoder != null ? dataDecoder.decode(hrd) : hrd.getData(),
                        hrd.getDuration());
                callback.accept((HTTPAPIResult<O>) hapir);
                successCounter.incrementAndGet();
            } catch (Exception e) {
                failedCounter.incrementAndGet();
                if (callback != null)
                    callback.exception(e);
            }
        }
    }

    private HTTPMessageConfigInterface config;
    private RateController rateController;
    private BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface> dataEncoder;
    private DataDecoder<HTTPResponseData, O> dataDecoder;
    private transient Executor executor;
    private transient TaskSchedulerProcessor tsp;
    private final NamedDescription namedDescription = new NamedDescription();
    private String domain;

    private final Map<Integer, HTTPStatusCode> positiveResults = new HashMap<>();

    private final AtomicLong successCounter = new AtomicLong();
    private final AtomicLong failedCounter = new AtomicLong();
    private NVGenericMap properties = new NVGenericMap();



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

    public HTTPAPIEndPoint<I,O> setDataEncoder(BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface> dataEncoder)
    {
        this.dataEncoder = dataEncoder;
        return this;
    }

    public String getDomain()
    {
        return domain;
    }

    public HTTPAPIEndPoint<I,O> setDomain(String domain)
    {
        domain = SharedStringUtil.trimOrNull(domain);
        if (domain != null)
        {
            if (domain.endsWith("."))
                domain = domain.substring(0, domain.length() -1);
        }
        this.domain = domain;
        return this;
    }

    public String toCanonicalID()
    {
        return SharedUtil.toCanonicalID(true, '.', domain, getName());
    }


    public HTTPAPIEndPoint<I,O> setName(String name)
    {
        namedDescription.setName(name);
        return this;
    }

    public HTTPAPIEndPoint<I,O> setDescription(String desciption)
    {
        namedDescription.setDescription(desciption);
        return this;
    }

    public String getName()
    {
        return namedDescription.getName();
    }

    public String getDescription()
    {
        return namedDescription.getDescription();
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




    public HTTPAPIResult<O> syncCall(I input)
            throws IOException
    {
        return syncCall(input, null);
    }


    public HTTPAPIResult<O> syncCall(I input, HTTPAuthorization authorization) throws IOException
    {
        HTTPResponseData hrd = HTTPCall.send(createHMCI(input, authorization));
        HTTPAPIResult<O> hapir = null;
        if(dataDecoder != null)
            hapir = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), dataDecoder.decode(hrd), hrd.getDuration());
        else
            hapir = (HTTPAPIResult<O>) new HTTPAPIResult<byte[]>(hrd.getStatus(), hrd.getHeaders(), hrd.getData(), hrd.getDuration());

        successCounter.incrementAndGet();
        return hapir;
    }



    public HTTPAPIEndPoint<I,O> syncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization)
    {

        ToRun toRun = new ToRun(callback, authorization);
        toRun.run();
        return this;

    }



    protected HTTPMessageConfigInterface createHMCI(I input, HTTPAuthorization authorization)
    {

        HTTPMessageConfigInterface ret = HTTPMessageConfig.createAndInit(config.getURL(), config.getURI(), config.getMethod(), config.isSecureCheckEnabled());
        NVGenericMap.copy(config.getHeaders(), ret.getHeaders(), true);
        NVGenericMap.copy(config.getParameters(), ret.getParameters(), true);
        ret.setProxyAddress(config.getProxyAddress());
        ret.setRedirectEnabled(config.isRedirectEnabled());
        ret.setHTTPErrorAsException(config.isHTTPErrorAsException());
        ret.setCharset(config.getCharset());

        if (authorization != null)
            ret.setAuthorization(authorization);
        else
            ret.setAuthorization(config.getAuthorization());

        if (dataEncoder != null)
            ret = dataEncoder.encode(ret, input);


        return ret;

    }


    public HTTPAPIEndPoint<I,O> asyncCall(HTTPCallBack<I,O> callback)
    {
        return asyncCall(callback, null, rateController != null ? rateController.nextWait() : 0);
    }

    public HTTPAPIEndPoint<I,O>  asyncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization)
    {
        return asyncCall(callback, authorization, rateController != null ? rateController.nextWait() : 0);
    }


    public HTTPAPIEndPoint<I,O> asyncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization, long delayInMillis)
    {

        ToRun toRun = new ToRun(callback, authorization);

        if(tsp != null)
        {
            tsp.queue(delayInMillis, toRun);
        }
        else if(executor != null)
            executor.execute(toRun);
        else
            throw new IllegalArgumentException("No executor or scheduler found can't execute");

        return this;

    }


    public long successCount()
    {
        return successCounter.get();
    }

    public long failedCount()
    {
        return failedCounter.get();
    }



    public HTTPAPIEndPoint<I,O> setPositiveResults(int ...httpStatusCodes) {
        if (httpStatusCodes != null)
        {
            for (int statusCode : httpStatusCodes)
            {
                HTTPStatusCode hsc = HTTPStatusCode.statusByCode(statusCode);
                if (hsc != null)
                    positiveResults.put(statusCode, hsc);
            }
        }
        return this;
    }

    public synchronized HTTPAPIEndPoint<I,O>  setPositiveResults(List<Integer> httpStatusCodes) {
        if (httpStatusCodes != null)
        {
            positiveResults.clear();
            for (int statusCode : httpStatusCodes)
            {
                HTTPStatusCode hsc = HTTPStatusCode.statusByCode(statusCode);
                if (hsc != null)
                    positiveResults.put(statusCode, hsc);
            }
        }
        return this;
    }

    public HTTPAPIEndPoint<I,O> setPositiveResults(HTTPStatusCode ...httpStatusCodes)
    {
        if(httpStatusCodes != null)
        {
            for (HTTPStatusCode statusCode : httpStatusCodes)
            {
                positiveResults.put(statusCode.CODE, statusCode);
            }
        }
        return this;
    }

    public NVGenericMap getProperties()
    {
        return properties;
    }

    public HTTPAPIEndPoint<I,O> setProperties(NVGenericMap properties)
    {
        this.properties = properties;
        if(properties != null)
        {
            setPositiveResults((List<Integer>) properties.getValue("positive_result_codes"));
        }

        return this;
    }

    public HTTPStatusCode lookupPositiveResult(int statusCode)
    {
        return positiveResults.get(statusCode);
    }
}
