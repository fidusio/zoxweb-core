package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;


public final class HTTPCodecs {
  public final static LogWrapper log = new LogWrapper(HTTPCodecs.class).setEnabled(false);

  private HTTPCodecs() {
  }

  public static final DataDecoder<byte[], NVGenericMap> BytesToNVGM = (input) -> {
    return GSONUtil.fromJSONGenericMap(SharedStringUtil.toString(input), null, Base64Type.DEFAULT);
  };

  public static final DataDecoder<HTTPResponseData, NVGenericMap> HRDToNVGM = (input) -> {
    return GSONUtil
        .fromJSONGenericMap(SharedStringUtil.toString(input.getData()), null, Base64Type.DEFAULT);
  };


  public static final DataDecoder<HTTPResponseData, NVGenericMap> NVGMDecoderPAS = (input)->
          GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class, true);

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
            NamedValue<String> multipartFromData = HTTPHeaderParser.parseHeader(HTTPHeader.CONTENT_TYPE.getName() +": " + hmci.getContentType());

            String boundary = multipartFromData.getProperties().getValue(HTTPConst.CNP.BOUNDARY);
            if(log.isEnabled()) log.getLogger().info("boundary=" + boundary);



            if(SUS.isNotEmpty(boundary))
            {

              byte[] boundaryTag = SharedStringUtil.getBytes("--" + boundary);
              byte[] boundaryStart = SharedStringUtil.getBytes("--" + boundary + Delimiter.CRLF.getValue());
              byte[] boundaryEnd = SharedStringUtil.getBytes("--" + boundary + "--");

              UByteArrayOutputStream ubaos = hrm.getDataStream();
              // check is boundaryEnd exit
              if(ubaos.indexOf(hrm.endOfHeadersIndex(), boundaryEnd) == -1 )
              {
                throw new IllegalArgumentException("boundary end " + SharedStringUtil.toString(boundaryEnd) + " not found");
              }

              hmci.setBoundary(boundary);

              // we need to parse the payload next
              int index = hrm.endOfHeadersIndex();
              if(log.isEnabled()) log.getLogger().info("index of the end main headers: " + index);

              while((index = ubaos.indexOf(index, boundaryStart)) != -1)
              {
                index += boundaryStart.length;

                if(log.isEnabled()) log.getLogger().info("index of start of a part: "  + index);
                // index point to the first --boundary\r\n
                int endOfSubHeaders = ubaos.indexOf(index, Delimiter.CRLFCRLF.getBytes());
                if(endOfSubHeaders == -1)
                { // we have a problem
                  if (log.isEnabled()) log.getLogger().info(index + " end of sub headers missing from index " + index);
                }
                else
                  endOfSubHeaders += Delimiter.CRLFCRLF.getBytes().length;


                int endOfSubPart = ubaos.indexOf(endOfSubHeaders, boundaryTag);



                if (log.isEnabled()) log.getLogger().info(index + " end of SubHeader: " + endOfSubHeaders + " end of SubPart" + " content\n{" +
                        ubaos.getString(index, endOfSubPart - index - Delimiter.CRLF.getBytes().length) + "}");


                int startOfData = endOfSubHeaders;
                int dataLength = endOfSubPart - endOfSubHeaders - Delimiter.CRLF.getBytes().length;

                if(log.isEnabled()) log.getLogger().info("data: \"" + ubaos.getString(startOfData, dataLength) +"\"");



                // index points to the next sub-http-header
                // parse one sub part
                int headerIndex = index;
                //Map<String, NamedValue<String>> subPartRawHeaders = new HashMap<>();
                NVGenericMap subPartRawHeaders = new NVGenericMap();

                // header parsing
                while(headerIndex != -1 && headerIndex < endOfSubPart)
                {

                  int indexEndOfHeader = ubaos.indexOf(headerIndex, Delimiter.CRLF.getBytes());
                  if (indexEndOfHeader != -1 && indexEndOfHeader < endOfSubHeaders)
                  {
                    String fullHeader = ubaos.getString(headerIndex, indexEndOfHeader - headerIndex);
                    NamedValue<String> header = HTTPHeaderParser.parseHeader(fullHeader);
                    if (header != null) {
                      subPartRawHeaders.build(header);
//                      log.getLogger().info("header: " + fullHeader + " end");
                    }
                  }
                  headerIndex = indexEndOfHeader + Delimiter.CRLF.getBytes().length;
                }

                if(log.isEnabled()) log.getLogger().info("headers:\n" + subPartRawHeaders);


                NamedValue<String> contentDisposition = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.CONTENT_DISPOSITION);
                NamedValue<String> contentType = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.CONTENT_TYPE);
                String name = null;
                String filename = null;
                String mediaType = null;
                ByteArrayInputStream is = null;
                if (contentDisposition != null)
                {
                  name = contentDisposition.getProperties().getValue("name");
                  filename = contentDisposition.getProperties().getValue("filename");
                }
                if(contentType != null)
                {
                  mediaType = contentType.getValue();
                }

                if (filename != null)
                {
                  // we covert to input stream
                  is = new ByteArrayInputStream(ubaos.getInternalBuffer(), startOfData, dataLength);
                }

                if(log.isEnabled()) log.getLogger().info("name=" + name + " filename=" + filename + " mediaType=" + mediaType + " is=" + is);

                if (name != null)
                {
                  if (is != null && mediaType != null && HTTPMediaType.lookup(mediaType) != HTTPMediaType.APPLICATION_JSON)
                  {
                    NamedValue<InputStream> toAdd = new NamedValue<>();
                    toAdd.setName(name).setValue(is);
                    if (filename != null)
                      toAdd.getProperties().build("filename", filename);
                    if(mediaType != null)
                      toAdd.getProperties().build("media-type", mediaType);

                    toAdd.getProperties().build(new NVLong("length", is.available()));
                    if(log.isEnabled())  log.getLogger().info("toAdd: " + toAdd);
                    hmci.getParameters().add(toAdd);
                  }
                  else
                  {
                    String value = ubaos.getString(startOfData, dataLength);
                    if (mediaType != null && HTTPMediaType.lookup(mediaType) == HTTPMediaType.APPLICATION_JSON)
                    {
                      NVGenericMap toAdd = GSONUtil.fromJSONDefault(ubaos.getString(startOfData, dataLength), NVGenericMap.class);
                      toAdd.setName(name);
                      hmci.getParameters().build(toAdd);
                    }
                    else
                    {
                      NVPair nvp = new NVPair(name, value);
                      hmci.getParameters().build(nvp);
                      if (log.isEnabled()) log.getLogger().info("adding nvp: " + nvp);
                    }
                  }
                }


                index = endOfSubPart;

                if ((index + boundaryEnd.length) >= ubaos.size())
                  break;
                // we need to parse the delimiters of content-disposition
              }


              return hmci;
            }
          }
        }
    }

    return null;
  };

}
