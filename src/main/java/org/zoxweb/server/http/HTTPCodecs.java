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
        if (log.isEnabled()) log.getLogger().info("TRANSFER_CHUNKED entry point\n");
        if (!hrm.isEndOfChunkedContentReached())
            do {
                // parse the data size line in hex
                int index = ubaos.indexOf(hrm.getDataMark(), Delimiter.CRLF.getBytes());
                if (log.isEnabled()) log.getLogger().info("index of chunk marker : " + index);
                if (index == -1)
                    break;
                String hexSize = ubaos.getString(hrm.getDataMark(), index - hrm.getDataMark());
                if (log.isEnabled())
                    log.getLogger().info("dataMark : " + hrm.getDataMark() + " match index: " + index + " ubaos size: " + ubaos.size());
                int chunkSize = SharedUtil.hexToInt(hexSize);
                if (log.isEnabled()) log.getLogger().info("chunk size : " + chunkSize);
                if (chunkSize == 0) {
                    // we have the last chunk

                    hrm.endOfChunksReached();
                    // delete the last 0 chunk and trailing headers
                    ubaos.removeAt(hrm.getDataMark(), ubaos.size() - hrm.getDataMark());

                    // for now NO support for trailer header
                    break;
                }

                if (log.isEnabled())
                    log.getLogger().info("we have a chunk of size " + chunkSize + " raw data buffer size " + ubaos.size() + " dataMark: " + hrm.getDataMark() + " @ " + index);

                if (ubaos.size() >= (index + Delimiter.CRLF.length() + chunkSize + Delimiter.CRLF.length())) {
                    byte r = ubaos.byteAt(index + Delimiter.CRLF.length() + chunkSize);
                    byte n = ubaos.byteAt(index + Delimiter.CRLF.length() + chunkSize + 1);


                    if (r == '\r' && n == '\n') {
                        // we have at least one full chunk

                        // 1. remove the hex-size\r\n
                        ubaos.shiftLeft(index + Delimiter.CRLF.length(), hrm.getDataMark());

                        hrm.incDataMark(chunkSize);
                        // 2. remove the \r\n at the end of binary data
                        ubaos.removeAt(hrm.getDataMark(), Delimiter.CRLF.length());

                        if (log.isEnabled())
                            log.getLogger().info("After the cleanup chunk of size " + chunkSize + " raw data buffer size " + ubaos.size() + " dataMark: " + hrm.getDataMark() + " @ " + index + "\n" + ubaos);

                        continue;
                    } else {
                        // we have a problem
                        log.getLogger().info(index + " The end of the chunked is missing " + (index + Delimiter.CRLF.length() + chunkSize));
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
                                NamedValue<String> location = (NamedValue<String>) subPartRawHeaders.get(HTTPHeader.LOCATION);
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

                                        if (filename != null) {
                                            toAdd.getProperties().build("filename", filename);
                                            if(location != null)
                                                toAdd.getProperties().build(location.getName(), location.getValue());
                                        }

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
        if (hmci != null) {
            switch (hmci.getMethod()) {
                case GET:
                    break;
                default:
                    //boolean continueProcessing = (hmci.isTransferChunked() && hmci.isContentMultipartFormData()) || (hmci.isContentMultipartFormData() && hrm.isMessageComplete());
                    if (!hrm.isMessageComplete()) // this need to change we are doing partial processing
                    {

                        if (SUS.isEmpty(hmci.getBoundary())) {
                            NamedValue<String> multipartFromData = (NamedValue<String>) HTTPHeaderParser.parseHeader(HTTPHeader.CONTENT_TYPE.getName(), hmci.getContentType());

                            String boundary = multipartFromData.getProperties().getValue(HTTPConst.CNP.BOUNDARY);
                            hmci.setBoundary(boundary);
                            if (log.isEnabled()) log.getLogger().info("boundary=" + boundary);
                            hrm.getProperties()
                                    //        --BOUNDARY\r\n
                                    .build(new NVBlob(ProtoMarker.BOUNDARY_START_TAG, SharedStringUtil.getBytes("--" + hmci.getBoundary() + Delimiter.CRLF.getValue())))
                                    //   \r\n\--BOUNDARY\r\n
                                    .build(new NVBlob(ProtoMarker.BOUNDARY_CONTENT_END_TAG, SharedStringUtil.getBytes(Delimiter.CRLF.getValue() + "--" + hmci.getBoundary() + Delimiter.CRLF.getValue())))
                                    //   \r\n\--BOUNDARY--\r\n
                                    .build(new NVBlob(ProtoMarker.BOUNDARY_FINAL_TAG, SharedStringUtil.getBytes(Delimiter.CRLF.getValue() + "--" + hmci.getBoundary() + "--" + Delimiter.CRLF.getValue())));

                        }


                        if (SUS.isNotEmpty(hmci.getBoundary())) {
                            final int safetyBufferLength = ((byte[]) hrm.getProperties().getValue(ProtoMarker.BOUNDARY_FINAL_TAG)).length + 10;

                            // At this stage we hava multipart/form-data content type
                            // we need to start parsing the content
                            // one part at a time
                            NVGenericMap multiPartFullHeader;
                            UByteArrayOutputStream ubaos = hrm.getDataStream();
                            // check the last param if it exists

                            NamedValue<InputStream> pIncompleteParam = (NamedValue<InputStream>) hrm.incompleteParam();

                            // check the last param if it exists
                            if (pIncompleteParam != null) {
                                // find the end of the message
                                int indexEndOfContent = indexEndOfContent(hrm, hrm.getLastProcessedDataIndex());
                                if (indexEndOfContent == -1) {
                                    // safety measure we take the longest tag
                                    int safetyIndex = hrm.getDataMark() - safetyBufferLength;
                                    if (safetyIndex > hrm.getLastProcessedDataIndex()) {
                                        int lastDataMark = hrm.getDataMark();
                                        int lengthToAdd = safetyIndex - hrm.getLastProcessedDataIndex();
                                        UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), hrm.getLastProcessedDataIndex(), lengthToAdd,
                                                () -> {
                                                    if (log.isEnabled())
                                                        log.getLogger().info("*** 2---- CLOSE Incomplete**** " + pIncompleteParam.getName() + " lastDataMark: " + lastDataMark + " " + toString(hrm));

                                                    // Shrink the dataStream() buffer DO NOT TOUCH
                                                    hrm.getDataStream().shiftLeft(hrm.getLastProcessedDataIndex(), 0);
                                                    hrm.setDataMark(hrm.getDataMark() - hrm.getLastProcessedDataIndex());
                                                    hrm.setLastProcessedDataIndex(0);
                                                });
                                        pIncompleteParam.setValue(is);
                                        hrm.setLastProcessedDataIndex(safetyIndex);
                                        return hmci;
                                    } else {
                                        // nothing to do
                                        return hmci;
                                    }

                                } else {
                                    // we have reached the end
                                    if (log.isEnabled())
                                        log.getLogger().info("We a have the end of the partial file getLastProcessedDataIndex " + hrm.getLastProcessedDataIndex() + " indexEndOfContent: " + indexEndOfContent);
                                    UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), hrm.getLastProcessedDataIndex(), indexEndOfContent - hrm.getLastProcessedDataIndex());
                                    pIncompleteParam.setValue(is);
                                    pIncompleteParam.getProperties().build(new NVBoolean(ProtoMarker.LAST_CHUNK, true));
                                    hrm.incompleteParam(null);

                                    hrm.setLastProcessedDataIndex(indexEndOfContent);

                                }
                            }

                            if (log.isEnabled())
                                log.getLogger().info("Before while lastParsedDataIndex: " + hrm.getLastProcessedDataIndex());
                            while ((multiPartFullHeader = parseCompleteMultiPartHeaders(hrm, hrm.getLastProcessedDataIndex())) != null) {
                                // we have a full header
                                NamedValue<?> contentDisposition = multiPartFullHeader.getNV(HTTPHeader.CONTENT_DISPOSITION);
                                NamedValue<?> contentType = multiPartFullHeader.getNV(HTTPHeader.CONTENT_TYPE);
                                NamedValue<?> contentLength = multiPartFullHeader.getNV(HTTPHeader.CONTENT_LENGTH);
                                NamedValue<?> location = multiPartFullHeader.getNV(HTTPHeader.LOCATION);
                                boolean isAFile = multiPartFullHeader.getValue(ProtoMarker.IS_FILE);

                                int dataContentStartIndex = multiPartFullHeader.getValue(ProtoMarker.SUB_CONTENT_START_INDEX);
                                int dataContentEndIndex = multiPartFullHeader.getValue(ProtoMarker.SUB_CONTENT_END_INDEX);

                                String fieldName = contentDisposition.getProperties().getValue("name");

                                if (log.isEnabled())
                                    log.getLogger().info(hmci.getBoundary() + " DataMark: " + hrm.getDataMark() + " dataContentStartIndex: " + dataContentEndIndex + " dataContentEndIndex: " + dataContentEndIndex + " isAFile: " + isAFile);


                                if (isAFile) {

                                    if (dataContentEndIndex == -1) {
                                        // we have a partial file content
                                        int safetyIndex = hrm.getDataMark() - safetyBufferLength;
                                        if (safetyIndex > dataContentStartIndex) {
                                            NamedValue<InputStream> paramFileToAdd = new NamedValue<>();
                                            paramFileToAdd.setName(fieldName);
                                            paramFileToAdd.getProperties().build(contentDisposition.getProperties().get("filename"))
                                                    .build(contentType.getName(), "" + contentType.getValue());
                                            if (contentLength != null) {
                                                paramFileToAdd.getProperties().build(new NVLong(contentLength.getName(), (long) contentLength.getValue()));
                                            }
                                            paramFileToAdd.getProperties().build(new NVBoolean(ProtoMarker.LAST_CHUNK, false));
                                            if(location != null && location.getValue() !=null)
                                                paramFileToAdd.getProperties().build(location.getName(), ""+location.getValue());

                                            hmci.getParameters().add(paramFileToAdd);
                                            int lastDataMark = hrm.getDataMark();
                                            UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), dataContentStartIndex, safetyIndex - dataContentStartIndex
                                                    , () -> {

                                                if (log.isEnabled())
                                                    log.getLogger().info("*** 1---CLOSE first incomplete **** " + paramFileToAdd.getName() + " lastDataMark: " + lastDataMark + " " + toString(hrm));

                                                // Shrink the dataStream() buffer DO NOT TOUCH
                                                hrm.getDataStream().shiftLeft(hrm.getLastProcessedDataIndex(), 0);
                                                hrm.setDataMark(hrm.getDataMark() - hrm.getLastProcessedDataIndex());
                                                hrm.setLastProcessedDataIndex(0);

                                            });
                                            hrm.setLastProcessedDataIndex(safetyIndex);
                                            paramFileToAdd.setValue(is);
                                            hrm.incompleteParam(paramFileToAdd);
                                        }
                                        return hmci;

                                    } else {
                                        // We have a complete file with it s content
                                        NamedValue<InputStream> paramFileToAdd = new NamedValue<>();
                                        paramFileToAdd.setName(fieldName);
                                        paramFileToAdd.getProperties().build(contentDisposition.getProperties().get("filename"))
                                                .build(contentType.getName(), "" + contentType.getValue());
                                        if (contentLength != null) {
                                            paramFileToAdd.getProperties().build(new NVLong(contentLength.getName(), (long) contentLength.getValue()));
                                        }
                                        paramFileToAdd.getProperties().build(new NVBoolean(ProtoMarker.LAST_CHUNK, true));
                                        if(location != null && location.getValue() !=null)
                                            paramFileToAdd.getProperties().build(location.getName(), ""+location.getValue());

                                        hmci.getParameters().add(paramFileToAdd);
                                        UByteArrayInputStream is = new UByteArrayInputStream(ubaos.getInternalBuffer(), dataContentStartIndex, dataContentEndIndex - dataContentStartIndex);
                                        paramFileToAdd.setValue(is);
                                        hmci.getParameters().add(paramFileToAdd);
                                        if (log.isEnabled())
                                            log.getLogger().info("adding completed file: " + paramFileToAdd);
                                        // update index here

                                    }

                                } else {
                                    // it is not a file
                                    String value = ubaos.getString(dataContentStartIndex, dataContentEndIndex - dataContentStartIndex);
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

                                if (dataContentEndIndex == -1)
                                    break;


//                                int indexEndOfBoundaryFinalTag = ubaos.indexOf(dataContentEndIndex, hrm.getDataMark(), boundaryFinalTag);
//
//                                hrm.setLastParsedDataIndex(dataContentEndIndex);
////                                if (indexEndOfBoundaryFinalTag != -1) {
////                                    hrm.endOfContentReached();
////                                    break;
////                                }


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
        }

        return hmci;
    };


    private static String toString(HTTPRawMessage hrm) {
        return "++DataMark: " + hrm.getDataMark() + " LastProcessedDataIndex: " + hrm.getLastProcessedDataIndex() + " UBAOS Size: " + hrm.getDataStream().size() + " --";
    }

    private static int indexEndOfContent(HTTPRawMessage hrm, int startIndex) {
        if (startIndex > hrm.getDataMark())
            return -1;

        UByteArrayOutputStream ubaos = hrm.getDataStream();
        byte[] boundaryContentEndMarker = hrm.getProperties().getValue(ProtoMarker.BOUNDARY_CONTENT_END_TAG);
        byte[] boundaryEndMarker = hrm.getProperties().getValue(ProtoMarker.BOUNDARY_FINAL_TAG);

        int indexEndOfContent = ubaos.indexOf(startIndex, hrm.getDataMark(), boundaryContentEndMarker);
        int indexEndOfFullContent = ubaos.indexOf(startIndex, hrm.getDataMark(), boundaryEndMarker);

        if (indexEndOfContent == -1 && indexEndOfFullContent != -1) {
            if (log.isEnabled())
                log.getLogger().info("indexEndOfContent: " + indexEndOfContent + " indexEndOfFullContent: " + indexEndOfFullContent);
            indexEndOfContent = indexEndOfFullContent;
            hrm.endOfContentReached();
        }
        return indexEndOfContent;

    }

    private static NVGenericMap parseCompleteMultiPartHeaders(HTTPRawMessage hrm, int startIndex) {

        // 1 we need to find the BOUNDARY_START
        UByteArrayOutputStream ubaos = hrm.getDataStream();
        if (log.isEnabled()) log.getLogger().info("StartIndex: " + startIndex);
        byte[] boundaryStartMarker = hrm.getProperties().getValue(ProtoMarker.BOUNDARY_START_TAG);


        int indexOfBoundaryStart = ubaos.indexOf(startIndex, hrm.getDataMark(), boundaryStartMarker);
        // 2 the end of the headers \r\n\r\n
        if (indexOfBoundaryStart != -1) {
            int endOfSubHeadersIndex = ubaos.indexOf(indexOfBoundaryStart + boundaryStartMarker.length, hrm.getDataMark(), Delimiter.CRLFCRLF.getBytes());
//            if (log.isEnabled())
//                log.getLogger().info("startIndex, indexOfBoundaryStart, endOfSubHeadersIndex, indexEndOfContent, indexOfFinalTag: " + SharedUtil.toCanonicalID(',', startIndex, indexOfBoundaryStart, endOfSubHeadersIndex, indexEndOfContent, indexOfFinalTag));


            if (endOfSubHeadersIndex != -1) {
                int indexEndOfContent = indexEndOfContent(hrm, indexOfBoundaryStart + boundaryStartMarker.length);
                // we have full headers
                // we can parse of the part headers
                NVGenericMap ret = new NVGenericMap();
                int index = indexOfBoundaryStart + boundaryStartMarker.length;
                do {
                    int endOfHeaderLineIndex = ubaos.indexOf(index, hrm.getDataMark(), Delimiter.CRLF.getBytes());
                    if (endOfHeaderLineIndex != -1) {
                        // parse the whole header line
                        String wholeHeaderLine = ubaos.getString(index, endOfHeaderLineIndex - index);
                        if (log.isEnabled()) log.getLogger().info("Multipart header line " + wholeHeaderLine);
                        NamedValue<?> nv = HTTPHeaderParser.parseFullHeaderLine(wholeHeaderLine);
                        ret.add(nv);
                        if (endOfSubHeadersIndex == endOfHeaderLineIndex) {
                            index = -1;
                            NVInt startOfContentNV = new NVInt(ProtoMarker.SUB_CONTENT_START_INDEX, endOfSubHeadersIndex + Delimiter.CRLFCRLF.length());
                            ret.build(startOfContentNV).build(new NVInt(ProtoMarker.SUB_CONTENT_END_INDEX, indexEndOfContent));
                            if (log.isEnabled()) log.getLogger().info(startOfContentNV.toString());
                        } else
                            index = endOfHeaderLineIndex + Delimiter.CRLF.length();
                        if (log.isEnabled()) log.getLogger().info("index: " + index + " ret: " + ret);

                    }

                } while (index != -1);

                NamedValue<?> contentDisposition = ret.getNV(HTTPHeader.CONTENT_DISPOSITION);
                boolean isAFile = contentDisposition.getProperties().get("filename") != null;
                ret.add(new NVBoolean(ProtoMarker.IS_FILE, isAFile));

                // check if we have a full multipart content
                if (indexEndOfContent == -1) {
                    if (isAFile) {
                        // we have a file with end unknown
                        //hrm.setLastParsedDataIndex(endOfSubHeadersIndex + Delimiter.CRLFCRLF.length());
                        return ret;
                    }

                } else {
                    // we have a full parameter with its content
                    hrm.setLastProcessedDataIndex(indexEndOfContent + Delimiter.CRLF.length());
                    return ret;
                }
            }
        }

        // 3 try to find the next BOUNDARY_START
        //   If not found we have partial content
        //   If found we have a full content
        return null;
    }

}
