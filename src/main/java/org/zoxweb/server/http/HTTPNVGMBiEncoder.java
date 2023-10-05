package org.zoxweb.server.http;

import com.google.gson.annotations.SerializedName;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;

public class HTTPNVGMBiEncoder
    implements BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface>
{
    private final NVGenericMap config;

    @SerializedName("param_name")
    private final String paramName;
    public HTTPNVGMBiEncoder(NVGenericMap config, String paramName){
        this.config = config;
        this.paramName = paramName;
    }

    @Override
    public HTTPMessageConfigInterface encode(HTTPMessageConfigInterface hmci, NVGenericMap params)
    {
        NVGenericMap content = NVGenericMap.copy(config, true);
        NVGenericMap attributes = content.lookup(paramName);
        if (attributes != null && params != null)
        {
            for(GetNameValue<?> param: params.values())
                attributes.add(param);
        }

        hmci.setContent(GSONUtil.toJSONDefault(content));


        return hmci;
    }

}
