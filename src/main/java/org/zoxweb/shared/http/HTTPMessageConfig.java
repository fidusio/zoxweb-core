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

import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.List;


import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVGetNameValueList;
import org.zoxweb.shared.util.NVPairGetNameMap;
import org.zoxweb.shared.util.SetNameValue;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * This class is configuration object that contains all the required parameters to perform 
 * <p>
 * HTTTP Task such as:
 * <ul>
 * <li> do an http submit
 * <li> do an http query
 * <li> do a http rest invocation
 * </ul
 * </p> 
 * @author javaconsigliere
 *
 */

@SuppressWarnings("serial")
public class HTTPMessageConfig
    extends SetNameDescriptionDAO
    implements HTTPMessageConfigInterface
{

	


	public enum Params
	implements GetNVConfig
	{
		URL(NVConfigManager.createNVConfig("url", "The http URL", "URL", false, true, String.class)),
		URI(NVConfigManager.createNVConfig("uri", "The http URI", "URI", false, true, String.class)),
		CHARSET(NVConfigManager.createNVConfig("charset", "The character set", "Charset", false, true, String.class)),
		BOUNDARY(NVConfigManager.createNVConfig("boundary", "The mutltipart boundary", "Boundary", false, true, String.class)),
		CONNECTION_TIMEOUT(NVConfigManager.createNVConfig("connection_timeout", "The connection timeout in millis (<= 0 for ever)", "ConnectionTimeout", false, true, Integer.class)),
		READ_TIMEOUT(NVConfigManager.createNVConfig("read_timeout", "The read timeout in millis (<= 0 for ever)", "ReadTimeout", false, true, Integer.class)),
		REDIRECT_ENABLED(NVConfigManager.createNVConfig("redirect_enabled", "The redirect enabled", "RedirectEnabled", false, true, Boolean.class)),
		MULTI_PART_ENCODING(NVConfigManager.createNVConfig("mutli_part_encoding", "The multipart encoding", "MultiPartEncoding", false, true, Boolean.class)),
		HTTP_METHOD(NVConfigManager.createNVConfig("http_method", "The http method", "HTTPMethod", false, true, HTTPMethod.class)),
		HTTP_VERSION(NVConfigManager.createNVConfig("http_version", "The http version", "HTTPVersion", false, true, HTTPVersion.class)),
		HTTP_STATUS_CODE(NVConfigManager.createNVConfig("http_status_code", "The http status code", "HTTPStatusCode", false, true, HTTPStatusCode.class)),
		HEADER_PARAMETERS(NVConfigManager.createNVConfig("headers_parameters", "The header parameters", "HeaderParameters", false, true, true, String[].class, null)),
		REASON(NVConfigManager.createNVConfig("reason", "The server reaso", "Reason", false, true, String.class)),
		USER(NVConfigManager.createNVConfig("user", "The user name", "User", false, true, String.class)),
		PASSWORD(NVConfigManager.createNVConfig("password", "The user name password", "Password", false, true, String.class)),
		AUTHENTICATION(NVConfigManager.createNVConfigEntity("authentication", "The http authentication", "Authentication", false, true, HTTPAuthentication.class, ArrayType.NOT_ARRAY)),
		PARAMETERS(NVConfigManager.createNVConfig("parameters", "parameters", "Parameters", false, true, false, String[].class, null)),
		PROXY_ADDRESS(NVConfigManager.createNVConfigEntity("proxy_address", "The proxy address if not null","ProxyAddress",true, false, InetSocketAddressDAO.class, ArrayType.NOT_ARRAY)),
		ENABLE_ENCODING(NVConfigManager.createNVConfig("enable_encoding", "The NVP will be url encoded", "EnableEncoding", false, true, Boolean.class)),
		ENABLE_SECURE_CHECK(NVConfigManager.createNVConfig("enable_secure_check", "If the connection is secure, certificte will be valiated", "EnableSecureCheck", false, true, Boolean.class)),
		HTTP_PARAMETER_FORMATTER(NVConfigManager.createNVConfig("http_parameter_formatter", "The NVP parameter formatter", "HTTPParameterFormatter", false, true, HTTPParameterFormatter.class)),
		
		CONTENT(NVConfigManager.createNVConfig("content", "The payload content", "Content", false, true, byte[].class)),
		//CONTENT_LENGTH(NVConfigManager.createNVConfig("content_length", "The payload content length", "ContentLength", false, true, Integer.class)),
		;
	
		private final NVConfig cType;
		Params( NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}

	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on AddressDAO.
	 */
	public static final NVConfigEntity NVC_HTTP_MESSAGE_CONFIG = new NVConfigEntityLocal("http_message_config",
																					  null ,
																					  "HTTPMessageConfig",
																					  true,
																					  false,
																					  false,
																					  false,
																					  HTTPMessageConfig.class,
																					  SharedUtil.extractNVConfigs(Params.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);
		
	
	
	
	/**
	 * Explicit definition to make the class a java bean
	 */
	public HTTPMessageConfig()
	{
		this(NVC_HTTP_MESSAGE_CONFIG);
	}
	
	
	
	protected HTTPMessageConfig(NVConfigEntity nvce)
	{
		super(nvce);
		setRedirectEnabled(true);
		setURLEncodingEnabled(true);
		setSecureCheckEnabled(true);
		setHTTPParameterFormatter(HTTPParameterFormatter.URL_ENCODED);
		// updating PARAMETERS and HEADER_PARAMETERS to NVGetNameValueMap
		// reason to support multi-parts parameters
		attributes.put(Params.PARAMETERS.getNVConfig().getName(), new NVGetNameValueList(Params.PARAMETERS.getNVConfig().getName(), new ArrayList<GetNameValue<String>>()));
		attributes.put(Params.HEADER_PARAMETERS.getNVConfig().getName(), new NVPairGetNameMap(Params.HEADER_PARAMETERS.getNVConfig().getName(), new LinkedHashMap<GetName, GetNameValue<String>>()));
	}
	

	public boolean isMultiPartEncoding() 
	{
		
		GetNameValue<String> mp = getHeaderParameters().get(HTTPHeaderName.CONTENT_TYPE.getName());
		if (mp != null && mp.getValue() != null)
		{
			if (SharedStringUtil.contains(HTTPMimeType.MULTIPART_FORM_DATA.getValue(), mp.getValue(), true))
			{
				return true;
			}
		}
		
		return lookupValue(Params.MULTI_PART_ENCODING);
	}


	public void setMultiPartEncoding(boolean multiPartEncoding) 
	{
		setValue(Params.MULTI_PART_ENCODING, multiPartEncoding);
	}
	
	
	/**
	 * Get the action parameters as an array list of NVPairs.
	 * The parameters sequence should be preserved during invocation 
	 * @return parameters 
	 */
	@SuppressWarnings("unchecked")
	public synchronized ArrayValues<GetNameValue<String>> getParameters() 
	{
		return (ArrayValues<GetNameValue<String>>) lookup(Params.PARAMETERS);
	}

	/**
	 * Set the action parameters list
	 * @param params
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setParameters(List<GetNameValue<String>> params) 
	{
		ArrayValues<GetNameValue<String>> parameters = ((ArrayValues<GetNameValue<String>>)lookup(Params.PARAMETERS));
		if (params == null || params.size() == 0)
		{
			parameters.clear();
			return;
		}
		
		for (GetNameValue<String> gnv: params)
		{
			parameters.add(gnv);
		}
	}
	
	/**
	 * Set the HTTP request parameters
	 * @return headers
	 */
	@SuppressWarnings("unchecked")
	public synchronized ArrayValues<GetNameValue<String>> getHeaderParameters() 
	{
		return  (ArrayValues<GetNameValue<String>>) lookup(Params.HEADER_PARAMETERS);
	}

	/**
	 * Get the HTTP request parameters
	 * @param headerParams
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setHeaderParameters(List<GetNameValue<String>> headerParams) 
	{
		ArrayValues<GetNameValue<String>> headerParameters = ((ArrayValues<GetNameValue<String>>)lookup(Params.HEADER_PARAMETERS));
		if (headerParams == null || headerParams.size() == 0)
		{
			headerParameters.clear();
			return;
		}
		for (GetNameValue<String> gnv: headerParams)
		{
			headerParameters.add(gnv);
		}
	}

	/**
	 * Get the action type
	 * @return http method
	 */
	public HTTPMethod getMethod() 
	{
		return lookupValue(Params.HTTP_METHOD);
	}
	
	/**
	 * Set the action type
	 * @param httpMethod
	 */
	public void setMethod(HTTPMethod httpMethod) 
	{
		setValue(Params.HTTP_METHOD, httpMethod);
	}
	
	
	public void setMethod(String method) 
	{
	
		for (HTTPMethod m : HTTPMethod.values())
		{
			if (m.getName().equalsIgnoreCase(method))
			{
				setMethod(m);
				break;
			}
		}
		
	}
	
	/**
	 * Get the URI extension
	 * @return URI
	 */
	public String getURI() 
	{
		return lookupValue(Params.URI);
	}
	
	/**
	 * Set the URI extension
	 * @param uri
	 */
	public void setURI(String uri) 
	{
		setValue(Params.URI, uri);
	}
	
	/**
	 * Get the URL
	 * @return URL
	 */
	public String getURL() 
	{
		return lookupValue(Params.URL);
	}
	
	/**
	 * Set the URL
	 * @param url
	 */
	public void setURL(String url) 
	{
		setValue(Params.URL, url);
	}


	
	/**
	 * Set the request content
	 * @param content
	 */
	public void setContent(byte[] content)
	{
		setValue(Params.CONTENT, content);
	}
	
	/**
	 * Set the request content
	 * @param content
	 */
	public void setContent(String content)
	{
		setContent(SharedStringUtil.getBytes(content));
	}
	
	/**
	 * Get the request payload or content
	 * @return content 
	 */
	public byte[] getContent()
	{
		return lookupValue(Params.CONTENT);
	}

	/**
	 * @return the multipart boundary 
	 */
	public String getBoundary()
	{
		return lookupValue(Params.BOUNDARY);
	}

	/**
	 * This is a optional parameter that is set by the http call 
	 * in case of mutlitpart post
	 * @param boundary
	 */
	public void setBoundary(String boundary)
	{
		setValue(Params.BOUNDARY, boundary);
	}
	
	public static HTTPMessageConfigInterface createAndInit(String url, String uri, HTTPMethod method)
	{
		return createAndInit(url, uri, method, true);
	}
	public static HTTPMessageConfigInterface createAndInit(String url, String uri, HTTPMethod method, boolean sslCheck)
	{
		HTTPMessageConfigInterface ret = new HTTPMessageConfig();
		ret.setURL(url);
		ret.setURI(uri);
		ret.setMethod(method);
		ret.setSecureCheckEnabled(sslCheck);
		return ret;
	}
	
	
	public static HTTPMessageConfigInterface createAndInit(String url, String uri, String method)
	{
		return createAndInit(url, uri, method, true);
	}
	public static HTTPMessageConfigInterface createAndInit(String url, String uri, String method, boolean sslCheck)
	{
		HTTPMessageConfigInterface ret = new HTTPMessageConfig();
		ret.setURL(url);
		ret.setURI(uri);
		ret.setMethod(method);
		ret.setSecureCheckEnabled(sslCheck);
		return ret;
	}
	
//	@Override
//	public String toString() 
//	{
//		return "HTTPCallConfig [isMultiPartEncoding()=" + isMultiPartEncoding()
//				+ ", getParameters()=" + getParameters() + ", getMethod()="
//				+ getMethod() + ", getURI()=" + getURI() + ", getURL()="
//				+ getURL() + ", getHeaderParameters()=" + getHeaderParameters()
//				+ ", getContent()=" + (getContent() != null ? new String(getContent()) : "null")
//				+ ", getBoundary()=" + getBoundary() + ", getName()="
//				+ getName() + ", isRedirectEnabled()=" + isRedirectEnabled()
//				+ ", getConnectTimeout()=" + getConnectTimeout()
//				+ ", getReadTimeout()=" + getReadTimeout() + "]";
//	}
	
	
	
	@Override
	public String toString() {
		return "HTTPCallConfig [isMultiPartEncoding()=" + isMultiPartEncoding()
				+ ", getParameters()=" + getParameters()
				+ ", getHeaderParameters()=" + getHeaderParameters()
				+ ", getMethod()=" + getMethod() + ", getURI()=" + getURI()
				+ ", getURL()=" + getURL() + ", getContent()="
				+  (getContent() != null ? new String(getContent()) : "null") + ", getBoundary()="
				+ getBoundary() + ", isRedirectEnabled()="
				+ isRedirectEnabled() + ", getConnectTimeout()="
				+ getConnectTimeout() + ", getReadTimeout()="
				+ getReadTimeout() + ", getCharset()=" + getCharset()
				+ ", getProxyAddress()=" + getProxyAddress() + ", getUser()="
				+ getUser() + ", getPassword()=" + getPassword()
				+ ", getAuthentitcation()=" + getAuthentitcation() + "]";
	}


	
	public boolean isRedirectEnabled()
	{
		return lookupValue(Params.REDIRECT_ENABLED);
	}

	public void setRedirectEnabled(boolean redirectEnabled) 
	{
		setValue(Params.REDIRECT_ENABLED, redirectEnabled);
	}
	
	/**
	 * The connect timeout in millis seconds before throwing an exception, 0 to disable
	 * 
	 * @return connection timeout
	 */
	public int getConnectTimeout() 
	{
		return lookupValue(Params.CONNECTION_TIMEOUT);
	}

	@Override
	public boolean isSecureCheckEnabled()
	{
		return lookupValue(Params.ENABLE_SECURE_CHECK);
	}

	@Override
	public void setSecureCheckEnabled(boolean sslCheck)
	{
		setValue(Params.ENABLE_SECURE_CHECK, sslCheck);
	}


	public void setConnectTimeout(int connectTimeout) 
	{
		if ( connectTimeout < 0)
		{
			connectTimeout = 0;
		}
		
		setValue(Params.CONNECTION_TIMEOUT, connectTimeout);
		
	}
	/**
	 * The read timeout in millis seconds before throwing an exception, 0 to disable
	 * 
	 * @return read timeout
	 */
	public int getReadTimeout() 
	{
		return lookupValue(Params.READ_TIMEOUT);
	}


	public void setReadTimeout(int readTimeout) 
	{
		if ( readTimeout < 0)
		{
			readTimeout = 0;
		}
		
		setValue(Params.READ_TIMEOUT, readTimeout);
	}


	/**
	 * Get the encoding to be used for the parameter, if null default will be used
	 * @return charset
	 */
	public String getCharset() 
	{
		return lookupValue(Params.CHARSET);
	}



	public void setCharset(String charset) 
	{
		setValue(Params.CHARSET, charset);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getProxyAddress()
	 */
	@Override
	public InetSocketAddressDAO getProxyAddress() {
		
		return lookupValue(Params.PROXY_ADDRESS);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setProxyAddress(org.zoxweb.shared.net.InetSocketAddressDAO)
	 */
	@Override
	public void setProxyAddress(InetSocketAddressDAO proxyAddress)
	{
		
		setValue(Params.PROXY_ADDRESS, proxyAddress);
	}



	/**
	 */
	@Override
	public String getUser() {
		
		return lookupValue(Params.USER);
	}



	/**
	 */
	@Override
	public void setUser(String user) 
	{
		
		setValue(Params.USER, user);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getPassword()
	 */
	@Override
	public String getPassword() {
		
		return lookupValue(Params.PASSWORD);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setPassword(java.lang.String)
	 */
	@Override
	public void setPassword(String password) {
		
		setValue(Params.PASSWORD, password);
		
	}

	


	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getAuthentitcation()
	 */
	@Override
	public HTTPAuthentication getAuthentitcation() {
		
		return lookupValue(Params.AUTHENTICATION);
	}



	/**
	 */
	@Override
	public void setAuthentication(HTTPAuthentication httpAuthentication) {
		
		setValue(Params.AUTHENTICATION, httpAuthentication);
	}



	/**
	 * 
	 */
	@Override
	public HTTPParameterFormatter getHTTPParameterFormatter() {
		
		return lookupValue(Params.HTTP_PARAMETER_FORMATTER);
	}



	/**
	 * 
	 */
	public void setHTTPParameterFormatter(HTTPParameterFormatter value) {
		
		setValue(Params.HTTP_PARAMETER_FORMATTER, value);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getContentType()
	 */
	@Override
	public String getContentType()
	{
		
		return SharedUtil.getValue(getHeaderParameters().get(HTTPHeaderName.CONTENT_TYPE.getName()));
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setContentType(java.lang.String)
	 */
	@Override
	public void setContentType(String contentType)
	{
		
		getHeaderParameters().add(HTTPHeaderName.toHTTPHeader(HTTPHeaderName.CONTENT_TYPE, contentType));
	}

	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setContentType(org.zoxweb.shared.util.GetValue)
	 */
	@Override
	public void setContentType(GetValue<String> contentType)
	{
		
		getHeaderParameters().add(HTTPHeaderName.toHTTPHeader(HTTPHeaderName.CONTENT_TYPE, contentType));
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getCookie()
	 */
	@Override
	public String getCookie()
	{
		
		return SharedUtil.getValue(getHeaderParameters().get(HTTPHeaderName.COOKIE.getName()));
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setCookie(java.lang.String)
	 */
	@Override
	public void setCookie(String cookieValue)
	{
		
		getHeaderParameters().add(HTTPHeaderName.toHTTPHeader(HTTPHeaderName.COOKIE, cookieValue));
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setCookie(org.zoxweb.shared.util.GetValue)
	 */
	@Override
	public void setCookie(GetValue<String> cookieValue)
	{
		
		getHeaderParameters().add(HTTPHeaderName.toHTTPHeader(HTTPHeaderName.COOKIE, cookieValue));
		
	}


	/**
	 * Value -1 not set
	 */
	@Override
	public int getContentLength() 
	{
		
		String contentValue = SharedUtil.getValue(getHeaderParameters().get(HTTPHeaderName.CONTENT_LENGTH.getName()));
		if (contentValue != null)
		{
			return Integer.parseInt(contentValue);
		}
		else
		{
			byte content[] = getContent();
			if (content != null)
			{
				return content.length;
			}
		}
		
		return -1;
	}



	@Override
	public synchronized void setContentLength(int length) 
	{
		
		
		SetNameValue<String> ct = (SetNameValue<String>) getHeaderParameters().get(HTTPHeaderName.CONTENT_LENGTH.getName());
		if (ct != null)
		{
			ct.setValue("" + length);
		}
		else
		{
			getHeaderParameters().add(HTTPHeaderName.toHTTPHeader(HTTPHeaderName.CONTENT_LENGTH, "" + length));
		}
		
	}



	@Override
	public HTTPVersion getHTTPVersion()
	{
		
		return lookupValue(Params.HTTP_VERSION);
	}



	@Override
	public void setHTTPVersion(String version)
	{
		
		setHTTPVersion(HTTPVersion.lookup(version));
	}



	@Override
	public void setHTTPVersion(HTTPVersion version) 
	{
		
		setValue(Params.HTTP_VERSION, version);
	}
	
	@Override
	public HTTPStatusCode getHTTPStatusCode()
	{
		
		return lookupValue(Params.HTTP_STATUS_CODE);
	}
	
	@Override
	public void setHTTPStatusCode(HTTPStatusCode hStatus) 
	{
		
		setValue(Params.HTTP_STATUS_CODE, hStatus);
	}



	@Override
	public String getReason() {
		
		return lookupValue(Params.REASON);
	}



	@Override
	public void setReason(String reason) {
		
		setValue(Params.REASON, reason);
	}



	@Override
	public boolean isURLEncodingEnabled() {
		// TODO Auto-generated method stub
		return  lookupValue(Params.ENABLE_ENCODING);
	}



	@Override
	public void setURLEncodingEnabled(boolean value) {
		// TODO Auto-generated method stub
		setValue(Params.ENABLE_ENCODING, value);
	}
	
}
