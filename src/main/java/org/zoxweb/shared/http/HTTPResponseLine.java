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

@SuppressWarnings("serial")
public class HTTPResponseLine 
    extends MessageFirstLine
{

	public HTTPResponseLine(String statusLine)
	{
		super(statusLine);
	}
	
	public String getVersion() {
		return getFirstToken();
	}
	public void setVersion(String version) {
		setFirstToken(version);
	}
	public int getStatus() {
		return Integer.parseInt(getSecondToken());
	}
	public void setStatus(int status) {
		setSecondToken(""+status);
	}
	public String getReason() {
		return getThirdToken();
	}
	public void setReason(String reason) 
	{
		setThirdToken(reason);
	}

	
	public HTTPVersion getHTTPVersion()
	{
		return HTTPVersion.lookup(getVersion());
	}
	
	public HTTPStatusCode getHTTPStatusCode()
	{
		return HTTPStatusCode.statusByCode(getStatus());
	}
}
