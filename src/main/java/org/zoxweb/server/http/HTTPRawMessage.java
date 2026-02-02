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

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HTTPRawMessage
        implements GetNVProperties {

    public static final LogWrapper log = new LogWrapper(HTTPRawMessage.class).setEnabled(false);

    private final UByteArrayOutputStream ubaos;
    private volatile boolean parsedHeadersStatus = false;
    private volatile int dataMark = 0;
    private volatile int indexOfLastProcessedData = 0;

    private boolean endOfChunkedContentReached = false;
    private volatile String firstLine = null;
    private volatile boolean endOfContent = false;
    private volatile HTTPMessageConfigInterface hmci = new HTTPMessageConfig();
    private volatile NamedValue<?> pendingParameter = null;
    private volatile NVGenericMap properties = new NVGenericMap("parsing-properties");
    private boolean clientMode = false;


    private volatile NamedValue<?> lastParam = null;

    public HTTPRawMessage(String msg) {
        this(SharedStringUtil.getBytes(msg));
    }

    public HTTPRawMessage(byte[] fullMessage) {
        this(fullMessage, 0, fullMessage.length);
    }

    public HTTPRawMessage(byte[] fullMessage, int offset, int len) {
        ubaos = new UByteArrayOutputStream(len);
        ubaos.write(fullMessage, offset, len);
    }

    public HTTPRawMessage() {
        this(new UByteArrayOutputStream());
    }

    public HTTPRawMessage(boolean clientMode) {
        this(new UByteArrayOutputStream());
        this.clientMode = clientMode;
    }


    public HTTPRawMessage(UByteArrayOutputStream ubaos) {
        this.ubaos = ubaos;
    }


    public UByteArrayOutputStream getDataStream() {
        return ubaos;
    }

    private boolean parseRawHeaders() {
        if (!areHeadersParsed()) {
            int endOfHeadersIndex = ubaos.indexOf(Delimiter.CRLFCRLF.getBytes());
            if (endOfHeadersIndex != -1) {
                int headersLineCounter = 0;
                while (getDataMark() < endOfHeadersIndex) {
                    int endOfCurrentLine = ubaos.indexOf(getDataMark(), Delimiter.CRLF.getBytes());//, 0, ProtocolDelimiter.CRLF.getBytes().length);

                    if (endOfCurrentLine != -1) {
                        headersLineCounter++;
                        String currentHeaderLine = new String(Arrays.copyOfRange(ubaos.getInternalBuffer(), getDataMark(), endOfCurrentLine));

                        if (headersLineCounter > 1) {
                            GetNameValue<String> gnv = SharedUtil.toNVPair(currentHeaderLine, ":", true);
                            hmci.getHeaders().add(gnv);
                        } else {


                            if (isClientMode()) {
                                HTTPResponseLine fistLine = new HTTPResponseLine(currentHeaderLine);
                                hmci.setHTTPStatusCode(fistLine.getHTTPStatusCode());
                                hmci.setHTTPVersion(fistLine.getVersion());
                            } else {
                                /*
								first line of server request not part of the headers L=line H=header
								=====================================================================================
								(L1) POST /system-upload HTTP/1.1\r\n
								(H1) Authorization: Basic YWRtaW46YmF0YXRh\r\n
								(H2) Content-Type: multipart/form-data; boundary=18117525-f472-4559-baee-e5b7e3480095\r\n
								(H3) Content-Length: 709\r\n
								(H4) Host: localhost:6443\r\n
								(H5) Connection: Keep-Alive\r\n
								(H6) Accept-Encoding: gzip\r\n
								(H7) User-Agent: okhttp/5.0.0-alpha.14\r\n\r\n
								=====================================================================================
							    */
//                                HTTPResponseLine responseLine =  new HTTPResponseLine(currentHeaderLine);
//                                hmci.setMethod(responseLine.getFirstToken());
//                                hmci.setURI(responseLine.getSecondToken());
//                                hmci.set(responseLine.getHTTPStatusCode());
                                String[] tokens = currentHeaderLine.split(" ");
                                for (int i = 0; i < tokens.length; i++) {
                                    String token = tokens[i];
                                    switch (i) {
                                        case 0:
                                            hmci.setMethod(HTTPMethod.lookup(token));
                                            break;
                                        case 1:
                                            hmci.setURI(token);
                                            break;
                                        case 2:
                                            hmci.setHTTPVersion(token);
                                            break;
                                    }
                                }
                                if (hmci.getMethod() == HTTPMethod.GET) {
                                    HTTPCodecs.WWW_URL_ENC.decode(this);
                                }
                            }

                        }
                        setDataMark(endOfCurrentLine + Delimiter.CRLF.getBytes().length);

                    }
                }
                // finished parsing all the headers
                // after that we are ready for content
                ubaos.shiftLeft(endOfHeadersIndex + Delimiter.CRLFCRLF.getBytes().length, 0);
                setDataMark(0);
                parsedHeadersStatus = true;
            }
        }

        return parsedHeadersStatus;
    }


    public synchronized int getLastProcessedDataIndex() {
        return indexOfLastProcessedData;
    }

    public synchronized void setLastProcessedDataIndex(int indexOfLastParsedData) {
        if (indexOfLastParsedData > dataMark)
            throw new IllegalArgumentException("indexOfLastParsedData: " + indexOfLastParsedData + " > dataMark: " + dataMark);

        this.indexOfLastProcessedData = indexOfLastParsedData;
    }

    private void validateHTTPMethod() {
        int firstSpaceIndex = ubaos.indexOf(0, Delimiter.SPACE.getBytes());
        if (firstSpaceIndex != -1) {
            String maybeHttpMethod = ubaos.getString(0, firstSpaceIndex);
            // try to parse the http method
            if (HTTPMethod.lookup(ubaos.getString(0, firstSpaceIndex)) == null) {
                throw new IllegalArgumentException("Invalid HTTP method " + maybeHttpMethod);
            }
        } else if (ubaos.size() > 25) {
            throw new IllegalArgumentException("HTTP method token too big");
        }

    }


    public synchronized boolean isMessageComplete() {
        if (areHeadersParsed()) {
            if (hmci.getContentLength() != -1) {
                // content length is set
                if (log.isEnabled())
                    log.getLogger().info(SUS.toCanonicalID(',', hmci.getContentLength(), ubaos.size(), (hmci.getContentLength() - ubaos.size())));
                return (hmci.getContentLength() == ubaos.size());
            } else if (hmci.isTransferChunked()) {
                if (hmci.isMultiPartEncoding())// we have a chunked request
                    return endOfContent;
                return isEndOfChunkedContentReached();
            }

            // default there is no content
            return true;
        }
        return false;
    }


    public synchronized boolean parseResponse(URIScheme protocolMode, ByteBuffer inBuffer) throws IOException {

        ByteBufferUtil.write(inBuffer, getDataStream(), true);
        switch (protocolMode) {
            case HTTP:
            case HTTPS:
                HTTPMessageConfigInterface hmcitemp = parse();
                boolean ret = isMessageComplete();// ? rawRequest.getHTTPMessageConfig() : null;
                if (!ret && areHeadersParsed() && hmcitemp.isTransferChunked()) {
                    ret = true;
                }
                if (log.isEnabled())
                    log.getLogger().info("Protocol Mode: " + protocolMode + " message complete " + ret);
                return ret;
            case WS:
            case WSS:
                // to be added here
                return true;
            default:
                throw new IllegalStateException("Unexpected value: " + protocolMode);
        }
    }

    public boolean isClientMode() {
        return clientMode;
    }

    public synchronized HTTPMessageConfigInterface parse() {
        if (!isClientMode() && hmci.getMethod() == null)
            validateHTTPMethod();


        if (parseRawHeaders()) {
            if (hmci.isTransferChunked()) {
                HTTPCodecs.TRANSFER_CHUNKED.decode(this);
                if (hmci.isMultiPartEncoding()) {
                    HTTPCodecs.MULTIPART_FORM_DATA_CHUNKED.decode(this);
                }
            } else if (isMessageComplete()) {
                if (hmci.getMethod() != HTTPMethod.GET) {
                    HTTPMediaType hmt = HTTPMediaType.lookup(hmci.getContentType());
                    if (hmt != null) {
                        switch (hmt) {

                            case APPLICATION_WWW_URL_ENC:
                                HTTPCodecs.WWW_URL_ENC.decode(this);
                                break;

                            case APPLICATION_OCTET_STREAM:
                                break;
                            case MULTIPART_FORM_DATA:
                                HTTPCodecs.MULTIPART_FORM_DATA.decode(this);
                                break;
                            case TEXT_CSV:

                            case TEXT_CSS:

                            case TEXT_HTML:

                            case TEXT_JAVASCRIPT:

                            case TEXT_PLAIN:

                            case TEXT_YAML:

                            case APPLICATION_JSON:
                                hmci.setContent(ubaos.copyBytes(0));
                                break;
                            case IMAGE_BMP:
                                break;
                            case IMAGE_GIF:
                                break;
                            case IMAGE_JPEG:
                                break;
                            case IMAGE_PNG:
                                break;
                            case IMAGE_SVG:
                                break;
                            case IMAGE_ICON:
                                break;
                            case IMAGE_TIF:
                                break;
                        }
                    }
                }
            }

        }

        return hmci;
    }


    public synchronized void reset(boolean hmciToo) {
        ubaos.reset();
        if (hmciToo) {
            hmci = new HTTPMessageConfig();
            parsedHeadersStatus = false;
            setDataMark(0);
            firstLine = null;
            endOfContent = false;
            lastParam = null;
            endOfChunkedContentReached = false;
            properties.clear();
        }
    }

    public boolean areHeadersParsed() {
        return parsedHeadersStatus;
    }

    public synchronized void endOfContentReached() {
        endOfContent = true;
    }

    public synchronized void endOfChunksReached() {
        endOfChunkedContentReached = true;
    }

    public boolean isEndOfChunkedContentReached() {
        return endOfChunkedContentReached;
    }

    public HTTPMessageConfigInterface getHTTPMessageConfig() {
        return hmci;
    }

    @Override
    public String toString() {
        return "HTTPRawMessage [parsedHeadersStatus=" + areHeadersParsed()
                + ", contentLength=" + hmci.getContentLength() + ", headers=" + hmci.getHeaders()
                + ", firstLine=" + firstLine + ", baos=" + ubaos.size() + "]";
    }

    protected int getDataMark() {
        return dataMark;
    }

    public synchronized void setDataMark(int index) {
        this.dataMark = index;
    }

    public synchronized int incDataMark(int inc) {
        this.dataMark += inc;
        return dataMark;
    }

    public NamedValue<?> getPendingParameter() {
        return pendingParameter;
    }

    public void setPendingParameter(NamedValue<?> pending) {
        this.pendingParameter = pending;
    }


    public NamedValue<?> incompleteParam() {
        return lastParam;
    }

    public void incompleteParam(NamedValue<?> lastParam) {
        this.lastParam = lastParam;
    }

    /**
     * @return
     */
    @Override
    public NVGenericMap getProperties() {
        return properties;
    }
}