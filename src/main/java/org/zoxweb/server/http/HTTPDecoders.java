package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPAttribute;
import org.zoxweb.shared.http.HTTPMediaType;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.util.List;


public final class HTTPDecoders {
  public static final byte[] NAME_EQUAL_DOUBLE_QUOTE = SharedStringUtil.getBytes("name=\"");
  public static final byte[] DOUBLE_QUOTE = {'\"'};
  //public final static Logger log = Logger.getLogger(HTTPDecoder.class.getName());

  private HTTPDecoders() {
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
        HTTPUtil.parseQuery(hmci.getParameters().asArrayValuesString(), hmci.getURI(), true);
        return hmci;
      default:
        if (hrm.isMessageComplete())
        {
          if (HTTPMediaType.lookup(hmci.getContentType()) == HTTPMediaType.APPLICATION_WWW_URL_ENC) {
            int index = hrm.endOfHeadersIndex() + Delimiter.CRLFCRLF.getBytes().length;
            HTTPUtil.parseQuery(hmci.getParameters().asArrayValuesString(), hrm.getDataStream().getString(index),false);
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
          if (HTTPMediaType.lookup(hmci.getContentType()) == HTTPMediaType.MULTIPART_FORM_DATA)
          {
            String boundaryName = HTTPAttribute.BOUNDARY.getValue() + "=";

            int index = SharedStringUtil.indexOf(hmci.getContentType(), boundaryName, 0, true);

            if(index != -1)
            {
              String boundary = SharedStringUtil.trimOrNull(hmci.getContentType().substring(index+boundaryName.length()));
              hmci.setBoundary(boundary);
              // we need to parse the payload next
              index = hrm.endOfHeadersIndex() + Delimiter.CRLFCRLF.getBytes().length;

              UByteArrayOutputStream ubaos = hrm.getDataStream();
              while((index = ubaos.indexOf(index, NAME_EQUAL_DOUBLE_QUOTE)) != -1)
              {
                index += NAME_EQUAL_DOUBLE_QUOTE.length;
                int indexEnd = ubaos.indexOf(index, DOUBLE_QUOTE);
                String name = ubaos.getString(index, indexEnd - index);
                index = ubaos.indexOf(indexEnd, Delimiter.CRLFCRLF.getBytes());
                index += Delimiter.CRLFCRLF.getBytes().length;
                indexEnd = ubaos.indexOf(index, Delimiter.CRLF.getBytes());
                String value = ubaos.getString(index, indexEnd - index);
                hmci.getParameters().add(new NVPair(name, value));
              }

//              String payload = hrm.getUBAOS().getString(index);
//              index = 0;
//              // not very solid
//              while( (index = payload.indexOf("name=\"", index)) != -1)
//              {
//                index += 6;
//                String name = payload.substring(index, payload.indexOf("\"", index));
//                index = payload.indexOf(ProtocolDelimiter.CRLFCRLF.getValue(), index);
//                index += ProtocolDelimiter.CRLFCRLF.getBytes().length;
//                int endOfLine =  payload.indexOf(ProtocolDelimiter.CRLF.getValue(), index);
//                String value = payload.substring(index, endOfLine).trim();
//                index = endOfLine + ProtocolDelimiter.CRLF.getBytes().length;
//                hmci.getParameters().add(new NVPair(name, value));
//              }
              return hmci;
            }
          }
        }
    }

    return null;
  };

}
