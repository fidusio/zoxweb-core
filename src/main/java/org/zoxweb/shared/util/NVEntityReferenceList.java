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
package org.zoxweb.shared.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class declares NVBase as a list of NVEntity type. 
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVEntityReferenceList 
	extends NVBase<List<NVEntity>>
	implements ArrayValues<NVEntity>
	
{
	//private NVConfigEntity nvce;
	
	/**
	 * The default constructor (Java Bean compliant).
	 */
	public NVEntityReferenceList()
	{
		
	}
	
	/**
	 * This constructor instantiates NVEntityReferenceList based on value of NVConfig type.
	 * @param name
	 */
	public NVEntityReferenceList(String name)
	{
		super( name, new ArrayList<NVEntity>());
		//nvce = (NVConfigEntity) config;
	}

	
	public NVEntity get(GetName getName)
	{
		if (getName != null && getName.getName() != null)
		{
			return get(getName.getName());
		}
		
		return null;
	}
	

	
	/**
	 * Get nve entity based on name or ReferenceID
	 * @param name or refid
	 * @return matching nventity
	 */
	public NVEntity get(String name)
	{
		//NVEntity ret = SharedUtil.lookup((List<? extends GetName>) value, name);
		if (!SUS.isEmpty(name))
		{
			for (NVEntity nve : values())
			{
				if (name.equalsIgnoreCase(nve.getName()))
					return nve;
				if (name.equals(nve.getReferenceID()))
					return nve;
			}
		}
		
		return null;
	}
	
	public int size()
	{
		return value.size();
	}
	public NVEntity[] values()
	{
		return value.toArray( new NVEntity[0]);
	}


	public <V> V[] valuesAs(V[] t) {return value.toArray(t);}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#add(java.lang.Object)
	 */
	@Override
	public synchronized NVEntity add(NVEntity v)
	{
		if  (value.add(v))
		{
			return v;
		}
		return null;
	}
	
	
	public synchronized NVEntity remove(String str)
	{
		NVEntity toRemove = get(str);

		if (toRemove != null)
		{
			remove(toRemove);
		}
		
		return toRemove;
	}
	
	
	public synchronized NVEntity remove(GetName getName)
	{
		return remove(getName.getName());
	}
	/**
	 * @see org.zoxweb.shared.util.ArrayValues#remove(java.lang.Object)
	 */
	@Override
	public synchronized NVEntity remove(NVEntity v)
    {
		if (value.remove(v))
		{
			return v;
		}
		else
		{
			// remove by ref id
			if (v != null && v.getReferenceID() != null)
			{
				for(NVEntity nve : values())
				{
					if (nve != null  && nve.getReferenceID() != null)
					{
						if (nve.getReferenceID().equals(v.getReferenceID()))
						{
							if (value.remove(nve))
							{
								return nve;
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#clear()
	 */
	@Override
	public void clear()
	{
		value.clear();
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#add(java.lang.Object[], boolean)
	 */
	@Override
	public synchronized void add(NVEntity[] vals, boolean clear) 
	{
		if (clear)
		{
			clear();
		}
		
		if (vals != null)
		{
			for (NVEntity nve : vals)
			{
				add(nve);
			}
		}
		
	}
	
	
	
	@Override
	public List<NVEntity> search(String... criteria)
    {
		return SharedUtil.search(value, criteria);
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#isFixed()
	 */
	@Override
	public boolean isFixed()
    {
		return false;
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#setFixed(boolean)
	 */
	@Override
	public void setFixed(boolean isFixed)
    {

	}
	
}