/*
 * Copyright 2012 ZoxWeb.com LLC.
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

import org.zoxweb.shared.protocol.MessageFirstLine;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class HTTPRequestLine
extends MessageFirstLine
{
	public HTTPRequestLine(String fullRequestLine)
	{
		super(fullRequestLine);
	}
	
	public String getMethod() {
		return getFirstToken();
	}
	public void setMethod(String method) {
		setFirstToken(method);
	}
	public String getURI() {
		return getSecondToken();
	}
	public void setURI(String uri) {
		setSecondToken( uri);
	}
	public String getVersion() {
		return getThirdToken();
	}
	public void setVersion(String version) 
	{
		setThirdToken(version);
	}
	
	public HTTPVersion getHTTPVersion()
	{
		return HTTPVersion.lookup(getVersion());
	}
	
	public HTTPMethod getHTTPMethod()
	{
		return (HTTPMethod) SharedUtil.lookupEnum(HTTPMethod.values(), getMethod());
	}
	
}
