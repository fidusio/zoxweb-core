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

public enum HTTPStatusCode 
{
    /**
     * 100 Continue
     */
	CONTINUE(100, "Continue"),
    /**
     * 101 Switching Protocol
     */
	SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    PROCESSING(102, "Processing"),
    /**
     * 103
     */
    EARLY_HINTS(103, "Early Hints"),
	
	
	/**
     * 200 OK.
     */
    OK(200, "OK"),
    /**
     * 201 Created.
     */
    CREATED(201, "Created"),
    /**
     * 202 Accepted.
     */
    ACCEPTED(202, "Accepted"),
    /**
     * 202 Accepted.
     */
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    /**
     * 204 No Content.
     */
    NO_CONTENT(204, "No Content"),
    /**
     * 205 Reset Content.
     */
    RESET_CONTENT(205, "Reset Content"),
    /**
     * 206 Partial Content
     */
    PARTIAL_CONTENT(206, "Partial Content"),
    /**
     * 207
     */
    MULTI_STATUS(207, "Multi-Status"),
    /**
     * 208
     */
    AlREADY_REPORTED(208, "Already Reported"),
    /**
     * 226
     */
    IM_USED(226, "IM Used"),


    /**
     * 300
     */
    MULTIPLE_CHOICES(300, "Multiple Choices"),


    /**
     * 301 Moved Permanently.
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    /**
     * 302 Found.
     */
    FOUND(302, "Found"),
    /**
     * 303 See Other.
     */
    SEE_OTHER(303, "See Other"),
    /**
     * 304 Not Modified.
     */
    NOT_MODIFIED(304, "Not Modified"),
    /**
     * 305 Use Proxy.
     */
    USE_PROXY(305, "Use Proxy"),
    /**
     * 307 Temporary Redirect.
     */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    /**
     * 308 Permanent Redirect.
     */
    PERMANENT_REDIRECT(308, "Temporary Redirect"),


    /**
     * 400 Bad Request.
     */
    BAD_REQUEST(400, "Bad Request"),
    /**
     * 401 Unauthorized.
     */
    UNAUTHORIZED(401, "Unauthorized"),
    /**
     * 402 Payment Required.
     */
    PAYMENT_REQUIRED(402, "Payment Required"),
    /**
     * 403 Forbidden.
     */
    FORBIDDEN(403, "Forbidden"),
    /**
     * 404 Not Found.
     */
    NOT_FOUND(404, "Not Found"),
    /**
     * 405 Method Not Allowed.
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    /**
     * 406 Not Acceptable.
     */
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    /**
     * 407 Proxy Authentication Required.
     */
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    /**
     * 408 Request Timeout.
     */
    REQUEST_TIMEOUT(408, "Request Timeout"),
    /**
     * 409 Conflict.
     */
    CONFLICT(409, "Conflict"),
    /**
     * 410 Gone.
     */
    GONE(410, "Gone"),
    /**
     * 411 Length Required.
     */
    LENGTH_REQUIRED(411, "Length Required"),
    /**
     * 412 Precondition Failed.
     */
    PRECONDITION_FAILED(412, "Precondition Failed"),
    /**
     * 413 Request Entity Too Large.
     */
    REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    /**
     * 414 Request-URI Too Long.
     */
    REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
    /**
     * 415 Unsupported Media Type.
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    /**
     * 416 Requested Range Not Satisfiable.
     */
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
    /**
     * 417 Expectation Failed.
     */
    EXPECTATION_FAILED(417, "Expectation Failed"),
    /**
     * 418
     */
    TEAPOT(418, "I'm a teapot"),
    /**
     * 421
     */
    MISDIRECTED_REQUEST(421, "Misdirected Request"),
    /**
     * 422
     */
    UNPROCESSABLE_ENTITY(422, " Unprocessable Entity"),
    /**
     * 423
     */
    LOCKED(423, "Locked"),
    /**
     * 424
     */
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    /**
     * 425
     */
    TOO_EARLY(425, "Too Early"),
    /**
     * 426
     */
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    /**
     * 428
     */
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    /**
     * 429
     */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    /**
     * 431
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    /**
     * 451
     */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),



    /**
     * 500 Internal Server Error.
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    /**
     * 501 Not Implemented.
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),
    /**
     * 502 Bad Gateway.
     */
    BAD_GATEWAY(502, "Bad Gateway"),
    /**
     * 503 Service Unavailable.
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    /**
     * 504 Gateway Timeout.
     */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    /**
     * 505 HTTP Version Not Supported.
     */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    /**
     * 506 Variant Also Negotiated
     */
    VARIANT_ALSO_NEGOTIATED(506, "Variant Also Negotiated"),
    /**
     * 507 Insufficient Storage
     */
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    /**
     * 508 Loop Detected
     */
    LOOP_DETECTED(508, "Loop Detected"),
    /**
     * 510
     */
    NOT_EXTENDED(510, "Not Extended"),
    /**
     * 511 Network Authentication Required
     */
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),


	
	;
	
	public enum Family {INFORMATIONAL, SUCCESSFUL, REDIRECTION, CLIENT_ERROR, SERVER_ERROR, OTHER}
	
	public final int CODE;
	public final String REASON;
	public final Family FAMILY;
	HTTPStatusCode(int value, String reason)
	{
		CODE = value;
		REASON = reason;
		FAMILY = familyByCode(value);
	}
	
	public static HTTPStatusCode statusByCode(int val)
	{	
		for(HTTPStatusCode temp :  HTTPStatusCode.values())
		{
			if (val == temp.CODE)
			{
				return temp;
			}
		}
		return null;
	}
	
	public static Family familyByCode(int httpStatusCode)
	{
		switch(httpStatusCode/100) 
		{
        case 1:  return Family.INFORMATIONAL;
        case 2:  return Family.SUCCESSFUL;
        case 3:  return Family.REDIRECTION;
        case 4:  return Family.CLIENT_ERROR;
        case 5:  return Family.SERVER_ERROR;
		}
		
		throw new IllegalArgumentException("Invalid http status code " + httpStatusCode);
	}
}
