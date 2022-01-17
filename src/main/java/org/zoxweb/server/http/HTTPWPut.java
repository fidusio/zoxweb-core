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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.zoxweb.server.security.SSLCheckDisabler;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * JMPut: java mput is a command line form processor
 * @author mnael
 *
 */
public class HTTPWPut 
{

	public static void error( String error)
	{
		if (error != null)
		{
			System.out.println("Error:" + error);
		}
		
		System.out.println("usage: [-dSSL disable SSL check] [destination url] {[name=value parameters]}");
		System.out.println("uploading a file: [-dSSL disable SSL check] [destination url] file.url=filename or [-dSSL disable SSL check] [destination url] file.url=file:///full_path file=filename");
		System.exit(-1);
	}
	
	
	public static HTTPResponseData wPut(URL destURL, 
				 					 	boolean disableSSLCheck, 
				 					 	boolean isMultiPart,
				 					 	String username,
				 					 	String password,
				 					 	List<GetNameValue<String>> headers, 
				 					 	List<GetNameValue<String>> params) throws IOException
	{
		HTTPMessageConfig hcc = new HTTPMessageConfig();
		hcc.setUser(username);
		hcc.setPassword(password);
		hcc.setMethod(HTTPMethod.POST);
		hcc.setParameters(params);
		
		hcc.setURL(destURL.getProtocol() +"://"+ destURL.getHost() );
		hcc.setURI(destURL.getPath());
		if(isMultiPart)
			hcc.setContentType(HTTPMimeType.MULTIPART_FORM_DATA);
		//hcc.setMultiPartEncoding(isMutliPart);

		if (headers != null)
		{
			hcc.setHeaderParameters(headers);
		}

		HTTPCall hc = new HTTPCall( hcc, disableSSLCheck ? SSLCheckDisabler.SINGLETON : null);
		
		//System.out.println( hcc);
		return hc.sendRequest();
	}
	
	public static void main(String ...args)
	{
		ArrayList <GetNameValue<String>> params = new ArrayList<GetNameValue<String>>();
		ArrayList <GetNameValue<String>> hParams = new ArrayList<GetNameValue<String>>();
		
		URL destURL=null;
		boolean disableSSL = false;
		boolean multipart = false;
		String user = null;
		String password = null;
		
		if (args.length <2)
		{
			error("missing parameters");
		}
		
		for (String arg : args)
		{
			String nameval[] = SharedStringUtil.parseNameValue(arg, "=");

			try
			{
				if (nameval.length == 1)
				{
					if ("-dSSL".equalsIgnoreCase(nameval[0]))
					{
						disableSSL = true;
					}
					else
					{
						destURL = new URL(nameval[0]);
					}
				}
				else if (nameval.length == 2)
				{
					if ("-user".equalsIgnoreCase(nameval[0]))
					{
						user = nameval[1];
						break;
					}
					if ("-password".equalsIgnoreCase(nameval[0]))
					{
						password = nameval[1];
						break;
					}
					
					InputStream tempIS = null;
					File tempFile = null;
					URL tempURL = null;

					try
					{
						// support 2 mode local filename or fully qualified url
						tempURL = new URL(nameval[1]);
						multipart = true;
					}
					catch (Exception e)
					{
						//e.printStackTrace();
						
						try
						{
							tempFile = new File(nameval[1]);

							if (tempFile.exists() && tempFile.isFile())
							{
								tempIS  = new BufferedInputStream( new FileInputStream( tempFile));
							}

							multipart = true;
						
						}
						catch (Exception e1)
						{
							e1.printStackTrace();
						}
					}
					
					//GetNameValue<String> gnv = SharedUtil.lookupNV(params, nameval[0], ".");
					
					boolean add = false;
					String sep = null;
					// this is a trick
					// if name has [name].url
					String canonicalName[] = SharedStringUtil.parseNameValue(nameval[0], ".");

					if (canonicalName.length == 2)
					{
						if (canonicalName[1].equalsIgnoreCase("url") && (tempURL != null || tempIS != null))
						{
							sep = ".";
							nameval[0] = canonicalName[0];
						}
						else if (canonicalName[0].equalsIgnoreCase("-h"))
						{
							// we have a header parameter
							nameval[0] = canonicalName[1];
							
							hParams.add( new NVPair( nameval[0], nameval[1]));
							continue;
						}
					}

					HTTPMultiPartParameter p = (HTTPMultiPartParameter)SharedUtil.lookupNV(params, nameval[0], sep);

					if (p == null)
					{
						p = new HTTPMultiPartParameter();
						add = true;
					}
					
					if (tempURL != null || tempIS != null)
					{
						p.setName( nameval[0]);

						String value = p.getValue();
						p.setValue(null);

						if (value != null)
						{
							p.setFileName(value);
						}
						else if (tempURL != null)
						{
							p.setFileName(tempURL.getFile());
						}
						else if (tempFile != null)
						{
							p.setFileName(tempFile.getName());
						}

						if (tempURL != null)
						{
							p.setURL( tempURL);
						}

						if (tempIS != null)
						{
							p.setInputStreamValue(tempIS, true);
						}
					}
					else
					{
						p.setName(nameval[0]);
						
						String value = p.getFileName();
						p.setValue(null);

						if (value != null)
						{
							p.setFileName( nameval[1]);
						}
						else
						{
							p.setValue( nameval[1]);
						}
					}

					if (add)
					{
						params.add( p);	
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (destURL == null)
		{
			error("Missing URL");
		}

		try
		{
			HTTPResponseData rd =  wPut(destURL, disableSSL, multipart, user, password, hParams, params);
			System.out.println("status:" + rd.getStatus());
			System.out.println(new String(rd.getData()) );
		}
		catch (Exception e )
		{
			e.printStackTrace();
			error(null);
		}
	}

 }