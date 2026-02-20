package org.zoxweb.server.http;

import okhttp3.OkHttpClient;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPAPIEndPoint<I, O>
        implements CanonicalID, GetNVProperties {
    private volatile HTTPMessageConfigInterface config;
    private volatile RateController rateController;
    private volatile BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface> dataEncoder;
    private volatile DataDecoder<HTTPResponseData, O> dataDecoder;
    private transient Executor executor;
    private transient ScheduledExecutorService tsp;
    private final NamedDescription namedDescription = new NamedDescription();
    private volatile String domain;
    private volatile OkHttpClient okHttpClient = null;
    private final Map<Integer, HTTPStatusCode> positiveResults;
    private final AtomicLong successCounter = new AtomicLong();
    private final AtomicLong failedCounter = new AtomicLong();
    private volatile NVGenericMap properties = new NVGenericMap();
    private final boolean deepCopy = true;


    private class ToRun
            implements Runnable {
        private final HTTPCallback<I, O> callback;
        private final HTTPAuthorization authorization;

        ToRun(HTTPCallback<I, O> callback, HTTPAuthorization authorization) {
            this.callback = callback;
            this.authorization = authorization;
        }

        public void run() {
            try {
                HTTPResponseData hrd = OkHTTPCall.send(getOkHttpClient(), createHMCI(callback.get(), authorization));
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


    public HTTPAPIEndPoint<I, O> copy(boolean newRateController) {
        HTTPAPIEndPoint<I, O> ret = new HTTPAPIEndPoint<>(config, positiveResults);
        ret.setName(getName())
                .setDescription(getDescription())
                .setDataEncoder(dataEncoder)
                .setDataDecoder(dataDecoder);
        ret.executor = executor;
        ret.tsp = tsp;
        ret.okHttpClient = okHttpClient;
        ret.properties = NVGenericMap.copy(properties, deepCopy);
        ret.domain = domain;
        ret.rateController = rateController;

        if (rateController != null && newRateController) {
            ret.setRateController(new RateController(rateController.getName(), rateController.getRate(), rateController.getRateUnit()));
        }

        return ret;
    }

    public HTTPAPIEndPoint(HTTPMessageConfigInterface config, Map<Integer, HTTPStatusCode> positiveResults) {
        this.config = config;
        if (positiveResults != null)
            this.positiveResults = positiveResults;
        else
            this.positiveResults = new HashMap<>();
    }

    public HTTPAPIEndPoint(HTTPMessageConfigInterface config) {
        this(config, null);
    }


    public HTTPAPIEndPoint<I, O> setConfig(HTTPMessageConfigInterface hmci) {
        this.config = hmci;
        return this;
    }

    public HTTPMessageConfigInterface getConfig() {
        return config;
    }

    public HTTPAPIEndPoint<I, O> setRateController(RateController rateController) {
        this.rateController = rateController;
        return this;
    }

    public HTTPAPIEndPoint<I, O> setDataDecoder(DataDecoder<HTTPResponseData, O> dataDecoder) {
        this.dataDecoder = dataDecoder;
        return this;
    }

    public HTTPAPIEndPoint<I, O> setDataEncoder(BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface> dataEncoder) {
        this.dataEncoder = dataEncoder;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public HTTPAPIEndPoint<I, O> setDomain(String domain) {
        domain = SharedStringUtil.trimOrNull(domain);
        if (domain != null) {
            if (domain.endsWith("."))
                domain = domain.substring(0, domain.length() - 1);
        }
        this.domain = domain;
        return this;
    }

    public String toCanonicalID() {
        return SharedUtil.toCanonicalID(true, '.', domain, getName());
    }


    public HTTPAPIEndPoint<I, O> setName(String name) {
        namedDescription.setName(name);
        return this;
    }

    public HTTPAPIEndPoint<I, O> setDescription(String desciption) {
        namedDescription.setDescription(desciption);
        return this;
    }

    public String getName() {
        return namedDescription.getName();
    }

    public String getDescription() {
        return namedDescription.getDescription();
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public HTTPAPIEndPoint<I, O> setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        return this;
    }


    public HTTPAPIEndPoint<I, O> setExecutor(Executor exec) {
        this.executor = exec;
        return this;
    }

    public HTTPAPIEndPoint<I, O> setScheduler(TaskSchedulerProcessor scheduler) {
        this.tsp = scheduler;
        return this;
    }

    public ScheduledExecutorService getScheduler() {
        return tsp;
    }


    public HTTPAPIResult<O> syncCall(I input)
            throws IOException {
        return syncCall(input, null);
    }


    public HTTPAPIResult<O> syncCall(I input, HTTPAuthorization authorization) throws IOException {
        checkRateController();
        HTTPResponseData hrd = OkHTTPCall.send(getOkHttpClient(), createHMCI(input, authorization));
        HTTPAPIResult<O> hapir = null;
        if (dataDecoder != null)
            hapir = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), dataDecoder.decode(hrd), hrd.getDuration());
        else
            hapir = (HTTPAPIResult<O>) new HTTPAPIResult<byte[]>(hrd.getStatus(), hrd.getHeaders(), hrd.getData(), hrd.getDuration());

        successCounter.incrementAndGet();
        return hapir;
    }


    private void checkRateController()
            throws IOException {
        if (rateController == null)
            return;
        // first check without affecting rate controller
        boolean send = rateController.isPastThreshold(false);

        // now try to use the rate controller
        if (send || rateController.isPastThreshold(true))
            throw new IOException("Rate controller timeout threshold reached: " + rateController);
    }

    public HTTPAPIEndPoint<I, O> syncCall(HTTPCallback<I, O> callback, HTTPAuthorization authorization)
            throws IOException {
        checkRateController();
        ToRun toRun = new ToRun(callback, authorization);
        toRun.run();
        return this;

    }


    private HTTPMessageConfigInterface createHMCI(I input, HTTPAuthorization authorization) {

        HTTPMessageConfigInterface ret = HTTPMessageConfig.createAndInit(config.getURL(), config.getURI(), config.getMethod(), config.isSecureCheckEnabled());
        NVGenericMap.copy(config.getHeaders(), ret.getHeaders(), deepCopy);
        NVGenericMap.copy(config.getParameters(), ret.getParameters(), deepCopy);
        ret.setProxyAddress(config.getProxyAddress());
        ret.setRedirectEnabled(config.isRedirectEnabled());
        ret.setHTTPErrorAsException(config.isHTTPErrorAsException());
        ret.setCharset(config.getCharset());
        ret.setTimeout(config.getTimeout());
        ret.setProxyAddress(config.getProxyAddress());

        if (authorization != null)
            ret.setAuthorization(authorization);
        else
            ret.setAuthorization(config.getAuthorization());

        if (dataEncoder != null)
            ret = dataEncoder.encode(ret, input);


        return ret;

    }


    public HTTPAPIEndPoint<I, O> asyncCall(HTTPCallback<I, O> callback) {
        return asyncCall(callback, null, rateController != null ? rateController.nextWait() : 0);
    }

    public HTTPAPIEndPoint<I, O> asyncCall(HTTPCallback<I, O> callback, HTTPAuthorization authorization) {
        return asyncCall(callback, authorization, rateController != null ? rateController.nextWait() : 0);
    }


    public HTTPAPIEndPoint<I, O> asyncCall(HTTPCallback<I, O> callback, HTTPAuthorization authorization, long delayInMillis) {
        ToRun toRun = new ToRun(callback, authorization);

        if (tsp != null)
            tsp.schedule(toRun, delayInMillis, TimeUnit.MILLISECONDS);
        else if (executor != null)
            executor.execute(toRun);
        else
            throw new IllegalArgumentException("No executor or scheduler found can't execute");

        return this;
    }


    public long successCount() {
        return successCounter.get();
    }

    public long failedCount() {
        return failedCounter.get();
    }


    public HTTPAPIEndPoint<I, O> setPositiveResults(int... httpStatusCodes) {
        if (httpStatusCodes != null) {
            for (int statusCode : httpStatusCodes) {
                HTTPStatusCode hsc = HTTPStatusCode.statusByCode(statusCode);
                if (hsc != null)
                    positiveResults.put(statusCode, hsc);
            }
        }
        return this;
    }

    public synchronized HTTPAPIEndPoint<I, O> setPositiveResults(List<Integer> httpStatusCodes) {
        if (httpStatusCodes != null) {
            positiveResults.clear();
            for (int statusCode : httpStatusCodes) {
                HTTPStatusCode hsc = HTTPStatusCode.statusByCode(statusCode);
                if (hsc != null)
                    positiveResults.put(statusCode, hsc);
            }
        }
        return this;
    }

    public HTTPAPIEndPoint<I, O> setPositiveResults(HTTPStatusCode... httpStatusCodes) {
        if (httpStatusCodes != null) {
            for (HTTPStatusCode statusCode : httpStatusCodes) {
                positiveResults.put(statusCode.CODE, statusCode);
            }
        }
        return this;
    }

    public NVGenericMap getProperties() {
        return properties;
    }

    public HTTPAPIEndPoint<I, O> setProperties(NVGenericMap properties) {
        this.properties = properties;
        if (properties != null) {
            setPositiveResults((List<Integer>) properties.getValue("positive_result_codes"));
        }

        return this;
    }

    public HTTPStatusCode lookupPositiveResult(int statusCode) {
        return positiveResults.get(statusCode);
    }
}
