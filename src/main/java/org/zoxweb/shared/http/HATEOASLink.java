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

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * [Please state the purpose for this class or method because it will help the team for future maintenance ...].
 * 
 */
@SuppressWarnings("serial")
public class HATEOASLink
extends SetNameDAO
{
	public enum Params
	implements GetNVConfig
	{
		HREF(NVConfigManager.createNVConfig("href", "URL of the related HATEOAS link you can use for subsequent calls.", "Href", true, true, String.class)),
		REL(NVConfigManager.createNVConfig("rel", "TLink relation that describes how this link relates to the previous call.", "Rel", true, true, String.class)),
		METHOD(NVConfigManager.createNVConfig("method", "The HTTP method required for the related call.", "Method", true, true, String.class)),
		
	
		;
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	
	}

	public static final NVConfigEntity NVC_HATEOAS_LINK = new NVConfigEntityPortable("hateos_link", null , "HATEOASLink", true, false, false, false, HATEOASLink.class, SharedUtil.extractNVConfigs(Params.values()), null, false, SetNameDAO.NVC_NAME_DAO);
	
	
	public HATEOASLink()
	{
		super(NVC_HATEOAS_LINK);
	}
	
	
	public String getHRef()
	{
		return lookupValue(Params.HREF);
	}
	
	public void setHRef(String href)
	{
		setValue(Params.HREF, href);
	}
	
	public String getRel()
	{
		return lookupValue(Params.REL);
	}
	
	public void setRel(String rel)
	{
		setValue(Params.HREF, rel);
	}
	
	public String getMethod()
	{
		return lookupValue(Params.METHOD);
	}
	
	public void setMethod(String method)
	{
		setValue(Params.METHOD, method);
	}
}
