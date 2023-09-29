package org.zoxweb.server.http;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.util.*;

import java.util.*;

public final class HTTPAPIManager
{

    public static final HTTPAPIManager SINGLETON = new HTTPAPIManager();
    private final Map<String, HTTPAPIEndPoint<?,?>> map = new LinkedHashMap<String, HTTPAPIEndPoint<?,?>>();


    public static final DataDecoder<HTTPResponseData, NVGenericMap> NVGM_DECODER = new DataDecoder<HTTPResponseData, NVGenericMap>() {
        @Override
        public NVGenericMap decode(HTTPResponseData input)
        {
            return GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class, true);
        }
    };


    private HTTPAPIManager()
    {

    }


    public  HTTPAPIEndPoint<NVGenericMap, NVGenericMap> buildEndPoint(HTTPMessageConfigInterface config,
                                                                      BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface> encoder,
                                                                      DataDecoder<HTTPResponseData, NVGenericMap> decoder)
    {
        return buildEndPoint(config.getName(), null, config.getDescription(), config, encoder, decoder);
    }

    public  HTTPAPIEndPoint<NVGenericMap, NVGenericMap> buildEndPoint(String name,
                                                                      String domain,
                                                                    String description,
                                                                    HTTPMessageConfigInterface config,
                                                                    BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface> encoder,
                                                                    DataDecoder<HTTPResponseData, NVGenericMap> decoder)
    {
        return  new HTTPAPIEndPoint<NVGenericMap, NVGenericMap>(config)
                .setName(name)
                .setDomain(domain)
                .setDescription(description)
                .setDataEncoder(encoder)
                .setDataDecoder(decoder);
    }



    public <I,O> HTTPAPIManager register(HTTPAPIEndPoint<I,O> apiEndPoint)
    {
        if (apiEndPoint.toCanonicalID() != null)
        {
            synchronized (this)
            {
                map.put(apiEndPoint.toCanonicalID(), apiEndPoint);
            }
        }

        return this;
    }

    public HTTPAPIManager unregister(String name)
    {
        if (name != null)
        {
            synchronized (this)
            {
                map.remove(name);
            }
        }

        return this;
    }


    public HTTPAPIManager unregister(String name, String domain)
    {
        domain = SharedStringUtil.trimOrNull(domain);
        if (name != null)
        {
            return unregister(domain != null ? SharedUtil.toCanonicalID('.', domain, name) : name);
        }

        return this;
    }


    public <I,O> HTTPAPIManager unregister(HTTPAPIEndPoint<I,O> apiEndPoint)
    {
        if (apiEndPoint.toCanonicalID() != null)
        {
            synchronized (this)
            {
                map.remove(apiEndPoint.toCanonicalID(), apiEndPoint);
            }
        }

        return this;
    }

    public <I,O> HTTPAPIEndPoint<I,O> lookup(String name)
    {
        return (HTTPAPIEndPoint<I, O>) map.get(name);
    }

    public HTTPAPIEndPoint[] getAll()
    {
        return map.values().toArray(new HTTPAPIEndPoint[0]);
    }


    public String[] getAllIDs()
    {
        return map.keySet().toArray(new String[0]);
    }


    public HTTPAPIEndPoint[] getDomainEndPoints(String domain)
    {


        List<HTTPAPIEndPoint> ret = new ArrayList<>();
        domain = SharedStringUtil.trimOrNull(domain);
        if (domain != null) {
            domain = domain.endsWith(".") ? domain : domain + ".";
            synchronized (this) {
                Set<Map.Entry<String, HTTPAPIEndPoint<?, ?>>> entries = map.entrySet();
                for (Map.Entry<String, HTTPAPIEndPoint<?, ?>> entry : entries) {
                    if (entry.getKey().startsWith(domain))
                        ret.add(entry.getValue());
                }
            }
        }

        return ret.toArray(new HTTPAPIEndPoint[0]);
    }

}
