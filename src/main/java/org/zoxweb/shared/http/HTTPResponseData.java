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
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
public class HTTPResponseData
extends HTTPAPIResult<byte[]>
	implements Serializable

{

	/**
	 * Main constructor
	 * @param stat response status.
	 * @param data response data.
	 * @param rh response headers.
	 */
	public HTTPResponseData(int stat, Map<String, List<String>> rh, byte[] data, long duration)
	{
		super(stat, rh, data, duration);


	}




	@Override
	public String toString() {
		return "ResponseData [status=" + getStatus() + ", data="
				+ (getData() != null ? getDataAsString() : "null") + ", headers="
				+ getHeaders()+
				"duration: " + Const.TimeInMillis.toString(getDuration()) + "]";
	}

	public String getDataAsString()
	{
		if (getData() != null)
			return SharedStringUtil.toString(getData());

		return null;
	}


	
	
	
}
