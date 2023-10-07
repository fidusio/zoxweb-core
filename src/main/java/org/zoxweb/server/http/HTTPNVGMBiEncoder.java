package org.zoxweb.server.http;

import com.google.gson.annotations.SerializedName;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SetName;

import java.util.HashMap;
import java.util.Map;

public class HTTPNVGMBiEncoder
    implements BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface>
{
    private final NVGenericMap config;

    @SerializedName("attribute_name")
    private final String attributeName;
    @SerializedName("params_to_include")
    private final HashMap<String, String> mappedRequiredParameters;
    public HTTPNVGMBiEncoder(NVGenericMap config, String attributeName, String... inclusionParameters)
    {
        this.config = config;
        this.attributeName = attributeName;
        if(inclusionParameters != null)
        {
            mappedRequiredParameters = new HashMap<>();
            for(String toInclude : inclusionParameters)
            {
                if(toInclude != null)
                    mappedRequiredParameters.put(toInclude, toInclude);
            }
        }
        else {
            mappedRequiredParameters = null;
        }

    }

    public HTTPNVGMBiEncoder(NVGenericMap config, String attributeName, Map<String,String> requiredParametersMap)
    {
        this.config = config;
        this.attributeName = attributeName;
        if(requiredParametersMap != null)
        {
            mappedRequiredParameters = new HashMap<>();
            for(Map.Entry<String, String> toInclude : requiredParametersMap.entrySet())
            {
                if(toInclude != null && toInclude.getKey() != null)
                    mappedRequiredParameters.put(toInclude.getKey(), toInclude.getValue() != null ? toInclude.getValue() : toInclude.getKey());
            }
        }
        else {
            mappedRequiredParameters = null;
        }

    }

    @Override
    public HTTPMessageConfigInterface encode(HTTPMessageConfigInterface hmci, NVGenericMap params)
    {
        NVGenericMap content = NVGenericMap.copy(config, true);
        NVGenericMap attributes = content.lookup(attributeName);
        if (attributes != null && params != null)
        {
            for(GetNameValue<?> param: params.values())
            {
                if (mappedRequiredParameters != null) {
                    if (mappedRequiredParameters.containsKey(param.getName()))
                    {
                        String mappedName = mappedRequiredParameters.get(param.getName());
                        if(mappedName != null)
                        {
                            // this might throw an execption
                            ((SetName)param).setName(mappedName);
                        }
                        attributes.add(param);
                    }
                }
                else
                    attributes.add(param);

            }
        }
        hmci.setContent(GSONUtil.toJSONDefault(content));
        return hmci;
    }

}
