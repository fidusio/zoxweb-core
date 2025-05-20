package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayInputStream;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.protocol.ProtoMarker;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

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
        switch (hmci.getMethod()) {
            case GET:
                HTTPUtil.parseQuery(hmci.getParameters().asArrayValuesString(), hmci.getURI(), true);
                return hmci;
            default:
                if (hrm.isMessageComplete()) {
                    if (HTTPMediaType.lookup(hmci.getContentType()) == HTTPMediaType.APPLICATION_WWW_URL_ENC) {
                        HTTPUtil.parseQuery(hmci.getParameters().asArrayValuesString(), hrm.getDataStream().getString(0), false);
                        return hmci;
                    }
                }
        }

        return null;
    };


    public static final DataDecoder<HTTPRawMessage, HTTPRawMessage> TRANSFER_CHUNKED = (hrm) ->
    {
        // the headers are already parsed
        // we need to parse the body as chunks
        // hex-size\r\n
        // binary-data\r\n
        //
        UByteArrayOutputStream ubaos = hrm.getDataStream();
        if (log.isEnabled()) log.getLogger().info("TRANSFER_CHUNKED entry point");

        do {
            // parse the data size line in hex
            int index = ubaos.indexOf(hrm.getDataMark(), Delimiter.CRLF.getBytes());
            if (index == -1)
                break;
            String hexSize = ubaos.getString(hrm.getDataMark(), index - hrm.getDataMark());
            int chunkSize = SharedUtil.hexToInt(hexSize);
            if (chunkSize == 0) {
                // we have the last chunk
                hrm.endOfContentReached();
                // delete the last 0 chunk and trailing headers
                ubaos.removeAt(hrm.getDataMark(), ubaos.size() - hrm.getDataMark());

                // for now NO support for trailer header
                break;
            }

            if (log.isEnabled())
                log.getLogger().info("we have a chunk of size " + chunkSize + " raw data buffer size " + ubaos.size() + " dataMark: " + hrm.getDataMark());

            if (ubaos.size() >= (index + Delimiter.CRLF.length() + chunkSize + Delimiter.CRLF.length())) {


                if (ubaos.byteAt(index + Delimiter.CRLF.length() + chunkSize) == '\r' &&
                        ubaos.byteAt(index + Delimiter.CRLF.length() + chunkSize + 1) == '\n') {
                    // we have at least one full chunk

                    // 1. remove the hex-size\r\n
                    ubaos.shiftLeft(index + Delimiter.CRLF.length(), hrm.getDataMark());

                    // 2. remove the \r\n at the binary data
                    ubaos.removeAt(hrm.getDataMark() + chunkSize, Delimiter.CRLF.length());
                    hrm.incDataMark(chunkSize);
                    if (log.isEnabled())
                        log.getLogger().info("we have a at least one FULL chunk dataMark " + hrm.getDataMark() + " raw data buffer size " + ubaos.size());
                    continue;
                } else {
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

        } while (true);


        return hrm;
    };


    public static final DataDecoder<HTTPRawMessage, HTTPMessageConfigInterface> MULTIPART_FORM_DATA = (hrm) ->
    {

        HTTPMessageConfigInterface hmci = hrm.getHTTPMessageConfig();
        switch (hmci.getMethod()) {
            case GET:
                break;
            default:
                if (hrm.isMessageComplete()) {
                    if (HTTPMediaType.lookup(hmci.getContentType()) == HTTPMediaType.MULTIPART_FORM_DATA) {
                        NamedValue<String> multipartFromData = (NamedValue<String>) HTTPHeaderParser.parseHeader(HTTPHeader.CONTENT_TYPE.getName(), hmci.getContentType());


                        String boundary = multipartFromData.getProperties().getValue(HTTPConst.CNP.BOUNDARY);
                        if (log.isEnabled()) log.getLogger().info("boundary=" + boundary);


                        if (SUS.isNotEmpty(boundary)) {

                            byte[] boundaryTag = SharedStringUtil.getBytes("--" + boundary);
                            byte[] boundaryStart = SharedStringUtil.getBytes("--" + boundary + Delimiter.CRLF.getValue());
                            byte[] boundaryEnd = SharedStringUtil.getBytes("--" + boundary + "--" + Delimiter.CRLF.getValue());

                            UByteArrayOutputStream ubaos = hrm.getDataStream();
                            // check is boundaryEnd exit
                            if (ubaos.indexOf(0, boundaryEnd) == -1) {
                                throw new IllegalArgumentException("boundary end " + SharedStringUtil.toString(boundaryEnd).trim() + " not found");
                            }

                            hmci.setBoundary(boundary);

                            // we need to parse the payload next
                            int index = 0;
                            if (log.isEnabled()) log.getLogger().info("index of the end main headers: " + index);

                            while ((index = ubaos.indexOf(index, boundaryStart)) != -1) {
                                index += boundaryStart.length;

                                if (log.isEnabled()) log.getLogger().info("index of start of a part: " + index);
                                // index point to the first --boundary\r\n
                                int endOfSubHeaders = ubaos.indexOf(index, Delimiter.CRLFCRLF.getBytes());
                                if (endOfSubHeaders == -1) { // we have a problem
                                    log.getLogger().info(index + " end of sub headers missing from index " + index);
                                } else
                                    endOfSubHeaders += Delimiter.CRLFCRLF.length();


                                int endOfSubPart = ubaos.indexOf(endOfSubHeaders, boundaryTag);


                                if (log.isEnabled())
                                    log.getLogger().info(index + " end of SubHeader: " + endOfSubHeaders + " end of SubPart" + " content\n{" +
                                            ubaos.getString(index, endOfSubPart - index - Delimiter.CRLF.length()) + "}");


                                int startOfData = endOfSubHeaders;
                                int dataLength = endOfSubPart - endOfSubHeaders - Delimiter.CRLF.length();

                                if (log.isEnabled())
                                    log.getLogger().info("data: \"" + ubaos.getString(startOfData, dataLength) + "\"");


                                // index points to the next sub-http-header
                                // parse one sub part
                                int headerIndex = index;
                                //Map<String, NamedValue<String>> subPartRawHeaders = new HashMap<>();
                                NVGenericMap subPartRawHeaders = new NVGenericMap();

                                // header parsing
                                while (headerIndex != -1 && headerIndex < endOfSubPart) {

                                    int indexEndOfHeader = ubaos.indexOf(headerIndex, Delimiter.CRLF.getBytes());
                                    if (indexEndOfHeader != -1 && indexEndOfHeader < endOfSubHeaders) {
                                        String fullHeader = ubaos.getString(headerIndex, indexEndOfHeader - headerIndex);
                                        if (SUS.isNotEmpty(fullHeader)) {
                                            NamedValue<?> header = HTTPHeaderParser.parseFullHeaderLine(fullHeader);
                                            if (header != null)
                                                subPartRawHeaders.add(header);
                                        }
                                    }
                                    headerIndex = indexEndOfHeader + Delimiter.CRLF.length();
                                }

                                if (log.isEnabled()) log.getLogger().info("headers:\n" + subPartRawHeaders);


                                NamedValue<String> contentDisposition = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.CONTENT_DISPOSITION);
                                NamedValue<String> contentType = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.CONTENT_TYPE);
                                String name = null;
                                String filename = null;
                                String mediaType = null;
                                InputStream is = null;
                                //List<BytesArray> lBytesArray = new ArrayList<>();
                                if (contentDisposition != null) {
                                    name = contentDisposition.getProperties().getValue("name");
                                    filename = contentDisposition.getProperties().getValue("filename");
                                }
                                if (contentType != null) {
                                    mediaType = contentType.getValue();
                                }

                                if (filename != null) {
                                    // we covert to input stream
                                    is = new UByteArrayInputStream(ubaos.getInternalBuffer(), startOfData, dataLength);
                                }

                                if (log.isEnabled())
                                    log.getLogger().info("name=" + name + " filename=" + filename + " mediaType=" + mediaType + " is=" + is);

                                if (name != null) {
                                    if (is != null && mediaType != null && HTTPMediaType.lookup(mediaType) != HTTPMediaType.APPLICATION_JSON) {
                                        NamedValue<InputStream> toAdd = new NamedValue<>(name, is);

                                        if (filename != null)
                                            toAdd.getProperties().build("filename", filename);

                                        toAdd.getProperties().build("media-type", mediaType);

                                        toAdd.getProperties().build(new NVLong("length", dataLength));
                                        if (log.isEnabled()) log.getLogger().info("toAdd: " + toAdd);
                                        // must add it a t the end

                                        hmci.getParameters().add(toAdd);
                                    } else {
                                        String value = ubaos.getString(startOfData, dataLength);
                                        if (mediaType != null && HTTPMediaType.lookup(mediaType) == HTTPMediaType.APPLICATION_JSON) {
                                            NVGenericMap toAdd = GSONUtil.fromJSONDefault(ubaos.getString(startOfData, dataLength), NVGenericMap.class);
                                            toAdd.setName(name);
                                            hmci.getParameters().build(toAdd);
                                        } else {
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
        if (log.isEnabled()) log.getLogger().info("MULTIPART_FORM_DATA_CHUNKED entry point");
        switch (hmci.getMethod()) {
            case GET:
                break;
            default:
                boolean canContinue = !hmci.isTransferChunked() || hrm.getDataMark() > 0;
                if (hmci.isContentMultipartFormData() && canContinue) // this need to change we are doing partial processing
                {

                    if (SUS.isEmpty(hmci.getBoundary())) {
                        NamedValue<String> multipartFromData = (NamedValue<String>) HTTPHeaderParser.parseHeader(HTTPHeader.CONTENT_TYPE.getName(), hmci.getContentType());

                        String boundary = multipartFromData.getProperties().getValue(HTTPConst.CNP.BOUNDARY);
                        hmci.setBoundary(boundary);
                        if (log.isEnabled()) log.getLogger().info("boundary=" + boundary);
                        hrm.getProperties()
                                .build(new NVBlob(ProtoMarker.BOUNDARY_TAG, SharedStringUtil.getBytes("--" + hmci.getBoundary())))
//                                .build(new NVBlob(ProtoMarker.BOUNDARY_CONTENT_END, SharedStringUtil.getBytes(Delimiter.CRLF.getValue() + "--" + hmci.getBoundary())))
                                .build(new NVBlob(ProtoMarker.BOUNDARY_START, SharedStringUtil.getBytes("--" + hmci.getBoundary() + Delimiter.CRLF.getValue())))
                                .build(new NVBlob(ProtoMarker.BOUNDARY_END, SharedStringUtil.getBytes("--" + hmci.getBoundary() + "--" + Delimiter.CRLF.getValue())));
                    }


                    if (SUS.isNotEmpty(hmci.getBoundary())) {
                        byte[] boundaryTag = hrm.getProperties().getValue(ProtoMarker.BOUNDARY_TAG);

                        // At this stage we hava multipart/form-data content type
                        // we need to start parsing the content
                        // one part at a time
                        NVGenericMap multiPartFullHeader;
                        UByteArrayOutputStream ubaos = hrm.getDataStream();
                        // check the last param if it exists

                        NamedValue<InputStream> pIncompleteParam = (NamedValue<InputStream>) hrm.incompleteParam();
                        int index;
                        // check the last param if it exists
                        if (pIncompleteParam != null) {
                            // find the end of the message
                            index = ubaos.indexOf(hrm.getLastParsedDataIndex(), hrm.getDataMark(), hrm.getProperties().getValue(ProtoMarker.BOUNDARY_START));
                            if (index == -1) {
                                // safety measure
                                int safetyIndex = hrm.getDataMark() - (Delimiter.CRLF.length() + boundaryTag.length);
                                if (safetyIndex > hrm.getLastParsedDataIndex()) {
                                    UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), hrm.getLastParsedDataIndex(), safetyIndex - hrm.getLastParsedDataIndex());
//                                            ()->{
//                                        hrm.getDataStream().shiftLeft(safetyIndex, 0);
//                                        hrm.setLastParsedDataIndex(0);
//                                        hrm.setDataMark(hrm.getDataMark() - safetyIndex);
//                                    });
                                    pIncompleteParam.setValue(is);
                                    hrm.setLastParsedDataIndex(safetyIndex);
                                } else
                                    pIncompleteParam.setValue(new UByteArrayInputStream(Const.EMPTY_BYTE_ARRAY));
                                return hmci;
                            } else {
                                UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), hrm.getLastParsedDataIndex(), index - hrm.getLastParsedDataIndex() - Delimiter.CRLF.length());
                                pIncompleteParam.setValue(is);
                                pIncompleteParam.getProperties().build(new NVBoolean(ProtoMarker.LAST_CHUNK, true));
                                hrm.incompleteParam(null);
//                                if(ubaos.indexOf(0, hrm.getDataMark(), hrm.getProperties().getValue(ProtoMarker.BOUNDARY_START)) != -1)
//                                    // we are done
//                                    return hmci;

                                hrm.setLastParsedDataIndex(index);
//                                index += ((byte[])hrm.getProperties().getValue(ProtoMarker.BOUNDARY_START)).length;

                            }
                        }

                        if (log.isEnabled())
                            log.getLogger().info("Before while lastParsedDataIndex: " + hrm.getLastParsedDataIndex());
                        while ((multiPartFullHeader = parseCompleteMultiPartHeaders(hrm, hrm.getLastParsedDataIndex())) != null) {
                            // we have a full header
                            NamedValue<?> contentDisposition = multiPartFullHeader.getNV(HTTPHeader.CONTENT_DISPOSITION);
                            NamedValue<?> contentType = multiPartFullHeader.getNV(HTTPHeader.CONTENT_TYPE);
                            NamedValue<?> contentLength = multiPartFullHeader.getNV(HTTPHeader.CONTENT_LENGTH);
                            boolean isAFile = contentDisposition.getProperties().get("filename") != null;
                            int dataContentStartIndex = multiPartFullHeader.getValue(ProtoMarker.SUB_CONTENT_START_INDEX);

                            int indexEndOfPart = ubaos.indexOf(dataContentStartIndex, hrm.getDataMark(), boundaryTag);

                            String fieldName = contentDisposition.getProperties().getValue("name");

                            if (log.isEnabled())
                                log.getLogger().info(new String(boundaryTag) + " DataMark: " + hrm.getDataMark() + " indexEndOfPart: " + indexEndOfPart + " dataContentStartIndex: " + dataContentStartIndex + " isAFile: " + isAFile);


                            if (isAFile) {
                                NamedValue<InputStream> paramFileToAdd = new NamedValue<>();
                                paramFileToAdd.setName(fieldName);
                                paramFileToAdd.getProperties().build(contentDisposition.getProperties().get("filename"))
                                        .build(contentType.getName(), "" + contentType.getValue());
                                if (contentLength != null) {
                                    paramFileToAdd.getProperties().build(new NVLong(contentLength.getName(), (long) contentLength.getValue()));
                                }
                                paramFileToAdd.getProperties().build(new NVBoolean(ProtoMarker.LAST_CHUNK, indexEndOfPart != -1));
                                if (indexEndOfPart == -1) {

                                    int safetyIndex = hrm.getDataMark() - (Delimiter.CRLF.length() + boundaryTag.length);
                                    if (safetyIndex > dataContentStartIndex) {
                                        UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), dataContentStartIndex, safetyIndex - dataContentStartIndex
                                        , ()->{
                                            if (log.isEnabled())
                                                log.getLogger().info("***CLOSE**** " + paramFileToAdd.getName());
                                            hrm.getDataStream().shiftLeft(safetyIndex, 0);
                                            hrm.setLastParsedDataIndex(0);
                                            hrm.setDataMark(hrm.getDataMark() - safetyIndex);
                                        });
                                        paramFileToAdd.setValue(is);
                                        hrm.setLastParsedDataIndex(safetyIndex);
                                    } else {
                                        paramFileToAdd.setValue(UByteArrayInputStream.EMPTY_INPUT_STREAM);
                                        hrm.setLastParsedDataIndex(dataContentStartIndex);
                                    }

                                    // we have partial file data
                                    hrm.incompleteParam(paramFileToAdd);
                                    hmci.getParameters().add(paramFileToAdd);

                                    if (log.isEnabled())
                                        log.getLogger().info("adding incomplete file: " + paramFileToAdd);
                                } else {
                                    // the file is completed
                                    UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), dataContentStartIndex, indexEndOfPart - dataContentStartIndex - Delimiter.CRLF.length());
                                    paramFileToAdd.setValue(is);
                                    if (log.isEnabled())
                                        log.getLogger().info("adding completed file: " + paramFileToAdd);
                                    // update index here
                                }

                                hmci.getParameters().add(paramFileToAdd);
                            } else if (indexEndOfPart != -1) {
                                String value = ubaos.getString(dataContentStartIndex, indexEndOfPart - dataContentStartIndex - Delimiter.CRLF.length());
                                if (contentType != null && HTTPMediaType.lookup((String) contentType.getValue()) == HTTPMediaType.APPLICATION_JSON) {
                                    NVGenericMap nvgm = GSONUtil.fromJSONDefault(value, NVGenericMap.class);
                                    nvgm.setName(fieldName);
                                    hmci.getParameters().build(nvgm);
                                    if (log.isEnabled()) log.getLogger().info("adding nvmg: " + nvgm);

                                    // update index here
                                } else {
                                    NVPair nvp = new NVPair(fieldName, value);
                                    hmci.getParameters().build(nvp);
                                    if (log.isEnabled()) log.getLogger().info("adding nvp: " + nvp);
                                }

                            }

                            if (indexEndOfPart == -1)
                                break;


                            hrm.setLastParsedDataIndex(indexEndOfPart);

                            // continue processing


                            // check

                            // we need to update the index;

                            // nothing to do

                            // we don't have a file


                            // a field It must be complete with value


                            // analyse the content of the header and check if the content is partial or complete
                        }
                    }
                }
        }

        return hmci;
    };


    private static NVGenericMap parseCompleteMultiPartHeaders(HTTPRawMessage hrm, int startIndex) {
        NVGenericMap ret = null;
        // 1 we need to find the BOUNDARY_START
        UByteArrayOutputStream ubaos = hrm.getDataStream();
        if (log.isEnabled()) log.getLogger().info("StartIndex: " + startIndex);
        int indexOfBoundaryStart = ubaos.indexOf(startIndex, hrm.getDataMark(), hrm.getProperties().getValue(ProtoMarker.BOUNDARY_START));
        // 2 the end of the headers \r\n\r\n
        if (indexOfBoundaryStart != -1) {
            int endOfHeaderIndex = ubaos.indexOf(startIndex, hrm.getDataMark(), Delimiter.CRLFCRLF.getBytes());
            if (endOfHeaderIndex != -1) {
                // we have full headers
                // we can parse of the part headers
                ret = new NVGenericMap();
                int index = startIndex + ((byte[]) hrm.getProperties().getValue(ProtoMarker.BOUNDARY_START)).length;
                do {
                    int endOfHeaderLineIndex = ubaos.indexOf(index, hrm.getDataMark(), Delimiter.CRLF.getBytes());
                    if (endOfHeaderLineIndex != -1) {
                        // parse the whole header line
                        String wholeHeaderLine = ubaos.getString(index, endOfHeaderLineIndex - index);
                        if (log.isEnabled()) log.getLogger().info("Multipart header line " + wholeHeaderLine);
                        NamedValue<?> nv = HTTPHeaderParser.parseFullHeaderLine(wholeHeaderLine);
                        ret.add(nv);
                        if (endOfHeaderIndex == endOfHeaderLineIndex) {

                            index = -1;
                            NVInt nvint = new NVInt(ProtoMarker.SUB_CONTENT_START_INDEX, endOfHeaderIndex + Delimiter.CRLFCRLF.length());
                            ret.build(nvint);
                            if (log.isEnabled()) log.getLogger().info("" + nvint);
                        } else
                            index = endOfHeaderLineIndex + Delimiter.CRLF.length();

                        if (log.isEnabled()) log.getLogger().info("index: " + index + " ret: " + ret);

                    }

                } while (index != -1);
            }
        } else if (ubaos.indexOf(startIndex, hrm.getDataMark(), hrm.getProperties().getValue(ProtoMarker.BOUNDARY_END)) != -1) {
            // we are done
            hrm.endOfContentReached();
        }

        // 3 try to find the next BOUNDARY_START
        //   If not found we have partial content
        //   If found we have a full content
        return ret;
    }

}
