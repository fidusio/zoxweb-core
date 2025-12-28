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

import java.io.Serializable;

/**
 *
 */
@SuppressWarnings("serial")
public class GetNameKey
    implements Serializable, GetName, SetCaseSensitive
{

	private GetName getName;
	private boolean caseInsensitive;

	public GetNameKey()
    {

	}
	
	public GetNameKey(GetName gn, boolean caseInsensitive)
    {
		setGetName(gn);
		setCaseInsensitive(caseInsensitive);
	}

	public GetNameKey(String name, boolean caseInsensitive)
    {
		setGetName(new SetNamePortable(name));
		setCaseInsensitive(caseInsensitive);
	}

	public boolean equals(Object o)
    {
		if (o != null && getName() != null)
		{
			if (o == this || o.equals(getName))
			{
				return true;
			}

			if (o instanceof String)
			{
				if (caseInsensitive)
				{
					return getName().equalsIgnoreCase((String)o);
				}
				else
                {
					return getName().equals((String)o);
				}
			}
			
			if (o instanceof GetName)
			{
				if (caseInsensitive)
				{
					return getName().equalsIgnoreCase(((GetName)o).getName());
				}
				else
                {
					return getName().equals(((GetName)o).getName());
				}
			}

			return o.equals(getName());
		}

		return false;
	}

	public int hashCode()
    {
		if (caseInsensitive)
		{
			return getName().toLowerCase().hashCode();
		}

		return getName().hashCode();
	}
	
	public GetName getGetName()
    {
		return getName;
	}

	public void setGetName(GetName getName)
    {
		if (getName == null || getName.getName() == null)
		{
			throw new NullPointerException("name "+ getName + (getName != null ? getName.getName() : ""));
		}

		this.getName = getName;
	}
	
	public boolean isCaseInsensitive()
    {
		return caseInsensitive;
	}

	public void setCaseInsensitive(boolean caseInsensitive)
    {
		this.caseInsensitive = caseInsensitive;
	}

	/**
	 * @see org.zoxweb.shared.util.GetName#getName()
	 */
	@Override
	public String getName()
    {
		return getName != null ? getName.getName() : null;
	}

	public String toString()
    {
		return getName();
	}

}