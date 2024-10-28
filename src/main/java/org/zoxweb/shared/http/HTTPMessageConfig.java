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

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

/**
 * This class is configuration object that contains all the required parameters to perform 
 * <p>
 * HTTP Task such as:
 * <ul>
 * <li> do http submit
 * <li> do http query
 * <li> do http rest invocation
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
		BOUNDARY(NVConfigManager.createNVConfig("boundary", "The multipart boundary", "Boundary", false, true, String.class)),
		CONNECTION_TIMEOUT(NVConfigManager.createNVConfig("connection_timeout", "The connection timeout in millis (<= 0 for ever)", "ConnectionTimeout", false, true, Integer.class)),
		READ_TIMEOUT(NVConfigManager.createNVConfig("read_timeout", "The read timeout in millis (<= 0 for ever)", "ReadTimeout", false, true, Integer.class)),
		REDIRECT_ENABLED(NVConfigManager.createNVConfig("redirect_enabled", "The redirect enabled", "RedirectEnabled", false, true, Boolean.class)),
		//MULTI_PART_ENCODING(NVConfigManager.createNVConfig("multi_part_encoding", "The multipart encoding", "MultiPartEncoding", false, true, Boolean.class)),
		HTTP_METHOD(NVConfigManager.createNVConfig("http_method", "The http method", "HTTPMethod", false, true, HTTPMethod.class)),
		HTTP_VERSION(NVConfigManager.createNVConfig("http_version", "The http version", "HTTPVersion", false, true, HTTPVersion.class)),
		HTTP_STATUS_CODE(NVConfigManager.createNVConfig("http_status_code", "The http status code", "HTTPStatusCode", false, true, HTTPStatusCode.class)),
		HEADERS(NVConfigManager.createNVConfig("headers", "The header parameters", "HeaderParameters", false, true, NVGenericMap.class)),
		REASON(NVConfigManager.createNVConfig("reason", "The server reason", "Reason", false, true, String.class)),
		AUTHORIZATION(NVConfigManager.createNVConfigEntity("http_authorization", "The http authorization header", "HTTPAuthorization", false, true, HTTPAuthorization.class, ArrayType.NOT_ARRAY)),
		//PARAMETERS(NVConfigManager.createNVConfig("parameters", "parameters", "Parameters", false, true, false, String[].class, null)),
		PARAMETERS(NVConfigManager.createNVConfig("parameters", "parameters", "Parameters", false, true, NVGenericMap.class)),
		PROXY_ADDRESS(NVConfigManager.createNVConfigEntity("proxy_address", "The proxy address if not null","ProxyAddress",true, false, IPAddress.class, ArrayType.NOT_ARRAY)),
		//ENABLE_ENCODING(NVConfigManager.createNVConfig("enable_encoding", "The NVP will be url encoded", "EnableEncoding", false, true, Boolean.class)),
		ENABLE_SECURE_CHECK(NVConfigManager.createNVConfig("enable_secure_check", "If the connection is secure, certificate will be validated", "EnableSecureCheck", false, true, Boolean.class)),
		HTTP_PARAMETER_FORMATTER(NVConfigManager.createNVConfig("http_parameter_formatter", "The NVP parameter formatter", "HTTPParameterFormatter", false, true, HTTPEncoder.class)),
		ERROR_AS_EXCEPTION(NVConfigManager.createNVConfig("error_as_exception", "In case of processing error throw IOException", "ErrorAsException", false, true, Boolean.class)),
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
		setHTTPParameterFormatter(HTTPEncoder.URL_ENCODED);
		setHTTPErrorAsException(true);
		// updating PARAMETERS and HEADER_PARAMETERS to NVGetNameValueMap
		// reason to support multi-parts parameters
		//attributes.put(Params.PARAMETERS.getNVConfig().getName(), new NVGetNameValueList(Params.PARAMETERS.getNVConfig().getName(), new ArrayList<GetNameValue<String>>()));
		//attributes.put(Params.HEADER_PARAMETERS.getNVConfig().getName(), new NVPairGetNameMap(Params.HEADER_PARAMETERS.getNVConfig().getName(), new LinkedHashMap<GetName, GetNameValue<String>>()));

	}
	

	public boolean isMultiPartEncoding() 
	{
		String mp = getHeaders().getValue(HTTPHeader.CONTENT_TYPE);
		//GetNameValue<String> mp = getHeaders().get(HTTPHeaderName.CONTENT_TYPE.getName());
		if (mp != null)// && mp.getValue() != null)
		{
			return SharedStringUtil.contains(mp, HTTPMediaType.MULTIPART_FORM_DATA.getValue(), true);
		}
		
		return false;//lookupValue(Params.MULTI_PART_ENCODING);
	}


//	public void setMultiPartEncoding(boolean multiPartEncoding)
//	{
//		setValue(Params.MULTI_PART_ENCODING, multiPartEncoding);
//	}
	
	
	/**
	 * Get the action parameters as an array list of NVPairs.
	 * The parameters sequence should be preserved during invocation 
	 * @return parameters 
	 */
	public NVGenericMap getParameters()
	{
		return (NVGenericMap) lookup(Params.PARAMETERS);
	}

