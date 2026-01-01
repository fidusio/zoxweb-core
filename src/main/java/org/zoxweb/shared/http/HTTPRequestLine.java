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

import org.zoxweb.shared.protocol.MessageFirstLine;
import org.zoxweb.shared.util.SharedUtil;

/**
 * Represents the first line of an HTTP request message.
 * Parses and provides access to the HTTP method, URI, and version components.
 * <p>
 * Format: "METHOD URI HTTP/version" (e.g., "GET /index.html HTTP/1.1")
 * </p>
 *
 * @author mnael
 * @see MessageFirstLine
 */
@SuppressWarnings("serial")
public class HTTPRequestLine
extends MessageFirstLine
{
	/**
	 * Constructs an HTTPRequestLine by parsing the full request line string.
	 *
	 * @param fullRequestLine the complete HTTP request line
	 */
	public HTTPRequestLine(String fullRequestLine)
	{
		super(fullRequestLine);
	}

	/**
	 * Returns the HTTP method string (e.g., "GET", "POST").
	 *
	 * @return the HTTP method
	 */
	public String getMethod() {
		return getFirstToken();
	}

	/**
	 * Sets the HTTP method.
	 *
	 * @param method the HTTP method to set
	 */
	public void setMethod(String method) {
		setFirstToken(method);
	}

	/**
	 * Returns the request URI.
	 *
	 * @return the URI path
	 */
	public String getURI() {
		return getSecondToken();
	}

	/**
	 * Sets the request URI.
	 *
	 * @param uri the URI to set
	 */
	public void setURI(String uri) {
		setSecondToken( uri);
	}

	/**
	 * Returns the HTTP version string (e.g., "HTTP/1.1").
	 *
	 * @return the HTTP version string
	 */
	public String getVersion() {
		return getThirdToken();
	}

	/**
	 * Sets the HTTP version string.
	 *
	 * @param version the HTTP version to set
	 */
	public void setVersion(String version)
	{
		setThirdToken(version);
	}

	/**
	 * Returns the HTTP version as an enum.
	 *
	 * @return the HTTPVersion enum value
	 */
	public HTTPVersion getHTTPVersion()
	{
		return HTTPVersion.lookup(getVersion());
	}

	/**
	 * Returns the HTTP method as an enum.
	 *
	 * @return the HTTPMethod enum value
	 */
	public HTTPMethod getHTTPMethod()
	{
		return (HTTPMethod) SharedUtil.lookupEnum(getMethod(), HTTPMethod.values());
	}
	
}
