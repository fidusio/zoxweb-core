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

import java.io.FileInputStream;
import java.util.List;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.security.SSLCheckDisabler;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPResponseData;

/**
 *
 */
public class HTTPCallProcessor 
{
	
	private static void error(String str)
	{
		if (str != null)
		{
			System.out.println("Error message:" + str);
		}
		
		System.out.println("Usage:" + HTTPCallProcessor.class.getCanonicalName() + " [http config json file ...]");
		System.exit(-1);
	}
	
	public static void main(String... args)
	{
		if (args.length == 0)
		{
			error("Invalid parameter length " + args.length);
		}

		for (int i = 0 ; i < args.length; i++)
		{
			try
			{
				List<HTTPMessageConfig> list = GSONUtil.fromJSONs(IOUtil.inputStreamToString(new FileInputStream(args[i]), true), HTTPMessageConfig.class);
				for (HTTPMessageConfig hcc : list)
				{
					System.out.println("Calling:" + hcc.getURL() + "/" + hcc.getURI());
					try
					{
						HTTPResponseData rd = new HTTPCall(hcc, SSLCheckDisabler.SINGLETON).sendRequest();
						System.out.println("Result:" + rd.getStatus());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
	
			}
			catch (Exception e)
			{
				e.printStackTrace();
				error(null);
			}
		}
	}
}
