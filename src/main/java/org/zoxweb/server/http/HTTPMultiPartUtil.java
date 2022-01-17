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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.protocol.ProtocolDelimiter;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.Const.TimeUnitType;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SharedStringUtil;

public final class HTTPMultiPartUtil
{

	public static final GetNameValue<String> MULTI_PART_HEADER_CONTENT_TYPE = new NVPair(HTTPHeaderName.CONTENT_TYPE.getName(), "multipart/form-data; boundary=");
	private final static Logger log = Logger.getLogger(HTTPMultiPartUtil.class.getName());
	private HTTPMultiPartUtil(){
		log.info("Default ctr");
	}
	
	/**
	 * Generate a boundary string in hex format based on the java time in nano time
	 * @return boundary
	 */
	public static String generateBoundary(TimeUnitType tut)
	{
		if (tut == null)
		{
			tut = TimeUnitType.NANOS;
		}

		switch(tut)
		{
		case MILLIS:
			return "--------" + Long.toString(System.currentTimeMillis(), 16);
		case NANOS:
			return "--------" + Long.toString(System.nanoTime(), 16);
		default:
			throw new IllegalArgumentException("Unit not supported:" + tut);
		
			
		}
	}

//	/**
//	 * Format:boundary
//	 * @param boundary for
//	 * @return formatted boundary
//	 */
//	public static String formatBoundary(String boundary)
//	{
//		return  boundary;
//	}
	
	/**
	 * Format : BOUNDARY_EDGE + boundary + \r\n
	 * @param boundary value
	 * @return --boundary\r\n
	 */
	public static String formatStartBoundary(String boundary)
	{
		return HTTPHeaderValue.BOUNDARY_EDGE + boundary + ProtocolDelimiter.CRLF;
	}
	
	
	/**
	 * Format : BOUNDARY_EDGE + boundary + BOUNDARY_EDGE \r\n
	 * @param boundary value=
	 * @return --boundary--\r\n
	 */
	public static String formatEndBoundary(String boundary)
	{
		return HTTPHeaderValue.BOUNDARY_EDGE + boundary + HTTPHeaderValue.BOUNDARY_EDGE + ProtocolDelimiter.CRLF;
	}
	
	
	public static String formatMultiPartData(GetNameValue<String> gnv)
	{
		if ( gnv instanceof HTTPMultiPartParameter)
		{
			return formatMultiPartParameter((HTTPMultiPartParameter) gnv);
		}
		return formatMultiPartNameValue(gnv.getName(), gnv.getValue());
	}
	
	
	
	
	private static void appendNameValue(StringBuilder sb, GetNameValue<String> gnv)
	{
		if (gnv!= null)
		{
			if (gnv.getName() != null && gnv.getValue() != null)
			{
				sb.append( gnv.getName());
				sb.append( ProtocolDelimiter.EQUAL);
				sb.append( ProtocolDelimiter.DOUBLE_QUOTE);
				sb.append( gnv.getValue());
				sb.append( ProtocolDelimiter.DOUBLE_QUOTE);
			}
			else if (gnv.getName() == null && gnv.getValue() != null)
			{
				sb.append(gnv.getValue());
			}
		}
	}
	
	public static String formatMultiPartParameter(HTTPMultiPartParameter hmpp)
	{
		Map<String, ArrayList<GetNameValue<String>>>  headers = hmpp.getHeaders();
		StringBuilder sb = new StringBuilder();
		if (headers != null)
		{
			ArrayList<GetNameValue<String>> contentDisposition = headers.get(HTTPHeaderName.CONTENT_DISPOSITION.getName());

			sb.append(HTTPHeaderName.CONTENT_DISPOSITION.getName()+ProtocolDelimiter.COLON+ProtocolDelimiter.SPACE+
					   HTTPHeaderValue.FORM_DATA+ProtocolDelimiter.SEMICOLON+ProtocolDelimiter.SPACE+ HTTPHeaderValue.NAME+ProtocolDelimiter.EQUAL +
					   "\"" + hmpp.getName() + "\"" );
			
			if (hmpp.getFileName() != null)
			{
				sb.append( ProtocolDelimiter.SEMICOLON);
				sb.append( ProtocolDelimiter.SPACE);
				sb.append( HTTPHeaderValue.FILENAME );
				sb.append( ProtocolDelimiter.EQUAL);
				sb.append( ProtocolDelimiter.DOUBLE_QUOTE);
				sb.append( hmpp.getFileName());
				sb.append( ProtocolDelimiter.DOUBLE_QUOTE);
			}
			
			if (contentDisposition != null)
			{
				for (GetNameValue<String> gnv: contentDisposition)
				{
					//if ( !"name".equalsIgnoreCase( gnv.getName()))
					{
						sb.append( ProtocolDelimiter.SEMICOLON);
						sb.append( ProtocolDelimiter.SPACE);
						appendNameValue(sb, gnv);
						
					}
				}
				sb.append( ProtocolDelimiter.CRLF);
				
			}
			else
			{
				sb.append( ProtocolDelimiter.CRLF);
			}
			
			Set<Map.Entry<String, ArrayList<GetNameValue<String>>>> allHeaders = headers.entrySet();

			for (Map.Entry<String, ArrayList<GetNameValue<String>>> me : allHeaders)
			{
				if (me.getValue() != contentDisposition)
				{
					sb.append( me.getKey());
					sb.append( ProtocolDelimiter.COLON);
					sb.append( ProtocolDelimiter.SPACE);
					ArrayList<GetNameValue<String>> headersParameters = me.getValue();

					if (headersParameters!= null)
					{
						for (int i = 0; i < headersParameters.size(); i++)
						{
							if (i > 0)
							{
								sb.append( ProtocolDelimiter.SEMICOLON);
								sb.append( ProtocolDelimiter.SPACE);
							}

							appendNameValue(sb, headersParameters.get(i));
						}

						sb.append( ProtocolDelimiter.CRLF);
					}
					else
					{
						sb.append( ProtocolDelimiter.CRLF);	
					}
					
				}
			}
					   // we need to add all the extra fields and headers  here

			sb.append( ProtocolDelimiter.CRLF + 
					   hmpp.getValue() +ProtocolDelimiter.CRLF);
		}

		return sb.toString();
	}

