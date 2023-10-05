package org.zoxweb.server.http;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPAPIEndPoint<I,O>
    implements CanonicalID

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
                if (dataDecoder != null) {
                    HTTPAPIResult<O> hapir = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), dataDecoder.decode(hrd));
                    callback.accept(hapir);
                }
                else
                {
                    HTTPAPIResult<byte[]> hapir = new HTTPAPIResult<byte[]>(hrd.getStatus(), hrd.getHeaders(), hrd.getData());
                    callback.accept((HTTPAPIResult<O>) hapir);
                }
                successCounter.incrementAndGet();
            } catch (Exception e) {
                failedCoutner.incrementAndGet();
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
    private NamedDescription namedDescription = new NamedDescription();
    private String domain;


    private AtomicLong successCounter = new AtomicLong();
    private AtomicLong failedCoutner = new AtomicLong();



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
            hapir = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), dataDecoder.decode(hrd));
        else
            hapir = (HTTPAPIResult<O>) new HTTPAPIResult<byte[]>(hrd.getStatus(), hrd.getHeaders(), hrd.getData());

        successCounter.incrementAndGet();
        return hapir;
    }



    public void syncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization)
    {

        ToRun toRun = new ToRun(callback, authorization);
        toRun.run();

    }



    protected HTTPMessageConfigInterface createHMCI(I input, HTTPAuthorization authorization)
    {
        if(dataEncoder != null)
        {
            HTTPMessageConfigInterface ret = HTTPMessageConfig.createAndInit(config.getURL(), config.getURI(), config.getMethod(), config.isSecureCheckEnabled());
            NVGenericMap.copy(config.getHeaders(), ret.getHeaders(), true);
            NVGenericMap.copy(config.getParameters(), ret.getParameters(), true);
            ret.setProxyAddress(config.getProxyAddress());
            ret.setRedirectEnabled(config.isRedirectEnabled());
            ret.setHTTPErrorAsException(config.isHTTPErrorAsException());
            ret.setCharset(config.getCharset());

            ret = dataEncoder.encode(ret, input);
            if (authorization != null)
                ret.setAuthorization(authorization);
            else
                ret.setAuthorization(config.getAuthorization());

            return ret;
        }

        return config;
    }


    public void asyncCall(HTTPCallBack<I,O> callback)
    {
        asyncCall(callback, null, rateController != null ? rateController.nextWait() : 0);
    }

    public void asyncCall(HTTPCallBack<I,O> callback, HTTPAuthorization authorization)
    {
        asyncCall(callback, authorization, rateController != null ? rateController.nextWait() : 0);
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


    public long successCount()
    {
        return successCounter.get();
    }

    public long failCount()
    {
        return failedCoutner.get();
    }
}
