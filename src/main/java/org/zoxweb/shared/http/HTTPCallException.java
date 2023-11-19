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

import java.io.IOException;

@SuppressWarnings("serial")
public class HTTPCallException 
extends IOException 
{
	
	private final HTTPStatusCode statusCode;
	private final HTTPResponseData responseData;

	private final HTTPMessageConfigInterface hmci;
	
	public HTTPCallException()
	{
		this(null, null, (HTTPResponseData)null);
	}
	


	public HTTPCallException(String reason)
	{
		this(reason, null, (HTTPResponseData)null);
	}

	public HTTPCallException(String reason, HTTPResponseData rd)
	{
		this(reason, null, rd);
	}

	public HTTPCallException(String reason, HTTPStatusCode statusCode)
	{
		this(reason, statusCode, (HTTPResponseData)null);
	}

	public HTTPCallException(String reason, HTTPStatusCode statusCode, HTTPResponseData rd)
	{
		super(reason);
		this.responseData = rd;
		this.hmci = null;
		if (statusCode == null && responseData != null)
			this.statusCode = HTTPStatusCode.statusByCode(rd.getStatus());
		else
			this.statusCode = statusCode;
	}

	public HTTPCallException(String reason, HTTPMessageConfigInterface hmci)
	{
		this(reason, null, hmci);
	}

	public HTTPCallException(String reason, HTTPStatusCode statusCode, HTTPMessageConfigInterface hmci)
	{
		super(reason);
		this.responseData = null;
		this.hmci = hmci;
		this.statusCode =  statusCode != null ? statusCode : hmci.getHTTPStatusCode();
	}

	
	@Override
	public String toString() {
		return super.toString()+  " " +statusCode + "\n" + (getResponseData() != null ? getResponseData() : getMessageConfig());
	}


	public HTTPResponseData getResponseData() 
	{
		return responseData;
	}
	public HTTPMessageConfigInterface getMessageConfig()
	{
		return hmci;
	}

	public HTTPStatusCode getStatusCode()
	{
		return statusCode;
	}
}
