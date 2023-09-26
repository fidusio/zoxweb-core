package org.zoxweb.server.http;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.NVGenericMap;

public final class HTTPAPIBuilder
{

    public static final DataDecoder<HTTPResponseData, NVGenericMap> NVGM_DECODER = new DataDecoder<HTTPResponseData, NVGenericMap>() {
        @Override
        public NVGenericMap decode(HTTPResponseData input)
        {
            return GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class, true);
        }
    };


    private HTTPAPIBuilder()
    {

    }


    public static HTTPAPIEndPoint<NVGenericMap, NVGenericMap> buildEndPoint(HTTPMessageConfigInterface config,
                                                                    BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface> encoder,
                                                                    DataDecoder<HTTPResponseData, NVGenericMap> decoder)
    {
        return buildEndPoint(config.getName(), config.getDescription(), config, encoder, decoder);
    }

    public static HTTPAPIEndPoint<NVGenericMap, NVGenericMap> buildEndPoint(String name,
                                                                    String description,
                                                                    HTTPMessageConfigInterface config,
                                                                    BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface> encoder,
                                                                    DataDecoder<HTTPResponseData, NVGenericMap> decoder)
    {
        return  new HTTPAPIEndPoint<NVGenericMap, NVGenericMap>(config)
                .setName(name)
                .setDescription(description)
                .setDataEncoder(encoder)
                .setDataDecoder(decoder);
    }

}
