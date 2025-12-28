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

import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;

@SuppressWarnings("serial")
public class CanonicalFilenameFilter
    implements ValueFilter<String, String>
{

	public static final CanonicalFilenameFilter SINGLETON = new CanonicalFilenameFilter();
	
	private CanonicalFilenameFilter() {
		
	}

	@Override
	public String toCanonicalID()
    {
		return "static:ValueFilter:CanonicalFilenameFilter";
	}

	@Override
	public String validate(String filename)
        throws NullPointerException, IllegalArgumentException
    {
		
		filename = SharedStringUtil.trimOrNull( filename);

		if (filename == null)
		{
			return "" + Const.FilenameSep.SLASH;
		}

		Const.FilenameSep replaceWith = Const.FilenameSep.SLASH;
		
		for (Const.FilenameSep replace: Const.FilenameSep.values())
		{
			if (replace == replaceWith)
			{
				continue;
			}

			filename = filename.replace(replace.sep, replaceWith.sep);
		}
		
		String tokens[] = filename.split(""+Const.FilenameSep.SLASH);
		StringBuilder ret = new StringBuilder();

		for (String token : tokens)
		{
			if (!token.isEmpty())
			{
				ret.append(replaceWith.sep);
				ret.append(token);
			}
		}
		
		if (ret.length() == 0)
		{
			ret.append(replaceWith.sep);
		}
		
		return ret.toString();
		
	}

	@Override
	public boolean isValid(String in)
    {
		return validate(in).equals(in);
	}

}