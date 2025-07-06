package org.zoxweb.server.http;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPAuthorization;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public final class HTTPAPIManager
    extends RegistrarMap<String, HTTPAPIEndPoint<?,?>, HTTPAPIManager>
{


    public static final LogWrapper log = new LogWrapper(HTTPAPIManager.class);

    public static final HTTPAPIManager SINGLETON = new HTTPAPIManager();

    private final Map<String, Set<HTTPAPIEndPoint<?,?>>> groups = new ConcurrentSkipListMap<>();

    //private final Map<String, HTTPAPIEndPoint<?,?>> map = new LinkedHashMap<String, HTTPAPIEndPoint<?,?>>();




    private HTTPAPIManager()
    {
        super(new LinkedHashMap<String, HTTPAPIEndPoint<?,?>>());
    }


    public HTTPAPIEndPoint<NVGenericMap, NVGenericMap> buildEndPoint(NVGenericMap nvgm) throws ClassNotFoundException {
        HTTPMessageConfigInterface config = nvgm.getValue("hmci_config");
        String domain = nvgm.getValue("domain");
        NVGenericMap encoderMeta = (NVGenericMap) nvgm.get("data_encoder");
        if(log.isEnabled())
        {
            log.getLogger().info("encoder config: " + encoderMeta);
            log.getLogger().info("domain: " + domain);
            log.getLogger().info("hmci: " + config);
        }

        HTTPNVGMBiEncoder encoder = buildCodec(encoderMeta);//(HTTPNVGMBiEncoder) GSONUtil.fromJSONDefault(json, Class.forName(metaType));
        DataDecoder decoder = buildCodec((NVGenericMap)nvgm.get("data_decoder"));
        NVGenericMap nvgmRateController = (NVGenericMap)nvgm.get("rate_controller");
        RateController rateController = null;
        if (nvgmRateController != null)
        {
            rateController = new RateController(nvgmRateController.getValue("name"), nvgmRateController.getValue("rate"));
        }




        return buildEndPoint(config, encoder, decoder)
                .setRateController(rateController)
                .setScheduler(TaskUtil.defaultTaskScheduler())
                .setDomain(domain)
                .setProperties(nvgm.lookup("properties"));
//                .setPositiveResults((List<Integer>) nvgm.getValue("positive_result_codes"));
    }


    public <V extends Codec> V buildCodec(NVGenericMap nvgm) throws ClassNotFoundException
    {
        GetNameValue<String> id = nvgm.lookup("id");
        if (id != null)
        {
            if ("NVGM_DECODER_PAS".equals(id.getValue()))
                return (V) HTTPCodecs.NVGMDecoderPAS;
        }

        String metaType = nvgm.getValue("meta_type");

        // since content by default will be processed as NVGenericMap
        NVGenericMap content = nvgm.lookup("content");
        // reconvert content as string
        String json = GSONUtil.toJSONDefault(content);

        // convert json string to actual codec Object
        return (V)GSONUtil.fromJSONDefault(json, Class.forName(metaType));
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


    public  <I,O> HTTPAPIEndPoint<I, O> buildEndPoint(String name,
                                                      String domain,
                                                      String description,
                                                      HTTPMessageConfigInterface config)
    {
        return (HTTPAPIEndPoint<I, O>) new HTTPAPIEndPoint<>(config)
                .setName(name)
                .setDomain(domain)
                .setDescription(description);
    }

    public  <I,O> HTTPAPIEndPoint<I, O> buildEndPoint(GetName gn,
                                                      String domain,
                                                      String description,
                                                      HTTPMessageConfigInterface config)
    {
        return buildEndPoint(gn.getName(), domain, description, config);
    }





    public synchronized  <I,O> HTTPAPIManager register(HTTPAPIEndPoint<I,O> apiEndPoint)
    {
        if (apiEndPoint.toCanonicalID() != null)
        {
            return super.register(apiEndPoint.toCanonicalID(), apiEndPoint);
        }
        return this;
    }


    public HTTPAPIEndPoint<?,?> unregister(String name)
    {
        if (name != null)
        {
            synchronized (this)
            {
                return super.unregister(name);
            }
        }
        return null;
    }



    public HTTPAPIEndPoint<?,?> unregister(String name, String domain)
    {
        domain = SharedStringUtil.trimOrNull(domain);
        if (name != null)
        {
            return unregister(domain != null ? SharedUtil.toCanonicalID('.', domain, name) : name);
        }

        return null;
    }


    public HTTPAPIEndPoint<?,?> unregister(HTTPAPIEndPoint<?,?> apiEndPoint)
    {

        if (apiEndPoint != null)
            return unregister(apiEndPoint.toCanonicalID());
        return null;
    }



    public HTTPAPIEndPoint<?,?>[] getAll()
    {
        return getMapCache().values().toArray(new HTTPAPIEndPoint[0]);
    }


    public String[] getAllIDs()
    {
        return getMapCache().keySet().toArray(new String[0]);
    }


    public HTTPAPIEndPoint<?,?>[] getDomainEndPoints(String domain, boolean copy)
    {
        List<HTTPAPIEndPoint<?,?>> ret = new ArrayList<>();
        domain = SharedStringUtil.trimOrNull(domain);
        if (domain != null) {
            domain = domain.endsWith(".") ? domain : domain + ".";
            synchronized (this) {
                Set<Map.Entry<String, HTTPAPIEndPoint<?,?>>> entries = entrySet();
                for (Map.Entry<String, HTTPAPIEndPoint<?,?>> entry : entries) {
                    if (entry.getKey().startsWith(domain))
                        ret.add(copy ? entry.getValue().copy(true) : entry.getValue());
                }
            }
        }

        return ret.toArray(new HTTPAPIEndPoint[0]);
    }

    public  <V extends HTTPAPICaller> V createAPICaller(String domain, String name, HTTPAuthorization authorization)
    {
        HTTPAPIEndPoint<?,?>[] domainEndPoints = getDomainEndPoints(domain, true);
        Map<String, HTTPAPIEndPoint<?,?>>  endPointMap = new HashMap<>();
        for (HTTPAPIEndPoint<?,?> haep : domainEndPoints)
        {
            log.getLogger().info(haep.toCanonicalID());
            endPointMap.put(haep.toCanonicalID(), haep);
        }
        return new HTTPAPICaller(name, endPointMap).setHTTPAuthorization(authorization).setDomain(domain);
    }

    public  <V extends HTTPAPICaller> V buildAPICaller(V apiCaller, String domain, NVGenericMap props)
    {
        HTTPAPIEndPoint<?,?>[] domainEndPoints = getDomainEndPoints(domain, true);
        Map<String, HTTPAPIEndPoint<?,?>>  endPointMap = new HashMap<>();
        for (HTTPAPIEndPoint<?,?> haep : domainEndPoints)
        {
            log.getLogger().info(haep.toCanonicalID());
            endPointMap.put(haep.toCanonicalID(), haep);
        }
        apiCaller.setEndPoints(endPointMap);
        if(props != null)
        {
            HTTPAuthorization authorization = props.getValue(HTTPAPIBuilder.Prop.AUTHZ);
            String url = props.getValue(HTTPAPIBuilder.Prop.URL);
            if (authorization != null) apiCaller.setHTTPAuthorization(authorization);
            if (url != null) apiCaller.updateURL(url);
        }
        return apiCaller.setDomain(domain);
    }

}