	public static String formatMultiPartNameValue(String name, String value)
	{
		return HTTPHeaderName.CONTENT_DISPOSITION.getName() + ProtocolDelimiter.COLON+ProtocolDelimiter.SPACE +
			   HTTPHeaderValue.FORM_DATA + ProtocolDelimiter.SEMICOLON + ProtocolDelimiter.SPACE + 
			   HTTPHeaderValue.NAME + ProtocolDelimiter.EQUAL + "\"" + name + "\"" +
			   ProtocolDelimiter.CRLFCRLF + value + ProtocolDelimiter.CRLF;
	}

	/**
	 * Format:
	 * BOUNDARY_EDGE + boundary+ \r\n
	 * for every parameter
	 * {
	 * 		BOUNDARY_EDGE + boundary+ \r\n
	 * 		Content-Disposition: form-data; name="name" + \r\n\r\n + value + \r\n
	 * }
	 * BOUNDARY_EDGE + boundary + BOUNDARY_EDGE \r\n
	 * 
	 * @param boundary value
	 * @param params list
	 * @return all content formatted
	 */
	public static String formatMultiPartContent(String boundary, List<GetNameValue<String>> params)
	{
		StringBuilder sb = new StringBuilder();
		
		//sb.append(formatStartBoundary( boundary));
		for (GetNameValue<String> gnv : params)
		{
			sb.append(formatStartBoundary(boundary));
			String data = formatMultiPartData(gnv);
			sb.append(data);
		}
		
		sb.append(formatEndBoundary(boundary));
		
		return sb.toString();
	}
	
	
	public static void writeMultiPartContent(OutputStream os, String boundary, List<GetNameValue<String>> params)
		throws IOException
	{
		for ( GetNameValue<String> gnv : params)
		{
			os.write(formatStartBoundary(boundary).getBytes());
			writeMultiPartData(os, gnv);
		}
		
		os.write(formatEndBoundary(boundary).getBytes());
	}
	
	
	public static void writeMultiPartContent(OutputStream os, String boundary, ArrayValues<GetNameValue<String>> params)
			throws IOException
	{
		//log.info("StartBoundary: " + formatStartBoundary(boundary));
		for (GetNameValue<String> gnv : params.values())
		{
			os.write(formatStartBoundary(boundary).getBytes());
			writeMultiPartData(os, gnv);
		}

		os.write(formatEndBoundary( boundary).getBytes());
		//log.info("EndBoundary: " + formatEndBoundary(boundary));
	}
	

