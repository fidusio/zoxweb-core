package org.zoxweb.server.http;

import okhttp3.OkHttpClient;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.http.HTTPAPIResult;
import org.zoxweb.shared.http.HTTPAuthorization;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.util.Map;

public class HTTPAPICaller
    implements GetName, GetDescription
{
    public static class Callback<I,O>
        extends HTTPCallback<I,O>
    {
        private ConsumerCallback<O> callback;

        private Callback(I input, ConsumerCallback<O> callback)
        {
            super(input);
            this.callback = callback;
        }

        /**
         * Performs this operation on the given argument.
         *
         * @param httpapiResult the input argument
         */
        @Override
        public void accept(HTTPAPIResult<O> httpapiResult)
        {
            if(callback != null)
                callback.accept(httpapiResult.getData());
        }

        /**
         * @param
         */
        @Override
        public void exception(Exception e)
        {
            if(callback != null)
                callback.exception(e);
        }

    }



    private final NamedDescription namedDescription;

    private HTTPAuthorization httpAuthorization;

    private OkHttpClient okHttpClient;

    private final Map<String, HTTPAPIEndPoint> endpoints;



    private String domain;

    protected HTTPAPICaller(String name, Map<String, HTTPAPIEndPoint> endpoints)
    {
        this(name, null, endpoints);
    }

    protected HTTPAPICaller(String name, String description, Map<String, HTTPAPIEndPoint> endpoints)
    {
        namedDescription = new NamedDescription(name.toLowerCase().trim(), description);
        this.endpoints = endpoints;
    }

    /**
     * Returns the property description.
     *
     * @return description
     */
    @Override
    public String getDescription() {
        return namedDescription.getDescription();
    }

    /**
     * @return the name of the object
     */
    @Override
    public String getName() {
        return namedDescription.getName();
    }

    public HTTPAPICaller setHTTPAuthorization(HTTPAuthorization httpAuthorization)
    {
        this.httpAuthorization = httpAuthorization;
        return this;
    }

    public HTTPAuthorization getHTTPAuthorization()
    {
        return httpAuthorization;
    }


    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public HTTPAPICaller setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        return this;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof HTTPAPICaller)
        {
            return namedDescription.equals(((HTTPAPICaller) obj).namedDescription);
        }
        return false;
    }

    public int hashCode()
    {
        return namedDescription.hashCode();
    }

    public String getDomain()
    {
        return domain;
    }

    protected HTTPAPICaller setDomain(String domain)
    {
        this.domain = domain;
        return this;
    }

    public HTTPAPICaller updateURL(String url)
    {
        url = FilterType.URL.validate(url);
        for(HTTPAPIEndPoint<?,?> haep : endpoints.values())
            haep.getConfig().setURL(url);

        return this;
    }



    public <I,O> HTTPCallback<I,O> asyncCall(String endpointName, I input, ConsumerCallback<O> consumerCallback)
    {
        HTTPAPIEndPoint<I,O> endPoint = endpoints.get(SUS.toCanonicalID('.', domain, endpointName));
        if (endPoint == null)
            throw new IllegalArgumentException("endpoint " + endpointName + " not found");
        Callback<I,O> callback = new Callback<>(input, consumerCallback);
        endPoint.asyncCall(callback, httpAuthorization);
        return callback;
    }



    public <I,O> O syncCall(String endpointName, I input) throws IOException
    {
        HTTPAPIEndPoint<I,O> endPoint = endpoints.get(SUS.toCanonicalID('.', domain, endpointName));
        return endPoint.syncCall(input, httpAuthorization).getData();
    }
}
