/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
import org.zoxweb.shared.filters.SetValueFilter;
import org.zoxweb.shared.filters.ValueFilter;

/**
 * This class declares NVBase of string type.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVPair
	extends NVBase<String>
	implements SetValueFilter<String, String>, SetCharset
{
	private ValueFilter<String, String> valueFilter;
	private String charset = null;

	/**
	 * This constructor instantiates NVPair based on GetNameValue of string type parameter.
	 * @param nv
	 */
	public NVPair(GetNameValue<String> nv)
	{
		this(nv.getName(), nv.getValue(), FilterType.CLEAR);
	}
	
	/**
	 * This constructor instantiates NVPair based on GetName type and value parameters.
	 * @param gn
	 * @param v
	 */
	public NVPair(GetName gn, String v)
	{
		this(gn.getName(), v, FilterType.CLEAR);
	}
	
	
	/**
	 * This constructor instantiates NVPair based on GetName type and value parameters.
	 * @param gn
	 * @param v
	 */
	public NVPair(GetName gn, GetValue<String> v)
	{
		this(gn.getName(), v.getValue(), FilterType.CLEAR);
	}
	
	
	/**
	 * This constructor instantiates NVPair based on GetName type and value parameters.
	 * @param name
	 * @param v
	 */
	public NVPair(String name, GetValue<String> v)
	{
		this(name, v.getValue(), FilterType.CLEAR);
	}
	
	/**
	 * Default constructor used for Java Bean Compiler.
	 */
	public NVPair()
	{
		setValueFilter(FilterType.CLEAR);
	}
	
	/**
	 * This constructor instantiates NVPair based on name and value parameters.
	 * @param name
	 * @param value
	 */
	public NVPair(String name, String value)
	{
		this(name, value, FilterType.CLEAR);
	}
	
	/**
	 * This constructor instantiates NVPair based on parameters set for name, value, and filter type. 
	 * @param name of the parameter
	 * @param value as string of the parameter
	 * @param filter to be applier
	 */
	public NVPair(String name, String value, ValueFilter<String, String> filter)
	{
		setName(name);
		setValue(value);
		setValueFilter(filter);
	}
	
	/**
	 * This method returns the filter type.
	 * @return value filter
	 */
	public synchronized ValueFilter<String, String> getValueFilter() 
	{
		if ( valueFilter != null && valueFilter instanceof DynamicEnumMap )
		{
			ValueFilter<String, String> dem = DynamicEnumMapManager.SINGLETON.lookup(valueFilter.toCanonicalID());
			if ( dem != null)
			{
				return dem;
			}
		}
		return valueFilter;
	}

	/**
	 * This method sets the filter type.
	 * @param valueFilter
	 */
	public synchronized void setValueFilter(ValueFilter<String, String> valueFilter) 
	{
		this.valueFilter = valueFilter;
	}
	
	public synchronized void setValue(String v)
	{
		if (v != null && getValueFilter() != null)
		{
			value = getValueFilter().validate(v);
		}
		else
		{
			if (getValueFilter() == FilterType.TEXT_NOT_EMPTY)
			{
				value = getValueFilter().validate(v);
			}
			else
				value = v;
		}
		
	}

	@Override
	public String getCharset()
    {
		return charset;
	}

	@Override
	public void setCharset(String charset)
    {
		this.charset = charset;
	}
	

	
}