//	@Override
//	public NVGenericMap getParametersNVGM() {
//		return (NVGenericMap) lookup(Params.PARAMETERS);
//	}

	/**
	 * Set the action parameters list
	 * @param params list of parameters and string nv pairs
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setParameters(List<GetNameValue<String>> params) 
	{
		ArrayValues<GetNameValue<String>> parameters = ((ArrayValues<GetNameValue<String>>)lookup(Params.PARAMETERS));
		if (params == null || params.isEmpty())
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
	public NVGenericMap getHeaders()
	{
		return (NVGenericMap) lookup(Params.HEADERS);
	}


	/**
	 * Set the HTTP request parameters
	 * @param headerParams as list
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setHeaders(List<GetNameValue<String>> headerParams)
	{
		ArrayValues<GetNameValue<String>> headerParameters = ((ArrayValues<GetNameValue<String>>)lookup(Params.HEADERS));
		if (headerParams == null || headerParams.isEmpty())
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
	 * @param httpMethod the http requested method
	 */
	public void setMethod(HTTPMethod httpMethod) 
	{
		setValue(Params.HTTP_METHOD, httpMethod);
	}
	
	
	public void setMethod(String method) 
	{
		HTTPMethod httpMethod = SharedUtil.lookupEnum(method, HTTPMethod.values());
		setMethod(httpMethod);
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
	 * @param uri part of the request or path
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
		//setContentLength(content != null ? content.length  : 0);
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
	 * This is an optional parameter that is set by the http call
	 * in case of multipart post
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

	
	
	
	@Override
	public String toString() {
		return "HTTPCallConfig [isMultiPartEncoding()=" + isMultiPartEncoding()
				+ ", getParameters()=" + getParameters()
				+ ", getHeaders()=" + getHeaders()
				+ ", getMethod()=" + getMethod() + ", getURI()=" + getURI()
				+ ", getURL()=" + getURL() + ", getContent()="
				+  (getContent() != null ? new String(getContent()) : "null") + ", getBoundary()="
				+ getBoundary() + ", isRedirectEnabled()="
				+ isRedirectEnabled() + ", getConnectTimeout()="
				+ getConnectTimeout() + ", getReadTimeout()="
				+ getReadTimeout() + ", getCharset()=" + getCharset()
				+ ", getProxyAddress()=" + getProxyAddress()
//				", getUser()=" + getUser() + ", getPassword()=" + getPassword()
				+ ", getAuthentication()=" + getAuthorization() + "]";
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
	 * The connection timeout in millis seconds before throwing an exception, 0 to disable
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
		if (readTimeout < 0)
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
	public IPAddress getProxyAddress() {
		
		return lookupValue(Params.PROXY_ADDRESS);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setProxyAddress(IPAddress)
	 */
	@Override
	public void setProxyAddress(IPAddress proxyAddress)
	{
		
		setValue(Params.PROXY_ADDRESS, proxyAddress);
	}




	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getAuthorization()
	 */
	@Override
	public synchronized HTTPAuthorization getAuthorization() {
		
		HTTPAuthorization  authorization = lookupValue(Params.AUTHORIZATION);
		if (authorization == null)
		{
			GetNameValue<String> authorizationHeader = getHeaders().lookup(HTTPHeader.AUTHORIZATION);
			if (authorizationHeader != null)
			{
				authorization = HTTPAuthScheme.parse(authorizationHeader);
				if(authorization != null)
					setAuthorization(authorization);
			}
		}

		return  authorization;
	}



	/**
	 */
	@Override
	public synchronized void setAuthorization(HTTPAuthorization httpAuthentication)
	{
		setValue(Params.AUTHORIZATION, httpAuthentication);
	}

	@Override
	public void setBasicAuthorization(String user, String password)
	{
		setAuthorization(new HTTPAuthorizationBasic(user, password));
	}


	/**
	 * 
	 */
	@Override
	public HTTPEncoder getHTTPParameterFormatter() {
		
		return lookupValue(Params.HTTP_PARAMETER_FORMATTER);
	}



	/**
	 * 
	 */
	public void setHTTPParameterFormatter(HTTPEncoder value) {
		
		setValue(Params.HTTP_PARAMETER_FORMATTER, value);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getContentType()
	 */
	@Override
	public String getContentType()
	{
		return getHeaders().getValue(HTTPHeader.CONTENT_TYPE);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setContentType(java.lang.String)
	 */
	@Override
	public void setContentType(String contentType)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.CONTENT_TYPE, contentType));
	}

	@Override
	public String getAccept()
	{
		return getHeaders().getValue(HTTPHeader.ACCEPT);
	}

	@Override
	public void setAccept(String ...accept)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.ACCEPT, accept));
	}

	@Override
	public void setAccept(GetValue<String> ...accept)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.ACCEPT, accept));
	}


	/**
	 * Set the content type
	 * @param contentTypes list of parameters
	 */
	@Override
	public void setContentType(GetValue<String> ...contentTypes)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.CONTENT_TYPE, contentTypes));
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#getCookie()
	 */
	@Override
	public String getCookie()
	{
		return getHeaders().getValue(HTTPHeader.COOKIE);
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setCookie(java.lang.String)
	 */
	@Override
	public void setCookie(String cookieValue)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.COOKIE, cookieValue));
	}



	/**
	 * @see org.zoxweb.shared.http.HTTPMessageConfigInterface#setCookie(org.zoxweb.shared.util.GetValue)
	 */
	@Override
	public void setCookie(GetValue<String> cookieValue)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.COOKIE, cookieValue));
	}


	/**
	 * Value -1 not set
	 */
	@Override
	public int getContentLength() 
	{
		
		String contentValue = getHeaders().getValue(HTTPHeader.CONTENT_LENGTH);///SharedUtil.getValue(getHeaders().get(HTTPHeaderName.CONTENT_LENGTH.getName()));
		if (contentValue != null)
		{
			return Integer.parseInt(contentValue);
		}
		else
		{
			byte[] content = getContent();
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
		
		
		SetNameValue<String> ct = (SetNameValue<String>) getHeaders().get(HTTPHeader.CONTENT_LENGTH.getName());
		if (ct != null)
		{
			ct.setValue("" + length);
		}
		else
		{
			getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.CONTENT_LENGTH, "" + length));
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
	public boolean isHTTPErrorAsException()
	{
		return lookupValue(Params.ERROR_AS_EXCEPTION);
	}

	@Override
	public void setHTTPErrorAsException(boolean errorAsException)
	{
		setValue(Params.ERROR_AS_EXCEPTION, errorAsException);
	}



	@Override
	public void setHTTPStatusCode(HTTPStatusCode hStatus) 
	{
		setValue(Params.HTTP_STATUS_CODE, hStatus);
	}





	@Override
	public String getReason()
	{
		return lookupValue(Params.REASON);
	}



	@Override
	public void setReason(String reason)
	{
		setValue(Params.REASON, reason);
	}



	@Override
	public boolean isURLEncodingEnabled()
	{
		String mp = getHeaders().getValue(HTTPHeader.CONTENT_TYPE);
		if (mp != null)// && mp.getValue() != null)
		{
			return SharedStringUtil.contains(mp, HTTPMediaType.APPLICATION_WWW_URL_ENC.getValue(), true);
		}
		return false;
	}



	@Override
	public void setURLEncodingEnabled(boolean value)
	{
		if(value)
			getHeaders().build(HTTPHeader.CONTENT_TYPE.toHTTPHeader(HTTPMediaType.APPLICATION_WWW_URL_ENC));
	}


	public void setUserAgent(String ...userAgent)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(HTTPHeader.USER_AGENT, userAgent));
	}

	public String getUserAgent()
	{
		return getHeaders().getValue(HTTPHeader.USER_AGENT);
	}


	public HTTPMessageConfigInterface setHeader(String headerName, String ...values)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(headerName, values));
		return this;
	}

	public HTTPMessageConfigInterface setHeader(String headerName, GetValue<String> ...values)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(headerName, values));
		return this;
	}

	public HTTPMessageConfigInterface setHeader(GetName headerName, String ...values)
	{
		return setHeader(headerName.getName(), values);
	}
	public HTTPMessageConfigInterface setHeader(GetName headerName, GetValue<String> ...values)
	{
		getHeaders().add(HTTPConst.toHTTPHeader(headerName, values));
		return this;
	}

	public <V> HTTPMessageConfigInterface setParameter(String name, V value)
	{
		NVGenericMap parameters = getParameters();
		if (name != null)
		{
			if (value != null)
			{
				if(value instanceof String)
				{
					parameters.add(name, (String) value);
				}
				else if (value instanceof Integer)
				{
					parameters.add(new NVInt(name, (Integer) value));
				}
				else if (value instanceof Long)
				{
					parameters.add(new NVLong(name, (Long) value));
				}
				else if (value instanceof Boolean)
				{
					parameters.add(new NVBoolean(name, (Boolean) value));
				}
				else if (value instanceof Float)
				{
					parameters.add(new NVFloat(name, (Float) value));
				}
				else if (value instanceof Double)
				{
					parameters.add(new NVDouble(name, (Double) value));
				}
				else if (value instanceof GetNameValue)
				{
					if(value instanceof SetName)
					{
						((SetName) value).setName(name);
					}
					parameters.add((GetNameValue<?>) value);
				}
				else
				{
					throw new IllegalArgumentException("Unsupported value type " + value.getClass() + " for parameter name: " + name);
				}
			}
			else
				parameters.add(name, (String)value);
		}


		return this;
	}
}
