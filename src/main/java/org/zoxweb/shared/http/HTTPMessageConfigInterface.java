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

import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.*;

import java.util.List;

/**
 *
 */
public interface HTTPMessageConfigInterface
extends ReferenceID<String>, SetName, SetDescription
{
	long DEFAULT_TIMEOUT_20_SECOND = 20;
	long DEFAULT_TIMEOUT_40_SECOND = 40;
	/**
	 * @return true if multipart encoding
	 */
	boolean isMultiPartEncoding();

	
	
	/**
	 * Get the action parameters as an array list of NVPairs.
	 * The parameters sequence should be preserved during invocation 
	 * @return http parameters
	 */
	NVGenericMap getParameters();

	/**
	 * Set the action parameters list
	 * @param params parameters list
	 */
	void setParameters(List<GetNameValue<String>> params);

	/**
	 * Get the action type
	 * @return the method
	 */
	HTTPMethod getMethod();
	
	/**
	 * Set the action type
	 * @param httpMethod GET, POST ...
	 */
	void setMethod(HTTPMethod httpMethod);
	
	/**
	 * Set the action type
	 * @param httpMethod GET, POST ...
	 */
	void setMethod(String httpMethod);
	
	/**
	 * Get the URI extension
	 * @return the uri part
	 */
	String getURI();
	
	/**
	 * Set the URI extension
	 * @param uri part of the http path
	 */
	void setURI(String uri);
	
	/**
	 * Get the URL
	 * @return url part 
	 */
	String getURL();
	
	
	/**
	 * Set the URL
	 * @param url url part of the http path or the full one
	 */
	void setURL(String url);

	/**
	 * Set the HTTP request parameters
	 * @return headers
	 */
	//ArrayValues<GetNameValue<String>> getHeaders();

	/**
	 *
	 * @return the header collection
	 */
	NVGenericMap getHeaders();
	//NVGenericMap getHeadersNVGM();

	/**
	 * Get the HTTP request parameters
	 * @param headerParams the header parameters as list
	 */
	void setHeaders(List<GetNameValue<String>> headerParams);
	
	/**
	 * @return yhe formatting encoder if available
	 */
	HTTPEncoder getHTTPParameterFormatter();
	
	/**
	 * enable url encoding
	 * @param value of hte HTTPEncoder
	 */
	void setHTTPParameterFormatter(HTTPEncoder value);
	
	
	/**
	 * @return true if url encoding is enabled
	 */
	boolean isURLEncodingEnabled();

	/**
	 * @return true of content type  multipart/form-data
	 */
	boolean isMultipartFormDataEnabled();

	/**
	 * enable url encoding
	 * @param value true enable URL Encoding
	 */
	//void setURLEncodingEnabled(boolean value);
	
	
	
	/**
	 * Set the request payload or content
	 * @param payload as bytes of the http message
	 */
	void setContent(byte[] payload);

	/**
	 * Set the request payload or content
	 * @param payload as string
	 */
	void setContent(String payload);
	
	
	/**
	 * Get the request payload or content
	 * @return content
	 */
	byte[] getContent();
	
	/**
	 * @return content length
	 */
	int getContentLength();
	/**
	 * Set the content length
	 * @param length in bytes
	 */
	void setContentLength(int length);

	/**
	 * @return the multipart boundary 
	 */
	String getBoundary();

	/**
	 * This is an optional parameter that is set by the http call
	 * in case of a multipart post
	 * @param boundary separator for multipart message
	 */
	void setBoundary(String boundary);
	
	/**
	 * @return true if redirect is enabled
	 */
	boolean isRedirectEnabled();

	/**
	 * If true allow request redirection
	 * @param redirectEnabled true
	 */
	void setRedirectEnabled(boolean redirectEnabled);
	


	/**
	 * If true ssl check will be enabled, if the connection is a secure connection the remote server certificate will be checked.
	 * If false ssl check will be disabled, this mode should be used for a self-signed server certificate connections
	 * @return true if enabled, false disabled
	 */
	boolean isSecureCheckEnabled();

	/**
	 * Set the ssl check status
	 * @param sslCheck false ssl check will be disabled, true ssl check will validate the ssl certificate
	 */
	void setSecureCheckEnabled(boolean sslCheck);

	/**
	 * The connect timeout in seconds before throwing an exception, 0 to disable
	 *
	 * @return connection timeout in seconds
	 */
	long getTimeout();

	/**
	 * Set the connection timeout is sedonds
	 * @param connectTimeout in  seconds
	 */
	void setTimeout(long connectTimeout);
	
	/**
	 * The read timeout in millis seconds before throwing an exception, 0 to disable
	 * 
	 * @return the read timeout in millis
	 */
//	int getReadTimeout();
	
	/**
	 * Set the read timeout is millis
	 * @param readTimeout
	 */
//	void setReadTimeout(int readTimeout);

	/**
	 * Get the encoding to be used for the parameter, if null default will be used
	 * @return charset
	 */
	String getCharset();


	/**
	 * 
	 * Set the charset	 
	 * @param charset
	 */
	void setCharset(String charset);
	



	
	/**
	 * 
	 * @return HTTPAuthorization
	 */
	HTTPAuthorization getAuthorization();
	
	void setAuthorization(HTTPAuthorization httpAuthentication);

	void setBasicAuthorization(String user, String password);
	
	/**
	 * @return the proxy address null if not set
	 */
	IPAddress getProxyAddress();
	
	/**
	 * Set the proxy address
	 * @param proxyAddress
	 */
	void setProxyAddress(IPAddress proxyAddress);
	
	/**
	 * @return reason
	 */
	String getReason();
	
	void setReason(String reason);
	
	/**
	 * 
	 * Get the header content type
	 * 
	 * @return content type
	 */
	String getContentType();
	
	/**
	 * Set the header content type
	 * 
	 * @param contentType
	 */
	void setContentType(String contentType);


	String getAccept();
	void setAccept(String ...accept);

	void setAccept(GetValue<String> ...accept);
	
	/**
	 * 
	 * Set the header content type
	 * 
	 * @param contentType
	 */
	void setContentType(GetValue<String> ...contentType);
	
	/**
	 * Return the Cookie header value
	 * @return cookie
	 */
	String getCookie();
	
	/**
	 * Set cookie
	 * @param cookieValue
	 */
	void setCookie(String cookieValue);
	
	/**
	 * Set cookie
	 * @param cookieValue
	 */
	void setCookie(GetValue<String> cookieValue);
	
	/**
	 * @return HTTPVersion
	 */
	HTTPVersion getHTTPVersion();

	/**
	 * @param version HTTP version 1.1 or 1.0
	 */
	void setHTTPVersion(String version);

	/**
	 *
	 * @param version HTTP version 1.1 or 1.0
	 */
	void setHTTPVersion(HTTPVersion version);

	/**
	 * @param status set the status code
	 */
	void setHTTPStatusCode(HTTPStatusCode status);

	/**
	 * @return the status code of the message
	 */
	HTTPStatusCode getHTTPStatusCode();

	/**
	 * @return if true is case of HTTP error code and io exception should be thrown
	 */
	boolean isHTTPErrorAsException();

	/**
	 * @param errorAsException set the status of http error as exception
	 */
	void setHTTPErrorAsException(boolean errorAsException);


	/**
	 * Lookup http header that has any of the matching values if a header do not exist or no matching values were found in the header values
	 * this method will return null
	 * @param headerName to look for
	 * @param valuesToMatch to match
	 * @return the header if the return conditions where met otherwise null
	 */
	GetNameValue<String> lookupMatchingHeader(GetName headerName, String ...valuesToMatch);

	/**
	 * Lookup http header that has any of the matching values if a header do not exist or no matching values were found in the header values
	 * this method will return null
	 * @param headerName to look for
	 * @param valuesToMatch to match
	 * @return the header if the return conditions where met otherwise null
	 */
	GetNameValue<String> lookupMatchingHeader(String headerName, String ...valuesToMatch);


	/**
	 * Set the user agent of the caller
	 * @param userAgent to be set
	 */
	void setUserAgent(String ...userAgent);

	/**
	 * Return the user agent
	 * @return user agent
	 */
	String getUserAgent();


	HTTPMessageConfigInterface setHeader(String headerName, String ...values);

	HTTPMessageConfigInterface setHeader(GetName headerName, String ...values);

	HTTPMessageConfigInterface setHeader(String headerName, GetValue<String> ...values);

	HTTPMessageConfigInterface setHeader(GetName headerName, GetValue<String> ...values);


}