	public static void writeMultiPartData(OutputStream os, GetNameValue<String> gnv)
			throws IOException
	{
		if (gnv instanceof HTTPMultiPartParameter)
		{
			writeMultiPartParameter(os, (HTTPMultiPartParameter) gnv );
			return;
		}
		
		os.write(formatMultiPartNameValue( gnv.getName(), gnv.getValue()).getBytes());
	}
	
	
	public static void writeMultiPartParameter(OutputStream os, HTTPMultiPartParameter hmpp)
			throws IOException
	{
		Map<String, ArrayList<GetNameValue<String>>>  headers = hmpp.getHeaders();
		StringBuilder sb = new StringBuilder();

		if (headers != null)
		{
			ArrayList<GetNameValue<String>> contentDisposition = headers.get(HTTPHeaderName.CONTENT_DISPOSITION.getName());
			
			
			sb.append(HTTPHeaderName.CONTENT_DISPOSITION.getName()+ProtocolDelimiter.COLON+ProtocolDelimiter.SPACE+
					   HTTPHeaderValue.FORM_DATA+ProtocolDelimiter.SEMICOLON+ProtocolDelimiter.SPACE+ HTTPHeaderValue.NAME+ProtocolDelimiter.EQUAL +
					   "\"" + hmpp.getName() + "\"" );
			
			if (hmpp.getFileName() != null)
			{
				sb.append( ProtocolDelimiter.SEMICOLON);
				sb.append( ProtocolDelimiter.SPACE);
				sb.append( HTTPHeaderValue.FILENAME );
				sb.append( ProtocolDelimiter.EQUAL);
				sb.append( ProtocolDelimiter.DOUBLE_QUOTE);
				sb.append( hmpp.getFileName());
				sb.append( ProtocolDelimiter.DOUBLE_QUOTE);
			}
			
			if (contentDisposition != null)
			{
				for (GetNameValue<String> gnv: contentDisposition)
				{
					//if ( !"name".equalsIgnoreCase( gnv.getName()))
					{
						sb.append( ProtocolDelimiter.SEMICOLON);
						sb.append( ProtocolDelimiter.SPACE);
						appendNameValue(sb, gnv);
					}
				}

				sb.append( ProtocolDelimiter.CRLF);
			}
			else
			{
				sb.append( ProtocolDelimiter.CRLF);
			}
			
			Set<Map.Entry<String, ArrayList<GetNameValue<String>>>> allHeaders = headers.entrySet();

			for (Map.Entry<String, ArrayList<GetNameValue<String>>> me : allHeaders)
			{
				if (me.getValue() != contentDisposition)
				{
					sb.append( me.getKey());
					sb.append( ProtocolDelimiter.COLON);
					sb.append( ProtocolDelimiter.SPACE);
					ArrayList<GetNameValue<String>> headersParameters = me.getValue();

					if (headersParameters!= null)
					{
						for (int i = 0; i < headersParameters.size(); i++)
						{
							if (i > 0)
							{
								sb.append( ProtocolDelimiter.SEMICOLON);
								sb.append( ProtocolDelimiter.SPACE);
								
							}
							appendNameValue(sb, headersParameters.get(i));
						}

						sb.append( ProtocolDelimiter.CRLF);
					}
					else
					{
						sb.append( ProtocolDelimiter.CRLF);	
					}
				}
			}
					   // we need to add all the extra fields and headers  here
			//log.info(sb.toString());
			os.write(sb.toString().getBytes());
					   
			os.write(ProtocolDelimiter.CRLF.getBytes());
			
			//hmpp.getValue()
			if (hmpp.getInputStreamValue() != null)
			{
				InputStream is = hmpp.getInputStreamValue();
				byte[] buffer = new byte[512];
				int read;

				try
				{
					while((read = is.read( buffer)) != -1)
					{
						os.write( buffer, 0, read);
					}
				}
				finally
				{
					if (hmpp.isAutoClose())
					{
						IOUtil.close(is);
					}
				}
			}
			else if (hmpp.getContentValue() != null)
			{
				os.write(hmpp.getContentValue());
			}
			else if (hmpp.getValue() != null)
			{
				os.write(SharedStringUtil.getBytes(hmpp.getValue()));
				//log.info("Value: " + hmpp.getValue());
			}
					   
			os.write( ProtocolDelimiter.CRLF.getBytes());
						
		}
	}
	
	public static  boolean  preMultiPart(HTTPMessageConfigInterface hcc)
	{
		if (hcc.isMultiPartEncoding() && (hcc.getMethod() == HTTPMethod.POST || hcc.getMethod() == HTTPMethod.PUT))
		{
//			GetNameValue<String> ct = null;
//			String contentType = hcc.getContentType();
			if (!SharedStringUtil.contains(hcc.getContentType(), "boundary=" , true))
			{
				hcc.setBoundary(generateBoundary(TimeUnitType.NANOS));
				// add the boundary parameter to the request header
				hcc.getHeaderParameters().add(new NVPair(HTTPHeaderName.CONTENT_TYPE, MULTI_PART_HEADER_CONTENT_TYPE.getValue() + hcc.getBoundary()));

			}
//			if (contentType.equalsIgnoreCase(MULTI_PART_HEADER_CONTENT_TYPE.getValue()))
//			if (hcc.getHeaderParameters() != null)
//			{
//				ct = hcc.getHeaderParameters().get(MULTI_PART_HEADER_CONTENT_TYPE.getName());
//			}
//
//			if (ct == null)
//			{
//				// set the boundary
//				hcc.setBoundary(generateBoundary(TimeUnitType.NANOS));
//				// add the boundary parameter to the request header
//				hcc.getHeaderParameters().add(new NVPair(MULTI_PART_HEADER_CONTENT_TYPE.getName(), MULTI_PART_HEADER_CONTENT_TYPE.getValue() + hcc.getBoundary()));
//			}

			return true;
		}
		
		return false;
	}
	
}