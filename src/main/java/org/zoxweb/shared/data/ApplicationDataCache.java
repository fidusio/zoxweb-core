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
package org.zoxweb.shared.data;

import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApplicationDataCache
{
	
	public static class NVEntityManager
		implements CRUDOperation<NVEntity>
	{
		protected HashMap<String, NVEntity> nveCache = new HashMap<String, NVEntity>();

		private NVEntityManager()
		{
			
		}
		
		@Override
		public void applyCRUD(CRUD crud, NVEntity nve) 
		{
			if (crud != null && nve != null && nve.getReferenceID() != null)
			{
				switch(crud)
				{
				case DELETE:
					nveCache.remove(nve.getReferenceID());
					break;
				case CREATE:
				case READ:
				case UPDATE:
					nveCache.put(nve.getReferenceID(), nve);
					break;
				default:
					break;	
				}
			}	
		}
		
		public void clear()
		{
			nveCache.clear();
		}
		
		public NVEntity lookup( String refID)
		{
			return nveCache.get( refID);
		}
		
		public List<NVEntity> lookupInstancesByType(String classType)
		{
			List<NVEntity> ret = new ArrayList<NVEntity>();
			
			if (classType != null)
			{
				for (NVEntity nve : nveCache.values())
				{
					NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
					
					if (nvce.getMetaTypeBase().getName().equals(classType) || nvce.getName().equals(classType))
					{
						ret.add(nve);
					}
				}
			}
			
			return (ret.size() == 0 ? null : ret);
		}
	}
	
	public class NVBaseManager
		implements CRUDOperation<NVBase<?>>
	{
		
		private NVBaseManager()
		{
			
		}
		
		protected HashMap<String, NVBase<?>> nvbCache = new HashMap<String, NVBase<?>>();
		
		@Override
		public void applyCRUD(CRUD crud, NVBase<?> nvb) 
		{
			if (crud != null && nvb != null && nvb.getReferenceID() != null)
			{
				switch(crud)
				{
				case DELETE:
					nvbCache.remove(nvb.getReferenceID());
					if (nvb instanceof DynamicEnumMap)
					{
						// remove the dynamic enum map
						DynamicEnumMapManager.SINGLETON.deleteDynamicEnumMap(((DynamicEnumMap) nvb).getName());
					}
					break;
				case CREATE:
				case READ:
				case UPDATE:
					nvbCache.put(nvb.getReferenceID(), nvb);
					if (nvb instanceof DynamicEnumMap)
					{
						// update the dynaic enum map
						DynamicEnumMapManager.SINGLETON.addDynamicEnumMap((DynamicEnumMap) nvb);

					}
					break;
				default:
					break;	
				}
			}	
		}
		
		public void clear()
		{
			nvbCache.clear();
		}
		
		public NVBase<?> lookup( String refID)
		{
			return nvbCache.get( refID);
		}
	}
	
	
	protected NVEntityManager nveManager = new NVEntityManager();
	protected NVBaseManager  nvbManager = new NVBaseManager();
	
	public NVEntityManager getNVEntityManager()
	{
		return nveManager;
	}
	
	public NVBaseManager getNVBaseManager()
	{
		return nvbManager;
	}
	
}