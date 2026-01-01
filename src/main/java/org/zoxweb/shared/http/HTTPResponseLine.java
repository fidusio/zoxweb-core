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

/**
 * Represents the first line (status line) of an HTTP response message.
 * Parses and provides access to the HTTP version, status code, and reason phrase.
 * <p>
 * Format: "HTTP/version status-code reason-phrase" (e.g., "HTTP/1.1 200 OK")
 * </p>
 *
 * @author mnael
 * @see MessageFirstLine
 */
@SuppressWarnings("serial")
public class HTTPResponseLine
    extends MessageFirstLine
{

	/**
	 * Constructs an HTTPResponseLine by parsing the status line.
	 *
	 * @param statusLine the complete HTTP status line
	 */
	public HTTPResponseLine(String statusLine)
	{
		super(statusLine);
	}

	/**
	 * Returns the HTTP version string.
	 *
	 * @return the version (e.g., "HTTP/1.1")
	 */
	public String getVersion() {
		return getFirstToken();
	}

	/**
	 * Sets the HTTP version string.
	 *
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		setFirstToken(version);
	}

	/**
	 * Returns the HTTP status code as an integer.
	 *
	 * @return the status code
	 */
	public int getStatus() {
		return Integer.parseInt(getSecondToken());
	}

	/**
	 * Sets the HTTP status code.
	 *
	 * @param status the status code to set
	 */
	public void setStatus(int status) {
		setSecondToken(""+status);
	}

	/**
	 * Returns the reason phrase.
	 *
	 * @return the reason phrase (e.g., "OK", "Not Found")
	 */
	public String getReason() {
		return getThirdToken();
	}

	/**
	 * Sets the reason phrase.
	 *
	 * @param reason the reason phrase to set
	 */
	public void setReason(String reason)
	{
		setThirdToken(reason);
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
	 * Returns the HTTP status code as an enum.
	 *
	 * @return the HTTPStatusCode enum value
	 */
	public HTTPStatusCode getHTTPStatusCode()
	{
		return HTTPStatusCode.statusByCode(getStatus());
	}
}
