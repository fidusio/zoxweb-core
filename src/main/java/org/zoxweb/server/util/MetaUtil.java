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


import org.zoxweb.shared.util.*;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.UUID;

public class MetaUtil
{
	//public static final MetaUtil SINGLETON = new MetaUtil();

	private static HashMap<String, NVConfigEntity> classNameToNVCE = new HashMap<String, NVConfigEntity>();
	//private static MetaUtil tempObj = new MetaUtil();

	private MetaUtil()
    {

	}
	
	public static synchronized  NVConfigEntity fromClass(String className)
			throws ClassNotFoundException,
			InstantiationException,
			IllegalAccessException,
			NullPointerException,
			IllegalArgumentException,
			SecurityException, NoSuchMethodException, InvocationTargetException {
		SUS.checkIfNulls("Null class name", className);
		NVConfigEntity nvce = classNameToNVCE.get(className);

		if (nvce == null)
		{
			nvce = fromClass(Class.forName(className));
			classNameToNVCE.put(className, nvce);
		}
		
		return nvce;
	}

	public static NVConfigEntity fromClass(Class<?> clazz)
			throws InstantiationException, IllegalAccessException, NullPointerException, IllegalArgumentException, NoSuchMethodException, SecurityException, InvocationTargetException {
		SUS.checkIfNulls("Null class name", clazz);
		Object obj = clazz.getConstructor().newInstance();

		if (obj instanceof NVEntity)
		{
			NVEntity nve = (NVEntity) obj;
			return (NVConfigEntity) nve.getNVConfig();
		}
		
		throw new IllegalArgumentException("class:" + clazz.getName() + " is not derived from NVEntity");
	}
	
	public static void initCanonicalUUID(NVEntity nve)
    {
		if (nve != null)
		{
			if (nve instanceof SetCanonicalID)
			{
				SetCanonicalID sci = (SetCanonicalID) nve;

				if (sci.getCanonicalID() == null)
				{
					sci.setCanonicalID(UUID.randomUUID().toString());
				}
			}
		}
	}
	
	public static void initTimeStamp(NVEntity nve)
    {
		if (nve != null)
		{
			if (nve instanceof TimeStampInterface)
			{
				TimeStampInterface tsd = (TimeStampInterface) nve;

				if (tsd.getCreationTime() == 0)
				{
					tsd.setCreationTime(System.currentTimeMillis());
				}
				
				if (tsd.getLastTimeUpdated() == 0)
				{
					tsd.setLastTimeUpdated(System.currentTimeMillis());
				}
			}
		}		
	}
	
	public static void updateReadTimeStamp(NVEntity nve)
    {
		if (nve != null)
		{
			if (nve instanceof TimeStampInterface)
			{
				TimeStampInterface tsd = (TimeStampInterface) nve;
				tsd.setLastTimeRead(System.currentTimeMillis());
			}
		}		
	}
	
	public static void updateUpdateTimeStamp(NVEntity nve)
    {
		if (nve != null)
		{
			if (nve instanceof TimeStampInterface)
			{
				TimeStampInterface tsd = (TimeStampInterface) nve;
				tsd.setLastTimeUpdated(System.currentTimeMillis());
			}
		}		
	}


}