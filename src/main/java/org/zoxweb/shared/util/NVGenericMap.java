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

import org.zoxweb.shared.filters.FilterType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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



	public static NVGenericMap copy(NVGenericMap from, boolean deep)
	{
		return copy(from, new NVGenericMap(from.getName()), deep);
	}

	public static NVGenericMap copy(NVGenericMap from, NVGenericMap to, boolean deep)
	{
		for(GetNameValue<?> gnv: from.values())
		{
			if (deep)
			{
				if(gnv instanceof NVPair)
				{
					to.add(gnv.getName(), (String)gnv.getValue());
				}
				else if(gnv instanceof NVBoolean)
				{
					to.add(new NVBoolean(gnv.getName(), ((NVBoolean) gnv).value));
				}
				else if(gnv instanceof NVInt)
				{
					to.add(new NVInt(gnv.getName(), ((NVInt) gnv).getValue()));
				}
				else if(gnv instanceof NVLong)
				{
					to.add(new NVLong(gnv.getName(), ((NVLong) gnv).getValue()));
				}
				else if(gnv instanceof NVFloat)
				{
					to.add(new NVFloat(gnv.getName(), ((NVFloat) gnv).getValue()));
				}
				else if(gnv instanceof NVDouble)
				{
					to.add(new NVDouble(gnv.getName(), ((NVDouble) gnv).getValue()));
				}
				// TO DO must add the rest
				else if (gnv instanceof NVGenericMap)
				{
					to.add(copy((NVGenericMap) gnv, new NVGenericMap(gnv.getName()), deep));
				}
				else
					to.add(gnv);
			}
			else
				to.add(gnv);
		}

		return to;
	}



	public <V extends GetNameValue<?>> V lookup(String fullyQualifiedName)
	{
		GetNameValue<?> ret = get(fullyQualifiedName);

		if (ret == null && fullyQualifiedName.indexOf('.') != -1)
		{
			String[] subNames = fullyQualifiedName.split("\\.");
			if (subNames.length > 1)
			{
				ret = this;
				for (int i = 0; i < subNames.length; i++)
				{
					if (ret instanceof NVGenericMap)
						ret = ((NVGenericMap) ret).get(subNames[i]);
					else if (ret instanceof NVEntity)
						ret = (((NVEntity) ret).lookup(subNames[i]));
					else if (ret instanceof NVPairGetNameMap)
						ret = ((NVPairGetNameMap) ret).get(subNames[i]);
					else
						ret = null;

					if (ret == null)
						break;
				}
			}
		}

		return (V)ret;
	}


	public NVGenericMap build(GetNameValue<?> gnv)
	{
		add(gnv);
		return this;
	}

	public NVGenericMap build(String name, String value, FilterType ft)
	{
		add(name, value, ft);
		return this;
	}

	public NVGenericMap build(String name, String value)
	{
		add(name, value);
		return this;
	}

	public NVGenericMap build(NVEntity entity)
	{
		add(entity);
		return this;
	}

	public NVGenericMap build(GetName name, String value)
	{
		add(name, value);
		return this;
	}


	public static NVGenericMap merge(NVGenericMap from, NVGenericMap to)
	{
		for(GetNameValue<?> gnv: from.values())
		{
			to.add(gnv);
		}
		return to;
	}



}