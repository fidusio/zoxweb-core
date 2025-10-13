/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SetNameValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HTTPMultiPartParameter
        implements SetNameValue<String> {

    private String name;
    private String value;
    private final HashMap<String, ArrayList<GetNameValue<String>>> headers = new HashMap<String, ArrayList<GetNameValue<String>>>();

    private String fileName;
    private byte[] content = null;
    private URL url;
    private InputStream inputStream;
    private boolean autoClose;


    public HTTPMultiPartParameter() {

    }

    public HTTPMultiPartParameter(String name, String value) {
        setName(name);
        setValue(value);
    }

    public void addHeader(GetName gv, GetNameValue<String>... gnvs) {
        addHeader(gv.getName(), gnvs);
    }

    public synchronized void addHeader(String headerName, GetNameValue<String>... gnvs) {
        ArrayList<GetNameValue<String>> list = headers.get(headerName);

        if (list == null) {
            list = new ArrayList<GetNameValue<String>>();
            headers.put(headerName, list);
        }

        for (GetNameValue<String> gnv : gnvs) {
            list.add(gnv);
        }
    }

    public Map<String, ArrayList<GetNameValue<String>>> getHeaders() {
        return headers;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public InputStream getInputStreamValue() {
        return inputStream;
    }

    public void setInputStreamValue(InputStream inputStream, boolean close) {
        this.inputStream = inputStream;
        autoClose = close;
    }

    /**
     * If content is byte array that means we have a binary data
     * @return content value as byte array
     */
    public byte[] getContentValue() {
        return content;
    }

    public void setContentValue(byte[] content) {
        this.content = content;
    }

    /**
     * If the file name is set that mean this is a file upload
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isAutoClose() {
        return autoClose;
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) throws IOException {
        this.url = url;
        setInputStreamValue(url.openStream(), true);

    }

    @Override
    public String toString() {
        return "HTTPMultiPartParameter [name=" + name + ", value=" + value
                + ", headers=" + headers + ", fileName=" + fileName
                + ", content=" + Arrays.toString(content) + ", url=" + url
                + ", inputStream=" + inputStream + ", autoClose=" + autoClose
                + "]";
    }


}
