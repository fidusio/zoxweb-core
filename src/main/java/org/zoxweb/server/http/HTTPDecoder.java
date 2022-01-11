package org.zoxweb.server.http;

import java.util.List;
import java.util.logging.Logger;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.protocol.ProtocolDelimiter;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;

public final class HTTPDecoder {
  private final static Logger log = Logger.getLogger(HTTPDecoder.class.getName());

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


  public static final DataDecoder<HTTPRawMessage, HTTPMessageConfigInterface> WWW_URL_ENC = (hrm) ->{
    HTTPMessageConfigInterface hmci = hrm.getHTTPMessageConfig();
    switch(hmci.getMethod())
    {
      case GET:
        HTTPUtil.parseQuery(hmci.getParameters(), hmci.getURI(), true);
        return hmci;
      default:
        if (hrm.isMessageComplete())
        {
          if (HTTPMimeType.lookup(hmci.getContentType()) == HTTPMimeType.APPLICATION_WWW_URL_ENC) {
            int index = hrm.endOfHeadersIndex() + ProtocolDelimiter.CRLFCRLF.getBytes().length;
            HTTPUtil.parseQuery(hmci.getParameters(), hrm.getUBAOS().getString(index),false);
            return hmci;
          }
        }
    }

    return null;
  };


  public static final DataDecoder<HTTPRawMessage, HTTPMessageConfigInterface> MULTIPART_FORM_DATA = (hrm) ->{
    HTTPMessageConfigInterface hmci = hrm.getHTTPMessageConfig();
    switch(hmci.getMethod())
    {
      case GET:
        break;
      default:
        if (hrm.isMessageComplete())
        {
          if (HTTPMimeType.lookup(hmci.getContentType()) == HTTPMimeType.MULTIPART_FORM_DATA)
          {
            String boundaryName = HTTPHeaderValue.BOUNDARY.getValue() + "=";
            int index = SharedStringUtil.indexOf(hmci.getContentType(), boundaryName, 0, true);
            if(index != -1)
            {
              String boundary = SharedStringUtil.trimOrNull(hmci.getContentType().substring(index+boundaryName.length()));
              hmci.setBoundary(boundary);
              // we need to parse the payload next
              index = hrm.endOfHeadersIndex() + ProtocolDelimiter.CRLFCRLF.getBytes().length;
              String payload = hrm.getUBAOS().getString(index);



              return hmci;
            }


          }
        }
    }

    return null;
  };

}
