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

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.http.HTTPRequestLine;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.util.GetNameValue;

import java.util.List;

public class HTTPRawFormatter {
    private final String firstLine;
    private final List<GetNameValue<String>> headers;
    private final byte[] content;
    private UByteArrayOutputStream ubaos;

    public HTTPRawFormatter(HTTPRequestLine rrl, List<GetNameValue<String>> headers, byte[] content) {
        this.firstLine = rrl.toString();
        this.headers = headers;
        this.content = content;
    }

    public HTTPRawFormatter(String firstLine, List<GetNameValue<String>> headers, byte[] content) {
        this.firstLine = firstLine;
        this.headers = headers;
        this.content = content;
    }

    public synchronized UByteArrayOutputStream format() {
        if (ubaos == null) {
            ubaos = new UByteArrayOutputStream();
            ubaos.write(firstLine);
            ubaos.write(Delimiter.CRLF.getBytes());
            if (headers != null) {
                for (GetNameValue<String> gnv : headers) {
                    ubaos.write(gnv.getName());
                    ubaos.write(Delimiter.COLON.getBytes());
                    String value = gnv.getValue();

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

            if (content != null) {
                ubaos.write(content);
            }
        }

        return ubaos;
    }


}
