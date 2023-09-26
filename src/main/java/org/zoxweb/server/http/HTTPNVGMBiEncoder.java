package org.zoxweb.server.http;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;

public class HTTPNVGMBiEncoder
    implements BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface>
{
    private final NVGenericMap config;
    private final String attributesName;
    public HTTPNVGMBiEncoder(NVGenericMap config, String attributesName){
        this.config = config;
        this.attributesName = attributesName;
    }

    @Override
    public HTTPMessageConfigInterface encode(HTTPMessageConfigInterface hmci, NVGenericMap params)
    {
        NVGenericMap content = NVGenericMap.copy(config, true);
        NVGenericMap attributes = content.lookup(attributesName);
        if (attributes != null && params != null)
        {
            for(GetNameValue<?> param: params.values())
                attributes.add(param);
        }

        hmci.setContent(GSONUtil.toJSONDefault(content));


        return hmci;
    }

}
