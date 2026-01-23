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

import org.zoxweb.shared.util.SUS;

import java.io.IOException;

/**
 * Exception thrown when an HTTP call fails with an error status code or encounters
 * a processing error. This exception extends IOException and includes additional
 * HTTP-specific information such as the status code, response data, and message configuration.
 *
 * @author mnael
 * @see HTTPStatusCode
 * @see HTTPResponseData
 */
@SuppressWarnings("serial")
public class HTTPCallException
extends IOException
{
	
	private final HTTPStatusCode statusCode;
	private final HTTPResponseData responseData;

	private final HTTPMessageConfigInterface hmci;
	
	/**
	 * Default constructor creating an empty HTTPCallException.
	 */
	public HTTPCallException()
	{
		this(null, null, (HTTPResponseData)null);
	}

	/**
	 * Constructs an HTTPCallException with the specified reason message.
	 *
	 * @param reason the error message describing the failure
	 */
	public HTTPCallException(String reason)
	{
		this(reason, null, (HTTPResponseData)null);
	}

	/**
	 * Constructs an HTTPCallException with the specified reason and response data.
	 *
	 * @param reason the error message describing the failure
	 * @param rd     the HTTP response data associated with the error
	 */
	public HTTPCallException(String reason, HTTPResponseData rd)
	{
		this(reason, null, rd);
	}

	/**
	 * Constructs an HTTPCallException with the specified reason and status code.
	 *
	 * @param reason     the error message describing the failure
	 * @param statusCode the HTTP status code of the error response
	 */
	public HTTPCallException(String reason, HTTPStatusCode statusCode)
	{
		this(reason, statusCode, (HTTPResponseData)null);
	}

	/**
	 * Constructs an HTTPCallException with the specified reason, status code, and response data.
	 *
	 * @param reason     the error message describing the failure
	 * @param statusCode the HTTP status code of the error response
	 * @param rd         the HTTP response data associated with the error
	 */
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

	/**
	 * Constructs an HTTPCallException with the specified reason and message configuration.
	 *
	 * @param reason the error message describing the failure
	 * @param hmci   the HTTP message configuration associated with the error
	 */
	public HTTPCallException(String reason, HTTPMessageConfigInterface hmci)
	{
		this(reason, null, hmci);
	}

	/**
	 * Constructs an HTTPCallException with the specified reason, status code, and message configuration.
	 *
	 * @param reason     the error message describing the failure
	 * @param statusCode the HTTP status code of the error response
	 * @param hmci       the HTTP message configuration associated with the error
	 */
	public HTTPCallException(String reason, HTTPStatusCode statusCode, HTTPMessageConfigInterface hmci)
	{
		super(reason);
		this.responseData = null;
		this.hmci = hmci;
		this.statusCode =  statusCode != null ? statusCode : hmci.getHTTPStatusCode();
	}

	
	@Override
	public String toString() {
		return SUS.toCanonicalID(' ', super.toString(), statusCode) + "\n" + (getResponseData() != null ? getResponseData() : getMessageConfig());
	}


	/**
	 * Returns the HTTP response data associated with this exception.
	 *
	 * @return the response data, or null if not available
	 */
	public HTTPResponseData getResponseData()
	{
		return responseData;
	}

	/**
	 * Returns the HTTP message configuration associated with this exception.
	 *
	 * @return the message configuration, or null if not available
	 */
	public HTTPMessageConfigInterface getMessageConfig()
	{
		return hmci;
	}

	/**
	 * Returns the HTTP status code associated with this exception.
	 *
	 * @return the HTTP status code
	 */
	public HTTPStatusCode getStatusCode()
	{
		return statusCode;
	}
}
