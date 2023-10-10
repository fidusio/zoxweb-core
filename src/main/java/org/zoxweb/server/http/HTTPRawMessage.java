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

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPMediaType;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.util.Arrays;

public class HTTPRawMessage 
{
	private final UByteArrayOutputStream ubaos;
	private int endOfHeadersIndex = -1;
	private int parseIndex = 0;
	private String firstLine = null;
	private HTTPMessageConfigInterface hmci = new HTTPMessageConfig();

	public HTTPRawMessage(String msg)
	{
		this(SharedStringUtil.getBytes(msg));
	}
	public HTTPRawMessage(byte[] fullMessage)
	{
		this(fullMessage, 0 , fullMessage.length);
	}
	
	public HTTPRawMessage(byte[] fullMessage, int offset, int len)
	{
		ubaos = new UByteArrayOutputStream(len);
		ubaos.write(fullMessage, offset, len);
	}
	
	public HTTPRawMessage()
	{
		this(new UByteArrayOutputStream());
	}
	

	public HTTPRawMessage(UByteArrayOutputStream ubaos)
	{
		this.ubaos = ubaos;
	}
	
	public String getFirstLine()
	{
		return firstLine;
	}


	public UByteArrayOutputStream getDataStream()
	{
		return ubaos;
	}

	private void parseRawHeaders(boolean client)
	{
		if (endOfHeadersIndex != -1)
		{
			int lineCounter =0;
			while (parseIndex < endOfHeadersIndex)
			{
				int endOfCurrentLine = ubaos.indexOf(parseIndex, Delimiter.CRLF.getBytes());//, 0, ProtocolDelimiter.CRLF.getBytes().length);
				
				if (endOfCurrentLine != -1)
				{
					lineCounter++;
					String oneLine = new String(Arrays.copyOfRange(ubaos.getInternalBuffer(), parseIndex, endOfCurrentLine));

					if (lineCounter > 1)
					{
						GetNameValue<String> gnv = SharedUtil.toNVPair(oneLine, ":", true);
						hmci.getHeaders().add(gnv);
					}
					else
					{
						firstLine = oneLine;
						if(client)
						{
							String[] tokens = getFirstLine().split(" ");
							for (int i = 0; i < tokens.length; i++) {
								String token = tokens[i];
								switch (i) {
									case 0:
										hmci.setMethod(HTTPMethod.lookup(token));
										break;
									case 1:
										hmci.setURI(token);
										break;
									case 2:
										hmci.setHTTPVersion(token);
										break;
								}
							}
							if(hmci.getMethod() == HTTPMethod.GET)
							{
								HTTPDecoder.WWW_URL_ENC.decode(this);
							}
						}
					}
					parseIndex = endOfCurrentLine+ Delimiter.CRLF.getBytes().length;
				}


			}
		}
		
	}


	private HTTPMethod  parseHTTPMethod()
	{
		int firstSpaceIndex = ubaos.indexOf(0, Delimiter.SPACE.getBytes());
		if (firstSpaceIndex != -1)
		{
			String maybeHttpMethod = ubaos.getString(0, firstSpaceIndex);
			// try to parse the method
			HTTPMethod ret =  HTTPMethod.lookup(ubaos.getString(0, firstSpaceIndex));
			if (ret == null)
			{
				throw new IllegalArgumentException("Invalid HTTP method " + maybeHttpMethod);
			}
		}
		else if(ubaos.size() > 25)
		{
			throw new IllegalArgumentException("HTTP method token too big");
		}
		return null;
	}
	
	public synchronized boolean isMessageComplete()
	{
		if (endOfHeadersIndex != -1)
		{
			if (hmci.getContentLength() !=-1)
			{
				return ((endOfHeadersIndex + hmci.getContentLength()  + Delimiter.CRLFCRLF.getBytes().length) == endOfMessageIndex());
			}
			return true;
		}
		return false;
	}



	public int endOfMessageIndex()
	{
		return ubaos.size();
	}
	
	
	public int endOfHeadersIndex()
	{
		return endOfHeadersIndex;
	}
	
	public byte[] getRawHeaders()
	{
		if (endOfHeadersIndex != -1 )
		{
			return Arrays.copyOfRange(ubaos.getInternalBuffer(), 0, endOfHeadersIndex);
		}
		
		return null;
	}

	public synchronized HTTPMessageConfigInterface parse(boolean client)
	{
		if(client)
			parseHTTPMethod();

		if (endOfHeadersIndex() == -1) {
			// detect end of message
			endOfHeadersIndex = ubaos.indexOf(Delimiter.CRLFCRLF.getBytes());

			if (endOfHeadersIndex != -1) {
				parseRawHeaders(client);
			}
		}

		if (client && isMessageComplete())
		{

			if (hmci.getMethod() != HTTPMethod.GET) {

				HTTPMediaType hmt = HTTPMediaType.lookup(hmci.getContentType());
				if (hmt != null) {
					switch (hmt) {

						case APPLICATION_WWW_URL_ENC:
							HTTPDecoder.WWW_URL_ENC.decode(this);
							break;

						case APPLICATION_OCTET_STREAM:
							break;
						case MULTIPART_FORM_DATA:
							HTTPDecoder.MULTIPART_FORM_DATA.decode(this);
							break;
						case TEXT_CSV:

						case TEXT_CSS:

						case TEXT_HTML:

						case TEXT_JAVASCRIPT:

						case TEXT_PLAIN:

						case TEXT_YAML:

						case APPLICATION_JSON:
							hmci.setContent(ubaos.copyBytes(endOfHeadersIndex + Delimiter.CRLFCRLF.getBytes().length));
							break;
						case IMAGE_BMP:
							break;
						case IMAGE_GIF:
							break;
						case IMAGE_JPEG:
							break;
						case IMAGE_PNG:
							break;
						case IMAGE_SVG:
							break;
						case IMAGE_ICON:
							break;
						case IMAGE_TIF:
							break;
					}
				}
			}

		}

		return hmci;
	}

//	public int getContentLength()
//	{
//		return contentLength;
//	}
//
//	public byte[] getRawContent()
//	{
//		if (endOfHeadersIndex !=-1)
//		{
//			return Arrays.copyOfRange(ubaos.getInternalBuffer(), endOfHeadersIndex+4, endOfMessageIndex());
//		}
//
//		return null;
//	}


	public synchronized void reset()
	{
		endOfHeadersIndex = -1;
		parseIndex = 0;
		firstLine = null;
		ubaos.reset();
		hmci = new HTTPMessageConfig();

	}

	public HTTPMessageConfigInterface getHTTPMessageConfig() {
		return hmci;
	}

	@Override
	public String toString()
	{
		return "HTTPRawMessage [endOfMessage=" + endOfHeadersIndex
				+ ", contentLength=" + hmci.getContentLength() + ", headers=" + hmci.getHeaders()
				+ ", firstLine=" + firstLine + ", baos=" +ubaos.size()+"]";
	}

}