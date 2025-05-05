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


public final class HTTPCodecs {
  public final static LogWrapper log = new LogWrapper(HTTPCodecs.class).setEnabled(false);

  private HTTPCodecs() {
  }

  public static final DataDecoder<byte[], NVGenericMap> BytesToNVGM = (input) -> GSONUtil.fromJSONGenericMap(SharedStringUtil.toString(input), null, Base64Type.DEFAULT);

  public static final DataDecoder<HTTPResponseData, NVGenericMap> HRDToNVGM = (input) -> GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class);



  public static final DataDecoder<HTTPResponseData, NVGenericMap> NVGMDecoderPAS = (input) -> GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class, true);

  public static final DataDecoder<HTTPResponseData, NVGenericMapList> HRDToNVGMList = (input) -> GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMapList.class);


  public static final DataDecoder<byte[], NVEntity> BytesToNVE = GSONUtil::fromJSON;


  public static final DataDecoder<HTTPResponseData, NVEntity> HRDToNVE = (input) -> GSONUtil.fromJSON(input.getData());



  public static final DataDecoder<HTTPRawMessage, HTTPMessageConfigInterface> WWW_URL_ENC = (hrm) ->
  {
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
            HTTPUtil.parseQuery(hmci.getParameters().asArrayValuesString(), hrm.getDataStream().getString(0),false);
            return hmci;
          }
        }
    }

    return null;
  };


  public static final DataDecoder<HTTPRawMessage, HTTPRawMessage> TRANSFER_CHUNKED = (hrm)->
  {
    // the headers are already parsed
    // we need to parse the body as chunks
    // hex-size\r\n
    // binary-data\r\n
    //
    UByteArrayOutputStream ubaos = hrm.getDataStream();


    do
    {
      // parse the data size line in hex
      int index = ubaos.indexOf(hrm.getDataMark(), Delimiter.CRLF.getBytes());
      if(index == -1)
        break;
      String hexSize = ubaos.getString(hrm.getDataMark(), index - hrm.getDataMark());
      int chunkSize = SharedUtil.hexToInt(hexSize);
      if(chunkSize == 0)
      {
        // we have the last chunk
        hrm.endOfContentReached();
        // delete the last 0 chunk and trailing headers
        ubaos.removeAt(hrm.getDataMark(), ubaos.size() - hrm.getDataMark());

        // for now NO support for trailer header
        break;
      }

      if(ubaos.size() >= (index + Delimiter.CRLF.length() + chunkSize + Delimiter.CRLF.length()))
      {


        if(ubaos.byteAt(index + Delimiter.CRLF.length() + chunkSize ) == '\r' &&
                ubaos.byteAt(index + Delimiter.CRLF.length() + chunkSize + 1) == '\n')
        {
          // we have at least one full chunk

          // 1. remove the hex-size\r\n
          ubaos.shiftLeft(index + Delimiter.CRLF.length(), hrm.getDataMark());

          // 2. remove the \r\n at the binary data
          ubaos.removeAt(hrm.getDataMark() + chunkSize, Delimiter.CRLF.length());
          hrm.incDataMark(chunkSize);

          continue;
        }
        else
        {
          // we have a problem
          log.getLogger().info(index + "The end of the chunked is missing " + index + Delimiter.CRLF.length() + chunkSize);
        }
      }
      // datamark should be used by subsequent decoder as the end of HTTPRawMessage.getDataStream() as the end of the stream
      // after processing the data they should invoke
      // HTTPRawMessage.getDataStream().shiftLeft(datamark, 0)
      // then set datamark to zero
      // so the next call to TRANSFER_CHUNKED will process the remaining data
      break;

    }while(true);


    return hrm;
  };




  public static final DataDecoder<HTTPRawMessage, HTTPMessageConfigInterface> MULTIPART_FORM_DATA = (hrm) ->
  {

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
            NamedValue<String> multipartFromData = HTTPHeaderParser.parseFullLineHeader(HTTPHeader.CONTENT_TYPE.getName() +": " + hmci.getContentType());

            String boundary = multipartFromData.getProperties().getValue(HTTPConst.CNP.BOUNDARY);
            if(log.isEnabled()) log.getLogger().info("boundary=" + boundary);



            if(SUS.isNotEmpty(boundary))
            {

              byte[] boundaryTag = SharedStringUtil.getBytes("--" + boundary);
              byte[] boundaryStart = SharedStringUtil.getBytes("--" + boundary + Delimiter.CRLF.getValue());
              byte[] boundaryEnd = SharedStringUtil.getBytes("--" + boundary + "--" + Delimiter.CRLF.getValue());

              UByteArrayOutputStream ubaos = hrm.getDataStream();
              // check is boundaryEnd exit
              if(ubaos.indexOf(0, boundaryEnd) == -1 )
              {
                throw new IllegalArgumentException("boundary end " + SharedStringUtil.toString(boundaryEnd).trim() + " not found");
              }

              hmci.setBoundary(boundary);

              // we need to parse the payload next
              int index = 0;
              if(log.isEnabled()) log.getLogger().info("index of the end main headers: " + index);

              while((index = ubaos.indexOf(index, boundaryStart)) != -1)
              {
                index += boundaryStart.length;

                if(log.isEnabled()) log.getLogger().info("index of start of a part: "  + index);
                // index point to the first --boundary\r\n
                int endOfSubHeaders = ubaos.indexOf(index, Delimiter.CRLFCRLF.getBytes());
                if(endOfSubHeaders == -1)
                { // we have a problem
                  log.getLogger().info(index + " end of sub headers missing from index " + index);
                }
                else
                  endOfSubHeaders += Delimiter.CRLFCRLF.length();


                int endOfSubPart = ubaos.indexOf(endOfSubHeaders, boundaryTag);



                if (log.isEnabled()) log.getLogger().info(index + " end of SubHeader: " + endOfSubHeaders + " end of SubPart" + " content\n{" +
                        ubaos.getString(index, endOfSubPart - index - Delimiter.CRLF.length()) + "}");


                int startOfData = endOfSubHeaders;
                int dataLength = endOfSubPart - endOfSubHeaders - Delimiter.CRLF.length();

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
                    NamedValue<String> header = HTTPHeaderParser.parseFullLineHeader(fullHeader);
                    if (header != null) {
                      subPartRawHeaders.build(header);
//                      log.getLogger().info("header: " + fullHeader + " end");
                    }
                  }
                  headerIndex = indexEndOfHeader + Delimiter.CRLF.length();
                }

                if(log.isEnabled()) log.getLogger().info("headers:\n" + subPartRawHeaders);


                NamedValue<String> contentDisposition = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.CONTENT_DISPOSITION);
                NamedValue<String> contentType = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.CONTENT_TYPE);
                String name = null;
                String filename = null;
                String mediaType = null;
                InputStream is = null;
                //List<BytesArray> lBytesArray = new ArrayList<>();
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
                  if (is != null  && mediaType != null && HTTPMediaType.lookup(mediaType) != HTTPMediaType.APPLICATION_JSON)
                  {
                    NamedValue<InputStream> toAdd = new NamedValue<>();
                    toAdd.setName(name).setValue(is);
                    if (filename != null)
                      toAdd.getProperties().build("filename", filename);

                    toAdd.getProperties().build("media-type", mediaType);

                    toAdd.getProperties().build(new NVLong("length", dataLength));
                    if(log.isEnabled())  log.getLogger().info("toAdd: " + toAdd);
                    // must add it a t the end

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











  public static final DataDecoder<HTTPRawMessage, HTTPMessageConfigInterface> MULTIPART_FORM_DATA_CHUNKED = (hrm) ->
  {

    HTTPMessageConfig hmci = (HTTPMessageConfig) hrm.getHTTPMessageConfig();
    switch(hmci.getMethod())
    {
      case GET:
        break;
      default:
        if (hrm.isMessageComplete()) // this need to change we are doing partial processing
        {
//          if (HTTPMediaType.lookup(hmci.getContentType()) == HTTPMediaType.MULTIPART_FORM_DATA)
          if (hmci.isContentMultipartFormData())
          {
            if (SUS.isEmpty(hmci.getBoundary()))
            {
              NamedValue<String> multipartFromData = HTTPHeaderParser.parseFullLineHeader(HTTPHeader.CONTENT_TYPE.getName() + ": " + hmci.getContentType());

              String boundary = multipartFromData.getProperties().getValue(HTTPConst.CNP.BOUNDARY);
              hmci.setBoundary(boundary);
              if (log.isEnabled()) log.getLogger().info("boundary=" + boundary);
            }


            if(SUS.isNotEmpty(hmci.getBoundary()))
            {

              byte[] boundaryTag = SharedStringUtil.getBytes("--" + hmci.getBoundary());
              byte[] boundaryStart = SharedStringUtil.getBytes("--" + hmci.getBoundary() + Delimiter.CRLF.getValue());
              byte[] boundaryEnd = SharedStringUtil.getBytes("--" + hmci.getBoundary() + "--");

              UByteArrayOutputStream ubaos = hrm.getDataStream();
              // check is boundaryEnd exit
//              if(ubaos.indexOf(0, boundaryEnd) == -1 )
//              {
//                throw new IllegalArgumentException("boundary end " + SharedStringUtil.toString(boundaryEnd) + " not found");
//              }

              //hmci.setBoundary(boundary);

              // we need to parse the payload next
              int index = 0;
              if(log.isEnabled()) log.getLogger().info("index of the end main headers: " + index);

              while((index = ubaos.indexOf(index, boundaryStart)) != -1)
              {
                index += boundaryStart.length;

                if(log.isEnabled()) log.getLogger().info("index of start of a part: "  + index);
                // index point to the first --boundary\r\n
                int endOfSubHeaders = ubaos.indexOf(index +boundaryStart.length, hrm.getDataMark(), Delimiter.CRLFCRLF.getBytes());
                if(endOfSubHeaders == -1)
                { // we have a problem
                  if (log.isEnabled()) log.getLogger().info(index + " end of sub headers missing from index " + index);
                  break;
                }
                else
                  endOfSubHeaders += Delimiter.CRLFCRLF.length();

                // At this level have all the header data
                // we should parse the header

                int endOfSubPart = ubaos.indexOf(endOfSubHeaders, hrm.getDataMark(), boundaryTag);



                if (log.isEnabled()) log.getLogger().info(index + " end of SubHeader: " + endOfSubHeaders + " end of SubPart" + " content\n{" +
                        ubaos.getString(index, endOfSubPart - index - Delimiter.CRLF.length()) + "}");







                int startOfData = endOfSubHeaders;
                int dataLength = endOfSubPart - endOfSubHeaders - Delimiter.CRLF.length();

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
                    NamedValue<String> header = HTTPHeaderParser.parseFullLineHeader(fullHeader);
                    if (header != null) {
                      subPartRawHeaders.build(header);
//                      log.getLogger().info("header: " + fullHeader + " end");
                    }
                  }
                  headerIndex = indexEndOfHeader + Delimiter.CRLF.length();
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

    return hmci;
  };


  private static NamedValue<?> parseMultiPartHeader(HTTPRawMessage hrm, int startIndex, byte[] boundaryStart, byte[] boundaryEnd)
  {
    NamedValue<?> ret = null;

    return ret;
  }

}
