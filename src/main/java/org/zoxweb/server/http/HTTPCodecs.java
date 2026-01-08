/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayInputStream;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.protocol.ProtoMarker;
import org.zoxweb.shared.util.*;

import java.io.InputStream;

/**
 * A collection of {@link DataDecoder} implementations for parsing various HTTP content formats.
 * <p>
 * This utility class provides decoders for:
 * </p>
 * <ul>
 *     <li><b>JSON decoding:</b> Convert bytes or HTTP responses to {@link NVGenericMap} or {@link NVEntity}</li>
 *     <li><b>URL-encoded forms:</b> Parse {@code application/x-www-form-urlencoded} content</li>
 *     <li><b>Chunked transfer:</b> Process HTTP chunked transfer encoding</li>
 *     <li><b>Multipart forms:</b> Parse {@code multipart/form-data} for file uploads</li>
 *     <li><b>Streaming multipart:</b> Handle chunked multipart data for large file uploads</li>
 * </ul>
 *
 * <h3>Decoder Types</h3>
 * <table border="1">
 *     <tr><th>Decoder</th><th>Input</th><th>Output</th><th>Description</th></tr>
 *     <tr><td>{@link #BytesToNVGM}</td><td>byte[]</td><td>NVGenericMap</td><td>JSON bytes to map</td></tr>
 *     <tr><td>{@link #HRDToNVGM}</td><td>HTTPResponseData</td><td>NVGenericMap</td><td>HTTP response to map</td></tr>
 *     <tr><td>{@link #HRDToNVE}</td><td>HTTPResponseData</td><td>NVEntity</td><td>HTTP response to entity</td></tr>
 *     <tr><td>{@link #WWW_URL_ENC}</td><td>HTTPRawMessage</td><td>HTTPMessageConfigInterface</td><td>URL-encoded form</td></tr>
 *     <tr><td>{@link #TRANSFER_CHUNKED}</td><td>HTTPRawMessage</td><td>HTTPRawMessage</td><td>Chunked transfer</td></tr>
 *     <tr><td>{@link #MULTIPART_FORM_DATA}</td><td>HTTPRawMessage</td><td>HTTPMessageConfigInterface</td><td>Multipart form</td></tr>
 *     <tr><td>{@link #MULTIPART_FORM_DATA_CHUNKED}</td><td>HTTPRawMessage</td><td>HTTPMessageConfigInterface</td><td>Streaming multipart</td></tr>
 * </table>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Decode JSON response to NVGenericMap
 * HTTPResponseData response = httpCall.execute();
 * NVGenericMap result = HTTPCodecs.HRDToNVGM.decode(response);
 *
 * // Process chunked transfer encoding
 * HTTPRawMessage rawMessage = ...;
 * HTTPCodecs.TRANSFER_CHUNKED.decode(rawMessage);
 * }</pre>
 *
 * @see DataDecoder
 * @see HTTPRawMessage
 * @see HTTPResponseData
 * @see NVGenericMap
 */
public final class HTTPCodecs {

    /** Logger for debugging codec operations */
    public final static LogWrapper log = new LogWrapper(HTTPCodecs.class).setEnabled(false);

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private HTTPCodecs() {
    }

    /**
     * Decoder that converts a byte array containing JSON to an {@link NVGenericMap}.
     * <p>
     * Uses Base64 DEFAULT type for any embedded binary data.
     * </p>
     */
    public static final DataDecoder<byte[], NVGenericMap> BytesToNVGM = (input) -> GSONUtil.fromJSONDefault(input, NVGenericMap.class);

