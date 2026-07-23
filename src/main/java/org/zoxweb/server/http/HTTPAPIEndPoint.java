package org.zoxweb.server.http;

import okhttp3.OkHttpClient;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.util.GSONUtil;
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

/**
 * A typed, reusable client-side binding of a single remote HTTP endpoint.
 * <p>
 * An endpoint pairs a template {@link HTTPMessageConfigInterface} (url, uri, method, headers,
 * parameters, timeout, proxy, ...) with pluggable codecs:
 * <ul>
 *   <li>a {@link BiDataEncoder} that stamps the typed input {@code I} onto a per-call copy of the
 *       message config (body, parameters, ...);</li>
 *   <li>a {@link DataDecoder} that converts the raw {@link HTTPResponseData} into the typed output
 *       {@code O} (when absent, the raw response bytes are passed through);</li>
 *   <li>an authorization encoder that applies an {@link HTTPAuthorization} to the outgoing call,
 *       defaulting to {@link #DEFAULT_AUTHORIZATION_ENCODER}.</li>
 * </ul>
 * The template config is never mutated by a call: {@code createHMCI(...)} deep-copies it for every
 * invocation, so one endpoint instance can be shared by concurrent callers.
 * <p>
 * Calls can be issued synchronously ({@link #syncCall(Object)}) or asynchronously
 * ({@link #asyncCall(HTTPCallback)}) via the configured {@link Executor} or
 * {@link TaskSchedulerProcessor}; an optional {@link RateController} gates synchronous calls
 * (throwing when the rate is exceeded) and paces asynchronous ones by scheduling delays.
 * Success and failure counters track call outcomes.
 * <p>
 * The positive-results map ({@link #setPositiveResults(int...)}, {@link #lookupPositiveResult(int)})
 * lets callback code reinterpret specific http error statuses as successful outcomes; the endpoint
 * itself does not act on it.
 * <p>
 * Endpoints are typically created and registered via {@code HTTPAPIManager}, which keys them by
 * their canonical ID {@code domain.name} (see {@link #toCanonicalID()}).
 *
 * @param <I> the typed input handed to the data encoder to build the request
 * @param <O> the typed output produced by the data decoder from the response
 */
