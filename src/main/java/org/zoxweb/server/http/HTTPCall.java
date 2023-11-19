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
package org.zoxweb.server.http;

import org.zoxweb.server.io.CloseEnabledInputStream;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.NetUtil;
import org.zoxweb.server.net.ssl.SSLCheckDisabler;
import org.zoxweb.server.net.ssl.SSLSocketProp;
import org.zoxweb.shared.filters.ReplacementFilter;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HTTPCall 
{

	public final static RateCounter HTTP_CALLS = new RateCounter("HTTP_CALLS", "This counter is used the send static functions it capture the whole call duration including parameter formatting, call, result decoding.");

	public final static AtomicBoolean ENABLE_HTTP = new AtomicBoolean(true);
	private final HTTPMessageConfigInterface hcc;
	private final String urlOverride;
	private final ReplacementFilter uriFilter;
	
	private SSLSocketProp ssp;
	private final OutputStream osBypass;
	private final CloseEnabledInputStream contentAsIS;

	static
	{
		try
		{
			HTTPUtil.addHTTPMethods();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public HTTPCall(HTTPMessageConfigInterface params)
	{
		this( params, null, null, null, null, null);
	}
	
	public HTTPCall(HTTPMessageConfigInterface params, SSLSocketProp ssp)
	{
		this( params, ssp, null, null, null, null);
	}

	public HTTPCall(HTTPMessageConfigInterface params,
					SSLSocketProp ssp,
					String urlOverride,
					ReplacementFilter uriFilter,
					OutputStream osBypass,
					CloseEnabledInputStream contentAsIS)
	{
		SharedUtil.checkIfNulls("HTTPActionParameters can't be null", params);
		this.hcc = params;
		this.urlOverride = urlOverride;
		this.uriFilter = uriFilter;
		this.ssp = ssp;
		this.osBypass = osBypass;
		//this.embedParamsInURI = embedParamsInURI;
		this.contentAsIS = contentAsIS;

		// if the ssl override  is not set but secure check is disabled then set
		// it to the default SSLCheckDisabler
		if (this.ssp == null && !params.isSecureCheckEnabled())
		{
			this.ssp = SSLCheckDisabler.SINGLETON;
		}
	}


	public boolean isSecure()
	{
		if (fullURL().toLowerCase().startsWith("https:"))
		{
			return true;
		}
		
		return false;
	}
	/**
	 * 
	 * [Please state the purpose for this class or method because it will help the team for future maintenance ...].
	 * 
	 * @return HTTPResponseData
	 * @throws IOException in case of error
	 */
	public HTTPResponseData sendRequest() throws IOException
	{
		long ts = System.currentTimeMillis();
		ByteArrayOutputStream ret = null;
		int status;
		Map<String, List<String>> respHeaders = null;
		// format the payload first
		String encodedContentParams = HTTPUtil.formatParameters(hcc.getParameters().asArrayValuesString(), hcc.getCharset(), hcc.isURLEncodingEnabled(), hcc.getHTTPParameterFormatter());
		String urlURI = fullURL();

		boolean embedPostPutParamsInURI = false;
		GetNameValue<String> contentType = hcc.getParameters().asArrayValuesString().get(HTTPHeader.CONTENT_TYPE.getName());
		if (contentType != null && contentType.getValue() != null && contentType.getValue().toLowerCase().indexOf("x-www-form-urlencoded") == -1)
		{
			embedPostPutParamsInURI = true;
		}
		
		if (encodedContentParams.length() > 0)
		{
			switch(hcc.getMethod())
			{
			
			case POST:
			case PUT:
				if (!embedPostPutParamsInURI || hcc.isMultiPartEncoding())
				{
					HTTPMultiPartUtil.preMultiPart(hcc);
					break;
				}
			case GET:
			case DELETE:
			case PATCH:
			case PURGE:
			case CONNECT:
			case COPY:
			case LINK:
			case LOCK:
			case PROPFIND:
			case TRACE:
			case UNLINK:
			case UNLOCK:
			case VIEW:
				// if we have a GET
				if (!SharedStringUtil.isEmpty(encodedContentParams))
				{
					if (hcc.getHTTPParameterFormatter() == HTTPEncoder.URL_ENCODED)
						urlURI += "?" + encodedContentParams;
					else if (hcc.getHTTPParameterFormatter() == HTTPEncoder.URI_REST_ENCODED)
					{
						urlURI = SharedStringUtil.concat(urlURI, encodedContentParams, "" + HTTPEncoder.URI_REST_ENCODED.getValue());
					}
				}
				break;
			default:
				break;
			
					
			}
		}
		URL url = new URL(urlURI);
		OutputStream os = null;
		InputStream is = null;
		InputStream isError = null;
		HttpURLConnection con = null;
		Proxy proxy = null;
		if (hcc.getProxyAddress() != null)
		{
			Proxy.Type pType= NetUtil.lookup(hcc.getProxyAddress().getProxyType());
			if (pType != Proxy.Type.DIRECT)
				proxy = new Proxy(NetUtil.lookup(hcc.getProxyAddress().getProxyType()), new InetSocketAddress(hcc.getProxyAddress().getInetAddress(),  hcc.getProxyAddress().getPort()));
		}

		if(!ENABLE_HTTP.get())
		{
			return null;
		}
		try
		{
			if (proxy != null)
			{
				con = (HttpURLConnection) url.openConnection(proxy);
			}
			else
			{
				con = (HttpURLConnection) url.openConnection();
			}
			
	
			if ( ssp != null)
				ssp.updateURLConnection(con);
			con.setRequestMethod(hcc.getMethod().toString());
			
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setAllowUserInteraction(true);
			con.setInstanceFollowRedirects(hcc.isRedirectEnabled());
			con.setConnectTimeout(hcc.getConnectTimeout());
			con.setReadTimeout(hcc.getReadTimeout());
			
			
			
		
			ArrayValues<GetNameValue<String>> reqHeaders = hcc.getHeaders().asArrayValuesString();
			// set the request headers
			if (reqHeaders != null)
			{
				for (GetNameValue<String> nvp : reqHeaders.values())
				{
					con.setRequestProperty(nvp.getName(), nvp.getValue());
				}
			}
			
			
			// set the authentication from url or hcc
			// if url.getUserInfo() is not null ?
			// if hcc.getUser() && hcc.getPassword() != null
			// if still null check if the HTTPAuthentication is not null
			GetNameValue<String> authorizationHeader = HTTPAuthScheme.BASIC.toHTTPHeader(url.getUserInfo(), null);

			if(authorizationHeader == null && hcc.getAuthorization() != null)
			{
				authorizationHeader = hcc.getAuthorization().toHTTPHeader();
			}

			if (authorizationHeader != null)
			{
				con.setRequestProperty(authorizationHeader.getName(), authorizationHeader.getValue());
			}

			
			// write the payload if it is a post
			if (!embedPostPutParamsInURI)
			{
				switch (hcc.getMethod())
				{
				case POST:
				case PUT:
					os = con.getOutputStream();
					if (hcc.isMultiPartEncoding())
					{
						HTTPMultiPartUtil.writeMultiPartContent(os, hcc.getBoundary(), hcc.getParameters().asArrayValuesString());
					}
					else
						os.write(encodedContentParams.getBytes());
					break;
					default:
						
				}
			}
			
			
			

			
			if (hcc.getContent() != null && hcc.getContent().length > 0)
			{
				if (os == null)
				{
					os = con.getOutputStream();
				}
				
				os.write(hcc.getContent());
			}
			if (contentAsIS != null)
			{
				if ( os == null)
				{
					os = con.getOutputStream();
				}
				IOUtil.relayStreams(contentAsIS, os, false);
			}
			
			
			IOUtil.flush(os);
			status = (proxy != null && proxy.type() == Proxy.Type.SOCKS) ? 200 : con.getResponseCode();
			
			// check if we have an error in the response
			isError = con.getErrorStream();
			if ( isError != null)
			{
				ret  = IOUtil.inputStreamToByteArray(isError, false);
				respHeaders = con.getHeaderFields();
				HTTPResponseData hrd = new HTTPResponseData(status, respHeaders, ret.toByteArray(), System.currentTimeMillis() -ts );
				if (hcc.isHTTPErrorAsException())
					throw new HTTPCallException(con.getResponseMessage(),  hrd);
				else
					return hrd;
			}
			
			HTTPStatusCode hsc = HTTPStatusCode.statusByCode(status);

			if (hsc != null && hcc.isRedirectEnabled())
			{
				switch(hsc)
				{
				case MOVED_PERMANENTLY:
				case FOUND:
				case SEE_OTHER:
					NVPair cookie = HTTPUtil.extractCookie(con.getHeaderFields(), 0);
					if (cookie != null)
						hcc.getHeaders().add(cookie);
					String newURL = con.getHeaderField("Location");
					
					hcc.setURI(null);
					hcc.setURL(newURL);
					
					if (contentAsIS != null)
					{
						throw new HTTPCallException("Can not forward with ContentAsIS set",  new HTTPResponseData(status, con.getHeaderFields(), null, System.currentTimeMillis() -ts));
					}
					HTTPCall hccRedirect = new HTTPCall(hcc, ssp, urlOverride, uriFilter, osBypass, contentAsIS);
					return hccRedirect.sendRequest();
					
				default:
						
					
				}
			}
			
			
			
			is = con.getInputStream();
			
			
			if ( osBypass == null)
			{
				ret = IOUtil.inputStreamToByteArray(is, false);
			}
			else 
			{
				IOUtil.relayStreams(is, osBypass, false);
			}
			

			
			respHeaders = con.getHeaderFields();
			
			
		}
		finally
		{
			if ( con != null)
			{
			    try {
			      con.disconnect();
			    }
			    catch(Exception e) {}
			}
			
			IOUtil.close(os);
			IOUtil.close(is);
			IOUtil.close(isError);
			IOUtil.close(contentAsIS);
		}
		
		
		HTTPResponseData hrd = new HTTPResponseData(status, respHeaders, ret != null ? ret.toByteArray() : null, System.currentTimeMillis() - ts);


		return hrd;
	}
	
	
	
	public static HTTPResponseData send(HTTPMessageConfigInterface hmci)
			throws IOException
	{
		HTTPResponseData ret;
		try
		{
			ret = new HTTPCall(hmci).sendRequest();
		}
		catch(HTTPCallException e)
		{
			HTTP_CALLS.register(e.getResponseData().getDuration());
			throw e;
		}
		HTTP_CALLS.register(ret.getDuration());
		return ret;
	}

	public static <O> HTTPAPIResult<O> send(HTTPMessageConfigInterface hmci, Class<?> clazz)
			throws IOException
	{

		HTTPResponseData hrd;
		try
		{
			hrd = new HTTPCall(hmci).sendRequest();
		}
		catch(HTTPCallException e)
		{

			HTTP_CALLS.register(e.getResponseData().getDuration());
			throw e;
		}
		HTTPAPIResult<O> ret =  HTTPUtil.toHTTPResponseObject(hrd, clazz);
		HTTP_CALLS.register(hrd.getDuration());
		return ret;
	}

	public static <O> HTTPAPIResult<O> send(HTTPMessageConfigInterface hmci, DataDecoder<HTTPResponseData, O> decoder)
			throws IOException
	{
		HTTPResponseData hrd ;
		try
		{
			hrd = new HTTPCall(hmci).sendRequest();
		}
		catch(HTTPCallException e)
		{

			HTTP_CALLS.register(e.getResponseData().getDuration());
			throw e;
		}
		O result = decoder.decode(hrd);
		HTTPAPIResult<O> ret = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), result, hrd.getDuration());
		HTTP_CALLS.register(ret.getDuration());
		return ret;
	}






	
	private String fullURL()
	{
		
		String ret;
		
		String uri = hcc.getURI();
		if ( uriFilter != null)
		{
			uri = uriFilter.validate( uri);
		}
		
		if ( urlOverride != null)
			ret = SharedStringUtil.concat(urlOverride, uri, "/");
		else
			ret = SharedStringUtil.concat(hcc.getURL(), uri, "/");
		
		
		return ret;
	}



}