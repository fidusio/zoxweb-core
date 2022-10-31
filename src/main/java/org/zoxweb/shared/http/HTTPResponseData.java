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
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.SharedStringUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
public class HTTPResponseData
	implements Serializable
{
	private int status;
	

	@Override
	public String toString() {
		return "ResponseData [status=" + status + ", data="
				+ (data != null ? new String(data) : "null") + ", responseHeaders="
				+ responseHeaders+"]";
	}

	private byte[] data;
	
	
	

	private Map<String, List<String>> responseHeaders;

	/**
	 * Default constructor
	 */
	public HTTPResponseData()
	{	
	}

	/**
	 *
	 * @deprecated
	 * @param data response data.
	 * @param stat response status.
	 * @param rh response headers.
	 *
	 */
	public HTTPResponseData(final byte[] data, final int stat, Map<String, List<String>> rh)
	{
		this(stat, data, rh);
	}

	/**
	 * Main constructor
	 * @param stat response status.
	 * @param data response data.
	 * @param rh response headers.
	 */
	public HTTPResponseData(final int stat, final byte[] data, Map<String, List<String>> rh)
	{
		setStatus(stat);
		setData(data);
		setResponseHeaders(rh);
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public byte[] getData()
	{
		return data;
	}
	public String getDataAsString()
	{
		if (getData() != null)
			return SharedStringUtil.toString(getData());

		return null;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Map<String, List<String>> getHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	
	
	
}