public class HTTPAPIEndPoint<I, O>
        implements CanonicalID, GetNVProperties {

    public static final LogWrapper log = new LogWrapper(HTTPAPIEndPoint.class).setEnabled(false);

    /**
     * The default authorization encoder: applies the {@link HTTPAuthorization} via
     * {@code HTTPMessageConfigInterface.setAuthorization(...)}. Every endpoint starts with it;
     * pass it to {@link #setAuthorizationEncoder} to restore the default after a custom encoder.
     */
    public static final BiDataEncoder<HTTPMessageConfigInterface, HTTPAuthorization, HTTPMessageConfigInterface> DEFAULT_AUTHORIZATION_ENCODER = (h, a) -> {
        h.setAuthorization(a);
        return h;
    };
    // final variables
    private final NamedDescription namedDescription = new NamedDescription();
    private final Map<Integer, HTTPStatusCode> positiveResults;
    private final AtomicLong successCounter = new AtomicLong();
    private final AtomicLong failedCounter = new AtomicLong();
    private final boolean deepCopy = true;


    // volatile variables
    private volatile HTTPMessageConfigInterface config;
    private volatile RateController rateController;
    private volatile BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface> dataEncoder;
    private volatile DataDecoder<HTTPResponseData, O> dataDecoder;
    private volatile Executor executor;
    private volatile ScheduledExecutorService tsp;
    private volatile String domain;
    private volatile OkHttpClient okHttpClient = null;
    private volatile NVGenericMap properties = new NVGenericMap();
    private volatile BiDataEncoder<HTTPMessageConfigInterface, HTTPAuthorization, HTTPMessageConfigInterface> authorizationEncoder = DEFAULT_AUTHORIZATION_ENCODER;


    /**
     * One call execution unit: builds the per-call message config from the callback's input,
     * sends it via {@link OkHTTPCall}, decodes the response and reports the {@link HTTPAPIResult}
     * (or any exception) back to the callback, updating the success/failure counters.
     * The constructor rejects a null callback, so every sync and async entry point fails fast
     * on the caller's thread before any dispatch.
     */
    private class ToRun
            implements Runnable {
        private final HTTPCallback<I, O> callback;
        private final HTTPAuthorization authorization;

        ToRun(HTTPCallback<I, O> callback, HTTPAuthorization authorization) {
            SUS.checkIfNull("callback is null", callback);

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
                callback.exception(e);
            }
        }
    }


    /**
     * Creates a copy of this endpoint sharing the same config, codecs, executor, scheduler and
     * http client; the properties map is deep-copied and the success/failure counters start at zero.
     *
     * @param newRateController if true and a rate controller is set, the copy gets its own fresh
     *                          {@link RateController} (same name, rate and unit) instead of sharing
     *                          this endpoint's instance
     * @return the new endpoint copy
     */
    public HTTPAPIEndPoint<I, O> copy(boolean newRateController) {
        HTTPAPIEndPoint<I, O> ret = new HTTPAPIEndPoint<>(config, positiveResults);
        ret.setName(getName())
                .setDescription(getDescription())
                .setDataEncoder(dataEncoder)
                .setDataDecoder(dataDecoder)
                .setAuthorizationEncoder(authorizationEncoder);
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

    /**
     * Creates an endpoint bound to the given message config template.
     *
     * @param config          the template message config copied for every call
     * @param positiveResults map of http status codes considered successful, keyed by numeric code;
     *                        used as-is (not copied), an empty map is created when null
     */
    public HTTPAPIEndPoint(HTTPMessageConfigInterface config, Map<Integer, HTTPStatusCode> positiveResults) {
        this.config = config;
        if (positiveResults != null)
            this.positiveResults = positiveResults;
        else
            this.positiveResults = new HashMap<>();
    }


    /**
     * Creates an endpoint bound to the given message config template with an empty positive-results map.
     *
     * @param config the template message config copied for every call
     */
    public HTTPAPIEndPoint(HTTPMessageConfigInterface config) {
        this(config, null);
    }


    /**
     * Replaces the template message config used to build every subsequent call.
     *
     * @param hmci the new template message config
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setConfig(HTTPMessageConfigInterface hmci) {
        this.config = hmci;
        return this;
    }

    /**
     * @return the template message config this endpoint is bound to
     */
    public HTTPMessageConfigInterface getConfig() {
        return config;
    }

    /**
     * Sets the rate controller gating this endpoint: synchronous calls throw {@link IOException}
     * when the rate is exceeded, asynchronous calls are scheduled with the controller's next-wait delay.
     *
     * @param rateController the rate controller, null to disable rate control
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setRateController(RateController rateController) {
        this.rateController = rateController;
        return this;
    }


    /**
     * Sets the decoder converting the raw {@link HTTPResponseData} into the typed output {@code O};
     * when null the raw response bytes are returned instead.
     *
     * @param dataDecoder the response decoder
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setDataDecoder(DataDecoder<HTTPResponseData, O> dataDecoder) {
        this.dataDecoder = dataDecoder;
        return this;
    }

    /**
     * Sets the encoder stamping the typed input {@code I} onto the per-call message config
     * (body, parameters, ...); when null the input is ignored.
     *
     * @param dataEncoder the request encoder
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setDataEncoder(BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface> dataEncoder) {
        this.dataEncoder = dataEncoder;
        return this;
    }

    /**
     * @return the logical domain grouping this endpoint (e.g. the remote api family), may be null
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the logical domain of this endpoint; the value is trimmed and a trailing '.' is removed,
     * blank input resolves to null.
     *
     * @param domain the domain name
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setDomain(String domain) {
        domain = SUS.trimOrNull(domain);
        if (domain != null) {
            if (domain.endsWith("."))
                domain = domain.substring(0, domain.length() - 1);
        }
        this.domain = domain;
        return this;
    }

    /**
     * @return the canonical identifier {@code domain.name} (lower-cased, '.'-separated) used as
     *         the registration key in {@code HTTPAPIManager}
     */
    public String toCanonicalID() {
        return SUS.toCanonicalID(true, '.', domain, getName());
    }


    /**
     * Sets the endpoint name (the second half of the canonical ID).
     *
     * @param name the endpoint name
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setName(String name) {
        namedDescription.setName(name);
        return this;
    }


    /**
     * Sets the encoder that applies an {@link HTTPAuthorization} to the per-call message config,
     * replacing {@link #DEFAULT_AUTHORIZATION_ENCODER}; useful for apis that carry credentials in
     * custom headers or parameters. A null argument is ignored.
     *
     * @param authorizationEncoder the authorization encoder
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setAuthorizationEncoder(BiDataEncoder<HTTPMessageConfigInterface, HTTPAuthorization, HTTPMessageConfigInterface> authorizationEncoder) {
        if (authorizationEncoder != null) {
            this.authorizationEncoder = authorizationEncoder;
            if (log.isEnabled()) log.getLogger().info("*****setAuthorizationEncoder: " + authorizationEncoder);
        }
        return this;
    }


    /**
     * @return the current authorization encoder, never null
     */
    public BiDataEncoder<HTTPMessageConfigInterface, HTTPAuthorization, HTTPMessageConfigInterface> getAuthorizationEncoder() {
        return authorizationEncoder;
    }

    /**
     * Sets the human-readable description of this endpoint.
     *
     * @param description the description text
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setDescription(String description) {
        namedDescription.setDescription(description);
        return this;
    }

    /**
     * @return the endpoint name
     */
    public String getName() {
        return namedDescription.getName();
    }

    /**
     * @return the endpoint description
     */
    public String getDescription() {
        return namedDescription.getDescription();
    }

    /**
     * @return the dedicated {@link OkHttpClient} for this endpoint, or null when {@link OkHTTPCall}
     *         picks a client per call (its shared default, or a purpose-built one when the message
     *         config sets a proxy or disables redirects)
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * Sets a dedicated {@link OkHttpClient} for this endpoint; null falls back to the shared
     * {@link OkHTTPCall} default client.
     *
     * @param okHttpClient the http client to use
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        return this;
    }


    /**
     * Sets the executor running asynchronous calls; used only when no scheduler is set.
     *
     * @param exec the executor
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setExecutor(Executor exec) {
        this.executor = exec;
        return this;
    }

    /**
     * Sets the scheduler running asynchronous calls; takes precedence over the executor and
     * enables rate-controlled delayed dispatch.
     *
     * @param scheduler the task scheduler
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setScheduler(TaskSchedulerProcessor scheduler) {
        this.tsp = scheduler;
        return this;
    }

    /**
     * @return the scheduler used for asynchronous calls, may be null
     */
    public ScheduledExecutorService getScheduler() {
        return tsp;
    }


    /**
     * Invokes the endpoint synchronously on the caller's thread but reports the outcome through
     * the callback instead of returning it: {@code callback.accept(...)} on success,
     * {@code callback.exception(...)} on failure.
     *
     * @param callback      supplies the input and receives the result or exception
     * @param authorization per-call authorization overriding the template config's, null to use the template's
     * @return this endpoint
     * @throws IOException          when the rate controller threshold is exceeded
     * @throws NullPointerException when callback is null
     */
    public HTTPAPIEndPoint<I, O> syncCall(HTTPCallback<I, O> callback, HTTPAuthorization authorization)
            throws IOException {
        ToRun toRun = new ToRun(callback, authorization);
        checkRateController();
        toRun.run();
        return this;

    }

    /**
     * Invokes the endpoint synchronously with no per-call authorization override
     * (the template config's authorization applies).
     *
     * @param input the typed input encoded into the request, may be null
     * @return the typed api result
     * @throws IOException on i/o failure or when the rate controller threshold is exceeded
     */
    public HTTPAPIResult<O> syncCall(I input)
            throws IOException {
        return syncCall(input, null);
    }


    /**
     * Invokes the endpoint synchronously on the caller's thread: builds a per-call message config
     * from the template, sends it and decodes the response.
     *
     * @param input         the typed input encoded into the request, may be null
     * @param authorization per-call authorization overriding the template config's, null to use the template's
     * @return the typed api result (raw bytes when no data decoder is set)
     * @throws IOException on i/o failure, when the rate controller threshold is exceeded, or
     *                     wrapping any encoder/decoder failure — every call failure surfaces as
     *                     IOException
     */
    public HTTPAPIResult<O> syncCall(I input, HTTPAuthorization authorization) throws IOException {
        checkRateController();
        try {
            HTTPAPIResult<O> httpapiResult;
            HTTPResponseData hrd = OkHTTPCall.send(getOkHttpClient(), createHMCI(input, authorization));
            if (dataDecoder != null)
                httpapiResult = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), dataDecoder.decode(hrd), hrd.getDuration());
            else
                httpapiResult = (HTTPAPIResult<O>) new HTTPAPIResult<byte[]>(hrd.getStatus(), hrd.getHeaders(), hrd.getData(), hrd.getDuration());

            successCounter.incrementAndGet();
            return httpapiResult;
        } catch (Exception e) {
            failedCounter.incrementAndGet();
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e);
        }
    }


    /**
     * Gate for synchronous calls: no-op without a rate controller, otherwise throws when the
     * controller reports its threshold reached (peek first, then consume a slot).
     *
     * @throws IOException when the rate threshold is exceeded
     */
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


    /**
     * Builds the per-call message config: deep-copies the template (url, uri, method, headers,
     * parameters, proxy, redirect, error handling, charset, timeout), applies the data encoder
     * with the input, then the authorization encoder with the per-call or template authorization.
     *
     * @param input         the typed input, may be null
     * @param authorization per-call authorization, null to use the template config's
     * @return the ready-to-send message config
     */
    private HTTPMessageConfigInterface createHMCI(I input, HTTPAuthorization authorization) {

        HTTPMessageConfigInterface ret = HTTPMessageConfig.createAndInit(config.getURL(), config.getURI(), config.getMethod(), config.isSecureCheckEnabled());
        NVGenericMap.copy(config.getHeaders(), ret.getHeaders(), deepCopy);
        NVGenericMap.copy(config.getParameters(), ret.getParameters(), deepCopy);
        ret.setProxyAddress(config.getProxyAddress());
        ret.setRedirectEnabled(config.isRedirectEnabled());
        ret.setHTTPErrorAsException(config.isHTTPErrorAsException());
        ret.setCharset(config.getCharset());
        ret.setTimeout(config.getTimeout());

        if (dataEncoder != null)
            ret = dataEncoder.encode(ret, input);

        ret = authorizationEncoder.encode(ret, authorization != null ? authorization : config.getAuthorization());

        if (log.isEnabled()) log.getLogger().info(GSONUtil.toJSONDefault(ret, true));

        return ret;

    }


    /**
     * Invokes the endpoint asynchronously with no per-call authorization; when a rate controller
     * is set the call is delayed by its next-wait interval.
     *
     * @param callback supplies the input and receives the result or exception
     * @return this endpoint
     * @throws NullPointerException when callback is null
     */
    public HTTPAPIEndPoint<I, O> asyncCall(HTTPCallback<I, O> callback) {
        return asyncCall(callback, null, rateController != null ? rateController.nextWait() : 0);
    }

    /**
     * Invokes the endpoint asynchronously; when a rate controller is set the call is delayed by
     * its next-wait interval.
     *
     * @param callback      supplies the input and receives the result or exception
     * @param authorization per-call authorization overriding the template config's, null to use the template's
     * @return this endpoint
     * @throws NullPointerException when callback is null
     */
    public HTTPAPIEndPoint<I, O> asyncCall(HTTPCallback<I, O> callback, HTTPAuthorization authorization) {
        return asyncCall(callback, authorization, rateController != null ? rateController.nextWait() : 0);
    }


    /**
     * Invokes the endpoint asynchronously after the given delay, preferring the scheduler and
     * falling back to the executor; call failures are reported to the callback, not thrown.
     *
     * @param callback      supplies the input and receives the result or exception
     * @param authorization per-call authorization overriding the template config's, null to use the template's
     * @param delayInMillis dispatch delay in milliseconds, ignored when only an executor is set
     * @return this endpoint
     * @throws IllegalArgumentException when neither a scheduler nor an executor is configured
     * @throws NullPointerException     when callback is null
     */
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


    /**
     * @return the number of calls completed without an exception since creation
     */
    public long successCount() {
        return successCounter.get();
    }

    /**
     * @return the number of calls that ended in an exception since creation; rate-controller
     *         rejections are not counted (the call is never attempted)
     */
    public long failedCount() {
        return failedCounter.get();
    }


    /**
     * Adds the given numeric status codes to the positive-results map; unknown codes are ignored
     * and existing entries are kept.
     *
     * @param httpStatusCodes status codes to mark as successful
     * @return this endpoint
     */
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

    /**
     * Replaces the positive-results map with the given numeric status codes; the map is first
     * cleared (unless the list is null, which is a no-op) and unknown codes are ignored.
     *
     * @param httpStatusCodes status codes to mark as successful
     * @return this endpoint
     */
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

    /**
     * Adds the given status codes to the positive-results map; existing entries are kept.
     *
     * @param httpStatusCodes status codes to mark as successful
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setPositiveResults(HTTPStatusCode... httpStatusCodes) {
        if (httpStatusCodes != null) {
            for (HTTPStatusCode statusCode : httpStatusCodes) {
                positiveResults.put(statusCode.CODE, statusCode);
            }
        }
        return this;
    }

    /**
     * @return the free-form properties attached to this endpoint
     */
    public NVGenericMap getProperties() {
        return properties;
    }

    /**
     * Sets the free-form properties of this endpoint; when the map contains a
     * {@code positive_result_codes} list of integers, it replaces the positive-results map.
     *
     * @param properties the properties map, used as-is (not copied)
     * @return this endpoint
     */
    public HTTPAPIEndPoint<I, O> setProperties(NVGenericMap properties) {
        this.properties = properties;
        if (properties != null) {
            setPositiveResults((List<Integer>) properties.getValue("positive_result_codes"));
        }

        return this;
    }

    /**
     * Looks up a status code in the positive-results map. The endpoint itself never consults this
     * map; it is a contract for callback authors: an {@code HTTPCallback.exception(...)} handler
     * receiving an {@code HTTPCallException} can call
     * {@code getEndpoint().lookupPositiveResult(exception.getStatusCode().CODE)} to reinterpret an
     * http error status (e.g. 409 on a create meaning "already exists") as a positive outcome.
     *
     * @param statusCode the numeric http status code
     * @return the matching status code when it is registered as successful, null otherwise
     */
    public HTTPStatusCode lookupPositiveResult(int statusCode) {
        return positiveResults.get(statusCode);
    }
}
