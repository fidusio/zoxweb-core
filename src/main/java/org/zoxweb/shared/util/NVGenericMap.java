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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zoxweb.shared.filters.FilterType;

/**
 * NVGenericMap is a data generic container the jack of all trades.
 */
@SuppressWarnings("serial")
public class NVGenericMap
    extends NVBase<Map<GetName, GetNameValue<?>>>
    implements ArrayValues<GetNameValue<?>>
{

	public NVGenericMap()
	{
		this(null, new LinkedHashMap<GetName, GetNameValue<?>>());
	}
	
	public NVGenericMap(String name)
	{
		this(name, new LinkedHashMap<GetName, GetNameValue<?>>());
	}
	
	public NVGenericMap(String name, Map<GetName, GetNameValue<?>> map)
	{
		super(name, map);
	}
	

	public GetNameValue<?> get(GetName getName)
	{
		if (getName != null && getName.getName() != null)
		{
			return get(getName.getName());
		}
		
		return null;
	}



	public GetNameValue<?> get(String name)
	{
		return value.get(new GetNameKey(name, true));
	}



	public <V> V getValue(GetName name)
	{
		return getValue(name.getName(), null);
	}
	
	public <V> V getValue(String name)
	{
		return getValue(name, null);
	}
	
	@SuppressWarnings("unchecked")
	public <V> V getValue(String name, V defaultValue)
	{
		GetNameValue<?> ret = get(name);

		if (ret != null)
        {
            return (V) ret.getValue();
        }

		return defaultValue;
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#size()
	 */
	@Override
	public int size()
	{
		return value.size();
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#values()
	 */
	@Override
	public GetNameValue<?>[] values()
	{
		return getValue().values().toArray(new GetNameValue[0]);
	}

	public GetNameValue<?>[] values(GetNameValue<?>[] t) {return values();}


	public Object[] nvValues()
	{
		GetNameValue<?>[] allGNV = values();
		Object[] ret = new Object[allGNV.length];
		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = allGNV[i].getValue();
		}
		return ret;
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#add(java.lang.Object)
	 */
	@Override
	public synchronized GetNameValue<?> add(GetNameValue<?> v)
	{
		return value.put(new GetNameKey(v, true), v);
	}
	
	
	
	public GetNameValue<?> add(String name, String value, FilterType ft)
	{
		NVPair nvp = new NVPair(name, value, ft);
		return add(nvp);
	}
	
	public GetNameValue<?> add(String name, String value)
	{
		return add(name, value, null);
	}
	public GetNameValue<?> add(GetName name, String value)
	{
		return add(name.getName(), value, null);
	}
	
	
	
	public synchronized GetNameValue<?> add(NVEntity nve)
	{
		return add(new NVEntityReference(nve));
	}
	
	
	public synchronized GetNameValue<?> add(String name, NVEntity nve)
	{
		return add(new NVEntityReference(name, nve));
	}

	
	public synchronized GetNameValue<?> remove(String name)
	{
		return value.remove(new GetNameKey(name, true));
	}
	
	
	public synchronized GetNameValue<?> remove(GetName name)
	{
		return value.remove(new GetNameKey(name, true));
	}
	
	/**
	 * @see org.zoxweb.shared.util.ArrayValues#remove(java.lang.Object)
	 */
	@Override
	public synchronized GetNameValue<?> remove(GetNameValue<?> v)
	{
		return value.remove(new GetNameKey(v, true));
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
	public void add(GetNameValue<?>[] vals, boolean clear)
	{
		if (clear)
		{
			clear();
		}
		
		if (vals != null)
		{
			for (GetNameValue<?> gnv : vals)
			{
				add(gnv);
			}
		}
		
	}

	/**
	 * @see org.zoxweb.shared.util.ArrayValues#search(java.lang.String[])
	 */
	@Override
	public List<GetNameValue<?>> search(String... criteria)
	{
		return SharedUtil.search(values(), criteria[0]);
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


	public ArrayValues<GetNameValue<String>> asArrayValuesString()
	{
		return (ArrayValues<GetNameValue<String>>)((Object)this);
	}

}