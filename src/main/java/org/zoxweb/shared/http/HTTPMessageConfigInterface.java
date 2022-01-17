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

import java.util.List;

import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.ReferenceID;
import org.zoxweb.shared.util.SetDescription;
import org.zoxweb.shared.util.SetName;

/**
 *
 */
public interface HTTPMessageConfigInterface
extends ReferenceID<String>, SetName, SetDescription
{
	/**
	 * @return true if mutli part encoding
	 */
	boolean isMultiPartEncoding();

	/**
	 * @param multiPartEncoding true to enable multi part encoding
	 */
	//void setMultiPartEncoding(boolean multiPartEncoding);
	
	
	/**
	 * Get the action parameters as an array list of NVPairs.
	 * The parameters sequence should be preserved during invocation 
	 * @return http parameters
	 */
	ArrayValues<GetNameValue<String>> getParameters();

	/**
	 * Set the action parameters list
	 * @param params
	 */
	void setParameters(List<GetNameValue<String>> params);

	/**
	 * Get the action type
	 * @return the method
	 */
	HTTPMethod getMethod();
	
	/**
	 * Set the action type
	 * @param httpMethod
	 */
	void setMethod(HTTPMethod httpMethod);
	
	/**
	 * Set the action type
	 * @param httpMethod
	 */
	void setMethod(String httpMethod);
	
	/**
	 * Get the URI extension
	 * @return the uri part
	 */
	String getURI();
	
	/**
	 * Set the URI extension
	 * @param uri
	 */
	void setURI(String uri);
	
	/**
	 * Get the URL
	 * @return url part 
	 */
	String getURL();
	
	
	/**
	 * Set the URL
	 * @param url
	 */
	void setURL(String url);

	/**
	 * Set the HTTP request parameters
	 * @return headers
	 */
	ArrayValues<GetNameValue<String>> getHeaderParameters();

	/**
	 * Get the HTTP request parameters
	 * @param headerParams
	 */
	void setHeaderParameters(List<GetNameValue<String>> headerParams);
	
	/**
	 * @return true if url encoding is enabled
	 */
	HTTPEncoder getHTTPParameterFormatter();
	
	/**
	 * enable url encoding
	 * @param value
	 */
	void setHTTPParameterFormatter(HTTPEncoder value);
	
	
	/**
	 * @return true if url encoding is enabled
	 */
	boolean isURLEncodingEnabled();
	
	/**
	 * enable url encoding
	 * @param value
	 */
	void setURLEncodingEnabled(boolean value);
	
	
	
	/**
	 * Set the request payload or content
	 * @param payload
	 */
	void setContent(byte[] payload);

	/**
	 * Set the request payload or content
	 * @param payload
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
	 * @param length
	 */
	void setContentLength(int length);

	/**
	 * @return the multipart boundary 
	 */
	String getBoundary();

	/**
	 * This is a optional parameter that is set by the http call 
	 * in case of mutlitpart post
	 * @param boundary
	 */
	void setBoundary(String boundary);
	
	/**
	 * @return true if redirect is enabled
	 */
	boolean isRedirectEnabled();

	void setRedirectEnabled(boolean redirectEnabled);
	
	/**
	 * The connect timeout in millis seconds before throwing an exception, 0 to disable
	 * 
	 * @return connection timeout in millis
	 */
	int getConnectTimeout();


	/**
	 * If true ssl check will be enabled, if the connection is a secure connection the remote server certificate will be checked.
	 * If false ssl check will be disabled, this mode should be used for selfsigned server certificate connections
	 * @return true if enabled, false disabled
	 */
	boolean isSecureCheckEnabled();

	/**
	 * Set the ssl check status
	 * @param sslCheck
	 */
	void setSecureCheckEnabled(boolean sslCheck);

	
	/**
	 * Set the connection timeout is millis 
	 * @param connectTimeout
	 */
	void setConnectTimeout(int connectTimeout);
	
	/**
	 * The read timeout in millis seconds before throwing an exception, 0 to disable
	 * 
	 * @return the read timeout in millis
	 */
	int getReadTimeout();
	
	/**
	 * Set the read timeout is millis
	 * @param readTimeout
	 */
	void setReadTimeout(int readTimeout);

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
	 * @return the user
	 */
	String getUser();
	
	/**
	 * Set the user
	 * @param user
	 */
	void setUser(String user);
	
	/**
	 * @return user password
	 */
	String getPassword();
	
	/**
	 * Set user password
	 * @param password
	 */
	void setPassword(String password);


	
	/**
	 * 
	 * @return HTTPAuthentication
	 */
	HTTPAuthentication getAuthentication();
	
	void setAuthentication(HTTPAuthentication httpAuthentication);
	
	/**
	 * @return the proxy address null if not set
	 */
	InetSocketAddressDAO getProxyAddress();
	
	/**
	 * Set the proxy address
	 * @param proxyAddress
	 */
	void setProxyAddress(InetSocketAddressDAO proxyAddress);
	
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
	
	/**
	 * 
	 * Set the header content type
	 * 
	 * @param contentType
	 */
	void setContentType(GetValue<String> contentType);
	
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
	
	void setHTTPVersion(String version);
	
	void setHTTPVersion(HTTPVersion version);
	
	
	void setHTTPStatusCode(HTTPStatusCode status);
	
	HTTPStatusCode getHTTPStatusCode();
	
	
}