    /**
     * Decoder that converts {@link HTTPResponseData} JSON content to an {@link NVGenericMap}.
     */
    public static final DataDecoder<HTTPResponseData, NVGenericMap> HRDToNVGM = (input) -> GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class);

    /**
     * Decoder that converts {@link HTTPResponseData} JSON content to an {@link NVGenericMap}
     * with pretty-print and array support enabled.
     */
    public static final DataDecoder<HTTPResponseData, NVGenericMap> NVGMDecoderPAS = (input) -> GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMap.class, true);

    /**
     * Decoder that converts {@link HTTPResponseData} JSON array content to an {@link NVGenericMapList}.
     */
    public static final DataDecoder<HTTPResponseData, NVGenericMapList> HRDToNVGMList = (input) -> GSONUtil.fromJSONDefault(input.getDataAsString(), NVGenericMapList.class);

    /**
     * Decoder that converts a byte array containing JSON to an {@link NVEntity}.
     * <p>
     * Uses {@link GSONUtil#fromJSON(byte[])} for deserialization.
     * </p>
     */
    public static final DataDecoder<byte[], NVEntity> BytesToNVE = GSONUtil::fromJSON;

    /**
     * Decoder that converts {@link HTTPResponseData} JSON content to an {@link NVEntity}.
     */
    public static final DataDecoder<HTTPResponseData, NVEntity> HRDToNVE = (input) -> GSONUtil.fromJSON(input.getData());

    /**
     * Decoder for {@code application/x-www-form-urlencoded} content.
     * <p>
     * Parses URL-encoded form data from HTTP requests:
     * </p>
     * <ul>
     *     <li><b>GET requests:</b> Extracts parameters from the query string in the URI</li>
     *     <li><b>POST/PUT requests:</b> Extracts parameters from the request body when
     *         Content-Type is {@code application/x-www-form-urlencoded}</li>
     * </ul>
     *
     * <h4>URL-Encoded Format</h4>
     * <pre>
     * name1=value1&amp;name2=value2&amp;name3=encoded%20value
     * </pre>
     *
     * @return the {@link HTTPMessageConfigInterface} with parsed parameters, or null if not applicable
     */
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

    /**
     * Decoder for HTTP chunked transfer encoding (Transfer-Encoding: chunked).
     * <p>
     * Processes the chunked transfer encoding format where the message body is sent in
     * a series of chunks, each preceded by its size in hexadecimal. This decoder
     * reassembles the chunks into a contiguous data stream.
     * </p>
     *
     * <h4>Chunked Format</h4>
     * <pre>
     * [hex-size]\r\n
     * [binary-data]\r\n
     * [hex-size]\r\n
     * [binary-data]\r\n
     * 0\r\n
     * \r\n
     * </pre>
     *
     * <h4>Processing Steps</h4>
     * <ol>
     *     <li>Read chunk size in hexadecimal followed by CRLF</li>
     *     <li>Read chunk data of the specified size followed by CRLF</li>
     *     <li>Remove chunk metadata, keeping only the data</li>
     *     <li>Repeat until a zero-size chunk (0\r\n) is encountered</li>
     * </ol>
     *
     * <p>
     * The decoder modifies the {@link HTTPRawMessage} data stream in-place,
     * removing chunk size markers and preserving only the actual content.
     * </p>
     *
     * @return the same {@link HTTPRawMessage} with reassembled content
     */
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

    /**
     * Decoder for {@code multipart/form-data} content (file uploads).
     * <p>
     * Parses multipart form data as defined in RFC 2046, commonly used for HTML form
     * file uploads. Each part is separated by a boundary string and can contain
     * either form fields or file content.
     * </p>
     *
     * <h4>Multipart Format</h4>
     * <pre>
     * --boundary\r\n
     * Content-Disposition: form-data; name="field1"\r\n
     * \r\n
     * field1value\r\n
     * --boundary\r\n
     * Content-Disposition: form-data; name="file"; filename="example.txt"\r\n
     * Content-Type: text/plain\r\n
     * \r\n
     * [file content]\r\n
     * --boundary--\r\n
     * </pre>
     *
     * <h4>Extracted Data</h4>
     * <ul>
     *     <li><b>Form fields:</b> Added as {@link NVPair} to parameters</li>
     *     <li><b>JSON fields:</b> Parsed and added as {@link NVGenericMap}</li>
     *     <li><b>Files:</b> Added as {@link NamedValue}&lt;InputStream&gt; with metadata
     *         (filename, content-type, length) in properties</li>
     * </ul>
     *
     * <p>
     * <b>Note:</b> This decoder requires the complete message. For streaming/chunked
     * multipart processing, use {@link #MULTIPART_FORM_DATA_CHUNKED}.
     * </p>
     *
     * @return the {@link HTTPMessageConfigInterface} with parsed parameters and files, or null if not applicable
     * @see #MULTIPART_FORM_DATA_CHUNKED
     */
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
                                            if (location != null)
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

    /**
     * Decoder for streaming/chunked {@code multipart/form-data} content.
     * <p>
     * Handles large file uploads by processing multipart data incrementally as it arrives,
     * rather than waiting for the complete message. This is essential for handling large
     * files that shouldn't be fully buffered in memory.
     * </p>
     *
     * <h4>Key Features</h4>
     * <ul>
     *     <li><b>Streaming processing:</b> Processes data as it arrives without full buffering</li>
     *     <li><b>Partial file chunks:</b> Returns file data in chunks via {@link InputStream}</li>
     *     <li><b>Memory efficient:</b> Uses a safety buffer to prevent boundary misalignment</li>
     *     <li><b>Chunk tracking:</b> Marks chunks with {@code LAST_CHUNK} property when complete</li>
     * </ul>
     *
     * <h4>Processing Flow</h4>
     * <ol>
     *     <li>Extract boundary from Content-Type header</li>
     *     <li>Build boundary markers (start, content-end, final)</li>
     *     <li>Process each multipart section as data arrives</li>
     *     <li>For files: return partial {@link InputStream}s with close callbacks for buffer management</li>
     *     <li>For fields: parse complete value and add to parameters</li>
     * </ol>
     *
     * <h4>File Chunk Properties</h4>
     * <ul>
     *     <li>{@code filename} - Original filename</li>
     *     <li>{@code Content-Type} - MIME type of the file</li>
     *     <li>{@code Content-Length} - Total file size (if provided)</li>
     *     <li>{@code LAST_CHUNK} - Boolean indicating if this is the final chunk</li>
     * </ul>
     *
     * @return the {@link HTTPMessageConfigInterface} with incrementally parsed data
     * @see #MULTIPART_FORM_DATA
     */
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
                                            if (location != null && location.getValue() != null)
                                                paramFileToAdd.getProperties().build(location.getName(), "" + location.getValue());

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
                                        if (location != null && location.getValue() != null)
                                            paramFileToAdd.getProperties().build(location.getName(), "" + location.getValue());

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


    /**
     * Creates a debug string representation of the HTTPRawMessage state.
     *
     * @param hrm the raw message to describe
     * @return a string with DataMark, LastProcessedDataIndex, and buffer size
     */
    private static String toString(HTTPRawMessage hrm) {
        return "++DataMark: " + hrm.getDataMark() + " LastProcessedDataIndex: " + hrm.getLastProcessedDataIndex() + " UBAOS Size: " + hrm.getDataStream().size() + " --";
    }

    /**
     * Finds the index where the current multipart content section ends.
     * <p>
     * Searches for either the content boundary marker (between parts) or the
     * final boundary marker (end of multipart message).
     * </p>
     *
     * @param hrm the raw message being processed
     * @param startIndex the index to start searching from
     * @return the index of the content end marker, or -1 if not found
     */
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

    /**
     * Parses the headers of a complete multipart section.
     * <p>
     * Locates the boundary start marker and parses all headers up to the
     * header/content separator (CRLFCRLF). Returns structured header data
     * including content disposition, content type, and content indices.
     * </p>
     *
     * <h4>Returned Properties</h4>
     * <ul>
     *     <li>{@code Content-Disposition} - Form field name and optional filename</li>
     *     <li>{@code Content-Type} - MIME type of the part content</li>
     *     <li>{@code Content-Length} - Size of content (if provided)</li>
     *     <li>{@code SUB_CONTENT_START_INDEX} - Index where content data begins</li>
     *     <li>{@code SUB_CONTENT_END_INDEX} - Index where content data ends (-1 if incomplete)</li>
     *     <li>{@code IS_FILE} - Boolean indicating if this part is a file upload</li>
     * </ul>
     *
     * @param hrm the raw message being processed
     * @param startIndex the index to start searching from
     * @return an {@link NVGenericMap} with parsed headers and metadata, or null if incomplete
     */
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
