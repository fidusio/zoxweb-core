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

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayInputStream;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.io.WriteTo;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HTTPRequestFormatter
        implements WriteTo {
    private final String firstLine;
    private final NVGenericMap headers;
    private volatile UByteArrayOutputStream ubaos;
    private volatile InputStream bodyASIS;


    public HTTPRequestFormatter(HTTPMessageConfigInterface hmci) {
        URLInfo urlInfo = hmci.toURLInfo();
        hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);

        if (hmci.getAuthorization() != null)
            hmci.getHeaders().add(hmci.getAuthorization().toHTTPHeader());
        // need to process parameters also
        String urlEncodedParameter;
        String fullURI = urlInfo.toURI();
        if (hmci.isContentURLEncoded()) {
            urlEncodedParameter = SUS.trimOrNull(HTTPEncoder.URL_ENCODED.format(hmci.getParameters().asArrayValuesString().values()));
            if (hmci.getMethod() == HTTPMethod.GET && urlEncodedParameter != null) {
                if (fullURI.indexOf('?') == -1)
                    fullURI += "?" + urlEncodedParameter;
                else if (fullURI.endsWith("&"))
                    fullURI += urlEncodedParameter;
                else
                    fullURI += "&" + urlEncodedParameter;
            } else if (urlEncodedParameter != null && hmci.getContent() == null) {
                hmci.setContent(urlEncodedParameter);
            }

        }


        this.firstLine = hmci.getMethod().getName() + " " + fullURI + " " + hmci.getHTTPVersion();
        this.headers = hmci.getHeaders();
        bodyASIS = hmci.getContentAsIS();


    }


    public HTTPRequestFormatter(HTTPRequestLine rrl, NVGenericMap headers, byte[] content) {
        this.firstLine = rrl.toString();
        this.headers = headers;
        if (SUS.isNotEmpty(content))
            bodyASIS = new UByteArrayInputStream(content);
    }

    public HTTPRequestFormatter(String firstLine, NVGenericMap headers, byte[] content) {
        this.firstLine = firstLine;
        this.headers = headers;
        if (SUS.isNotEmpty(content))
            bodyASIS = new UByteArrayInputStream(content);
    }

    public synchronized UByteArrayOutputStream formatHeader() {
        if (ubaos == null) {
            ubaos = new UByteArrayOutputStream(512);
            ubaos.write(firstLine);
            ubaos.write(Delimiter.CRLF.getBytes());
            if (headers != null) {
                for (GetNameValue<?> gnv : headers.valuesAs(new GetNameValue[0])) {
                    ubaos.write(gnv.getName());
                    ubaos.write(Delimiter.COLON.getBytes());
                    String value = gnv.getValue() != null ? gnv.getValue().toString() : null;

                    if (value != null && !value.isEmpty()) {
                        if (value.charAt(0) != ' ') {
                            ubaos.write(' ');
                        }
                        ubaos.write(value);
                    }
                    ubaos.write(Delimiter.CRLF.getBytes());
                }
            }

            ubaos.write(Delimiter.CRLF.getBytes());
        }
        return ubaos;
    }


    /**
     * Write the whole content to output stream
     *
     * @param out stream
     * @throws IOException in case of error
     */
    @Override
    public synchronized void writeTo(OutputStream out) throws IOException {
        formatHeader().writeTo(out);
        if (bodyASIS != null)
            IOUtil.relayStreams(bodyASIS, out, true, false);

    }
}
