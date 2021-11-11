package org.zoxweb.server.http;

import java.util.List;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;

public final class HTTPDecoder {

  private HTTPDecoder() {
  }

  public static final DataDecoder<byte[], NVGenericMap> BytesToNVGM = (input) -> {
    return GSONUtil.fromJSONGenericMap(SharedStringUtil.toString(input), null, Base64Type.DEFAULT);
  };

  public static final DataDecoder<HTTPResponseData, NVGenericMap> HRDToNVGM = (input) -> {
    return GSONUtil
        .fromJSONGenericMap(SharedStringUtil.toString(input.getData()), null, Base64Type.DEFAULT);
  };

  public static final DataDecoder<HTTPResponseData, List<NVGenericMap>> HRDToNVGMList = (input) -> {
    return GSONUtil
        .fromJSONGenericMapArray(SharedStringUtil.toString(input.getData()), Base64Type.DEFAULT);
  };

  public static final DataDecoder<byte[], NVEntity> BytesToNVE = (input) -> {
    return GSONUtil.fromJSON(input);
  };

  public static final DataDecoder<HTTPResponseData, NVEntity> HRDToNVE = (input) -> {
    return GSONUtil.fromJSON(input.getData());
  };

}
