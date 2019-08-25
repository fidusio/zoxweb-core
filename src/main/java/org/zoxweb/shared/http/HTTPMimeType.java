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

import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 *
 */
public enum HTTPMimeType
	implements GetValue<String>
{
	APPLICATION_WWW_URL_ENC("application/x-www-form-urlencoded"),
	APPLICATION_JSON("application/json", "json"),
	APPLICATION_JAVA_SCRIPT("application/x-javascript", "js"),
	APPLICATION_OCTET_STREAM("application/octet-stream"),
	MULTIPART_FORM_DATA("multipart/form-data"),
	TEXT_CSV("text/csv", "csv"),
	TEXT_HTML("text/html", "htm", "html"),
	TEXT_PLAIN("text/plain", "txt"),
	TEXT_YAML("text/yaml", "yaml", "yml"),
	IMAGE_BMP("image/bmp", "bmp"),
	IMAGE_GIF("image/gif", "gif"),
	IMAGE_JPEG("image/jpeg", "jpe", "jpeg", "jpg"),
	IMAGE_PNG("image/png", "png"),
	IMAGE_SVG("image/svg+xml", "svg"),
	IMAGE_ICON("image/x-icon", "ico"),
	IMAGE_TIF("image/tiff", "tiff", "tif");
	
	

	
	;

	private final String value;
	private final String[] extensions;
	
	
	HTTPMimeType(String value, String ...extensions)
	{
		this.value = value;
		this.extensions = extensions;
	}

	/**
	 * @see org.zoxweb.shared.util.GetValue#getValue()
	 */
	@Override
	public String getValue() 
	{
		return value;
	}
	
	public static HTTPMimeType lookup(String str)
	{
		return (HTTPMimeType) SharedUtil.matchingEnumContent(HTTPMimeType.values(), str);
	}
	
	public static HTTPMimeType lookupByExtenstion(String str)
	{
		str = SharedStringUtil.trimOrNull(str);
		
		if (str != null)
		{
			String ext = SharedStringUtil.valueAfterRightToken(str, ".").toLowerCase();
			for (HTTPMimeType mt : HTTPMimeType.values())
			{
				if (mt.extensions != null)
				{
					for (String mtExt : mt.extensions)
					{
						if (ext.equals(mtExt))
							return mt;
					}
				}
			}
		}
		
		return null;
		
		
	}
	
	public static GetNameValue<String> toContentType(GetValue<String> contentType)
	{
		return new NVPair(HTTPHeaderName.CONTENT_TYPE, contentType);
	}
	
	
	public static GetNameValue<String> toContentType(String contentType)
	{
		return new NVPair(HTTPHeaderName.CONTENT_TYPE, contentType);
	}

}
