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
package org.zoxweb.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.app.AppVersionDAO;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * The release manager load and parse json release string and streams that represent application release objects
 */
public class ReleaseManager
{

	public static final ReleaseManager SINGLETON = new ReleaseManager();

	private HashMap<String, AppVersionDAO> map = new HashMap<String, AppVersionDAO>();

	private ReleaseManager()
    {
		
	}
	
	public AppVersionDAO load(String dir, String appName)
        throws IOException
    {
		return load(ReleaseManager.class.getResourceAsStream(dir +appName + ".json"));
	}
	
	public AppVersionDAO load(InputStream is)
        throws IOException
    {
		return load(IOUtil.inputStreamToString(is, true));
	}
	
	public synchronized AppVersionDAO load(String json)
    {
		String name = null;
		String description = null;
		int major = 0;
		int minor = 0;
		int nano  = 0;
		
		List<CharSequence> tokens = SharedStringUtil.parseGroup(json, "{", "}", false);
	
		String[] attributes = tokens.get(0).toString().split(",");

		for (String attribute :attributes)
		{
			NVPair nvp = SharedUtil.toNVPair(attribute, ":", false);
			nvp.setName(SharedStringUtil.filterString(nvp.getName(), "\""));
			switch( nvp.getName())
            {
			case "name":
				name = SharedStringUtil.filterString(nvp.getValue(), "\"");
				break;
			case "description":
				description = SharedStringUtil.filterString(nvp.getValue(), "\"");
				break;
			case "major":
				major = Integer.parseInt(nvp.getValue());
				break;
			case "minor":
				minor = Integer.parseInt(nvp.getValue());
				break;
			case "nano":
				nano = Integer.parseInt(nvp.getValue());
			break;
			}
		}

		if (!SharedStringUtil.isEmpty(name))
		{
			AppVersionDAO ret = new AppVersionDAO();
		
			ret.setName(name);
			ret.setDescription(description);
			ret.setMajor(major);
			ret.setMinor(minor);
			ret.setNano(nano);
			map.put(name.toLowerCase(), ret);
		
			return ret;
		}

		return null;
	}
	
	public AppVersionDAO lookup( String name)
    {
		name = SharedStringUtil.toLowerCase(SharedStringUtil.trimOrNull(name));
		return map.get(name);
	}

	public static void main(String... args)
    {
		String DEFAULT_DIR ="/org/zoxweb/conf/";
		
		try
        {
			System.out.println(ReleaseManager.SINGLETON.load( DEFAULT_DIR, "jwput").toCanonicalID());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}