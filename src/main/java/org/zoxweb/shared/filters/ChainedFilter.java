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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class ChainedFilter
    implements ValueFilter<String, String>
{
	
	private ValueFilter<String, String>[] vfs;

	public ChainedFilter()
    {
		
	}
	
	@SafeVarargs
	public ChainedFilter(ValueFilter<String, String>... vfs)
    {
		setValueFilters(vfs);
	}
	
	@Override
	public String toCanonicalID()
    {
		ValueFilter<String, String>[] filters = getValueFilters();
		
		if (filters != null)
		{
			String[] ret = new String[filters.length];
			
			for (int i = 0; i < filters.length; i++)
			{
				if (filters[i] != null)
				{
					ret[i] = filters[i].toCanonicalID();
				}
			}
			
			return SharedUtil.toCanonicalID(',', (Object[]) ret);
		}
		
		return null;
	}

	@Override
	public String validate(String in) 
        throws NullPointerException, IllegalArgumentException
    {
		SUS.checkIfNulls("Null or empty input", in);
		
		if (getValueFilters() != null)
		{
			for (ValueFilter<String, String> vf : getValueFilters())
			{
				if (vf != null)
				{
					in = vf.validate(in);
				}
			}
		}
		
		return in;
	}

	@Override
	public boolean isValid(String in)
    {
		try
        {
			validate(in);
		}
		catch (Exception e)
        {
			return false;
		}
		
		return true;
	}
	
	public ValueFilter<String, String>[] getValueFilters()
    {
		return vfs;
	}
	
	public void setValueFilters(ValueFilter<String, String>[] vfs)
    {
		this.vfs = vfs;
	}
	
	public boolean isFilterSupported(ValueFilter<?, ?> toCheck)
    {
		if (getValueFilters() != null)
		{
			for (ValueFilter<String, String> vf : getValueFilters())
			{
				if (SharedUtil.equals(vf, toCheck))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean isFilterSupported(ValueFilter<?, ?> toCheck, ValueFilter<?,?> toCheckFor)
    {
		boolean ret = false;
		
		if (toCheck != null && toCheckFor != null)
		{
			ret = SharedUtil.equals(toCheck, toCheckFor);

			if (!ret && toCheck instanceof ChainedFilter)
			{
				ret = ((ChainedFilter)toCheck).isFilterSupported(toCheckFor);
			}
		}
		
		return ret;
	}

}