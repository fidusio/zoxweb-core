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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;

@SuppressWarnings("serial")
public class NVEntityFilter
    implements ValueFilter<NVEntity, NVEntity>
{
	private NVConfigEntity[] nvces;
	
	public NVEntityFilter(NVConfigEntity... nvces)
    {
		this.nvces = nvces;
	}
	
	@Override
	public String toCanonicalID()
    {
		StringBuilder sb = new StringBuilder();
		
		for (NVConfigEntity nvce : nvces)
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			
			sb.append(nvce.getMetaType().getSimpleName());
		}
		
		return sb.toString();
	}
	
	@Override
	public NVEntity validate(NVEntity in) 
        throws NullPointerException, IllegalArgumentException
    {
		if (in != null)
		{
			if (isValid(in))
			{
				return in;
			}
			
			throw new IllegalArgumentException("Invalid NVEntity: " +  in);
		}
		else
        {
			throw new NullPointerException("Null NVEntity: " +  in);
		}
	}

	@Override
	public boolean isValid(NVEntity in)
    {
		if (in != null)
		{
			for (NVConfigEntity nvce : nvces)
			{
				if (nvce.getMetaType() == in.getClass())
				{
					return true;
				}
			}
		}
		
		return false;
	}

}