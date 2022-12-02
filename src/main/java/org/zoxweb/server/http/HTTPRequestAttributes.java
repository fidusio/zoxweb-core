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

import java.util.List;

import org.zoxweb.server.io.FileInfoStreamSource;
import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.shared.http.HTTPAuthentication;
import org.zoxweb.shared.http.HTTPAuthenticationBasic;
import org.zoxweb.shared.http.HTTPAuthenticationBearer;
import org.zoxweb.shared.http.HTTPAuthorizationType;
import org.zoxweb.shared.http.HTTPHeader;
import org.zoxweb.shared.http.HTTPMimeType;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTToken;
import org.zoxweb.shared.util.AppIDURI;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGetNameValueList;
import org.zoxweb.shared.util.NVPairGetNameMap;
import org.zoxweb.shared.util.OutputDataDecoder;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

public class HTTPRequestAttributes 
{

    public static final String HRA = "HRA";

	//private final List<GetNameValue<String>> headers;
	private final NVPairGetNameMap headers;
	private final NVGetNameValueList parameters;
	//private final List<GetNameValue<String>> parameters;
	
	/**
	 * The caller must close the streams
	 */
	private final List<FileInfoStreamSource> streamList;
	private final boolean isMultiPart;
	private String content = null;
	private OutputDataDecoder<String> contentDecoder = null;
	private final String contentType;
	private final HTTPAuthentication httpAuthentication;
	private final String pathInfo;
	private final String uri;
	private AppIDURI appIDURI;
	private String subjectID = null;
	
	private Object contentObject = null;
	
	private JWTToken jwtToken = null;
	
	public HTTPRequestAttributes(String uri,
            String pathInfo,
			 String contentType,
			 boolean isMultiPart,
			 List<GetNameValue<String>> headers, 
			 List<GetNameValue<String>> parameters, 
			 List<FileInfoStreamSource> streamList,
			 String content)
	{
		this(uri, pathInfo, contentType, isMultiPart, headers, parameters, streamList, content, null);
	}
	
	public HTTPRequestAttributes(String uri,
            String pathInfo,
			 String contentType,
			 boolean isMultiPart,
			 List<GetNameValue<String>> headers, 
			 List<GetNameValue<String>> parameters, 
			 List<FileInfoStreamSource> streamList,
			 OutputDataDecoder<String> dataDecoder)
	{
		this(uri, pathInfo, contentType, isMultiPart, headers, parameters, streamList, null, dataDecoder);
	}
	
	public HTTPRequestAttributes(String uri,
            String pathInfo,
			 String contentType,
			 boolean isMultiPart,
			 List<GetNameValue<String>> headers, 
			 List<GetNameValue<String>> parameters, 
			 List<FileInfoStreamSource> streamList)
	{
		this(uri, pathInfo, contentType, isMultiPart, headers, parameters, streamList, null, null);
	}
	
	
	
	@SuppressWarnings("unchecked")
	private HTTPRequestAttributes(String uri,
                                 String pathInfo,
								 String contentType,
								 boolean isMultiPart,
								 List<GetNameValue<String>> headers, 
								 List<GetNameValue<String>> parameters, 
								 List<FileInfoStreamSource> streamList,
								 String content, OutputDataDecoder<String> dataDecoder)
	{
	    this.uri = uri;
		this.pathInfo = pathInfo;
		this.isMultiPart = isMultiPart;
		this.headers = new NVPairGetNameMap("headers", headers);
		this.parameters = new NVGetNameValueList ("parameters", parameters);
		this.streamList = streamList;
		this.content = content;
		this.contentType = contentType;
		this.contentDecoder = dataDecoder;

		if (headers != null)
		{
			HTTPAuthentication temp = HTTPAuthorizationType.parse((GetNameValue<String>) SharedUtil.lookup(headers, HTTPHeader.AUTHORIZATION));
			
			if (temp != null && temp instanceof HTTPAuthenticationBearer)
			{
				try
				{
					JWT jwt = CryptoUtil.parseJWT(((HTTPAuthenticationBearer)temp).getToken());
					jwtToken = new JWTToken();
					jwtToken.setJWT(jwt);
					jwtToken.setToken(((HTTPAuthenticationBearer)temp).getToken());
					subjectID = jwt.getPayload().getSubjectID();
				}
				catch (Exception e)
				{
					
				}
			}
			else if (temp != null && temp instanceof HTTPAuthenticationBasic)
			{
				subjectID = ((HTTPAuthenticationBasic)temp).getUser();
			}
			
			httpAuthentication = temp;//HTTPAuthorizationType.parse((GetNameValue<String>) SharedUtil.lookup(headers, HTTPHeaderName.AUTHORIZATION));
		}
		else
		{
			httpAuthentication = null;
		}
		
		try
		{
			appIDURI = AppIDURI.parse(pathInfo);
		}
		catch(Exception e)
		{
			
		}
	}



	/**
	 * Get the incoming request headers
	 * 
	 * @return the headers
	 */
	public ArrayValues<GetNameValue<String>> getHeaders()
	{
		return headers;
	}
	
	/**
	 * Get the incoming request parameters
	 * 
	 * @return parameters
	 */
	public  ArrayValues<GetNameValue<String>> getParameters()
	{
		return parameters;
	}
	
	/**
	 * 
	 * @return true is the  request is a multipart request
	 */
	public boolean isMultiPart()
	{
		return isMultiPart;
	}
	
	
	public AppIDURI getAppIDURI()
	{
		return appIDURI;
	}
	
	/**
	 * 
	 * @return the HTTPAuthentication 
	 */
	public HTTPAuthentication getHTTPAuthentication()
	{
		return httpAuthentication;
	}
	
	/**
	 * Return the list of file information and stream information if the request has embedded multipart files
	 * 
	 * @return stream list
	 */
	public List<FileInfoStreamSource> getFileInfoStreamSources()
	{
		return streamList;
	}

	public String getContent()
	{
		if(content == null && contentDecoder != null)
		{
			synchronized(this)
			{
				if(content == null && contentDecoder != null)
				{
					content = contentDecoder.decode();
				}
			}
		}
		return content;
	}
	
	/**
	 * 
	 * @return the content type of the request
	 */
	public String getContentType()
	{
		return contentType;
	}
	
	
	public HTTPMimeType getContentMimeType()
	{
		return HTTPMimeType.lookup(getContentType());
	}
	
	/**
	 * Returns any extra path information associated with the URL the client sent when it made this request. The extra path information follows the servlet path but precedes the query string and will start with a "/" character.
	 * This method returns null if there was no extra path information. 
	 * (Optional)
	 * @return path info 
	 */
	public String getPathInfo()
	{
		return pathInfo;
	}
	
	
	public JWTToken getJWTToken()
	{
		return jwtToken;
	}

	public String getURI() 
	{
	    return uri;
    }

	
	public String getPathInfoTokenByIndex(int index, boolean caseInsensitive)
	{
		return SharedStringUtil.getTokenByIndex(getPathInfo(), "/", index, caseInsensitive);
//		String params[] = SharedStringUtil.parseStringLenient(getPathInfo(), "/");
//		if (index < params.length)
//		{
//			return caseInsensitive ? params[index].toLowerCase() : params[index];
//		}
//
//		return null;
	}


	@SuppressWarnings("unchecked")
	public <V extends Object> V getDataContent() 
	{
		return (V) contentObject;
	}



	public void setDataContent(Object contentObject) 
	{
		this.contentObject = contentObject;
	}
	
	public String getSubjectID()
	{
		return subjectID;
	}
}