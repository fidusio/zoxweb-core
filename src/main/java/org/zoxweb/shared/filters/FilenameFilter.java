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

import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class is used to filter file names.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class FilenameFilter
    implements ValueFilter<String, String>
{
	/**
	 * This variable declares that only one instance of this class can be 
	 * created.
	 */
	public static final FilenameFilter SINGLETON = new FilenameFilter();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private FilenameFilter()
    {
		
	}
	
	/**
	 * Validates the filename and remove all the directory post fix from it and return the file name itself.
	 * @param fileName to validated
	 * @return the filtered fileName
	 * @exception NullPointerException if fileName is null or white space
	 * @exception IllegalArgumentException if the fileName is invalid
	 */
	public String validate(String fileName) throws NullPointerException, IllegalArgumentException
    {
		fileName = SharedStringUtil.trimOrNull(fileName);
		SUS.checkIfNulls("Null filename ", fileName);
		int lastIndex = -1;
		
		for (Const.FilenameSep fns : Const.FilenameSep.values())
		{
			int index = fileName.lastIndexOf(fns.sep);
			
			if (index > lastIndex)
			{
				lastIndex = index;
			}
		}
		
		
		if (lastIndex == -1)
		{
			return fileName;
		}
		
		fileName = fileName.substring(lastIndex+1);
		fileName = SharedStringUtil.trimOrNull(fileName);
		
		if (fileName == null)
		{
			throw new IllegalArgumentException ("Invalid filename " + fileName);
		}
		
		return fileName;
	}

    /**
     * Checks whether the file name is valid.
     * @param fileName
     * @return
     */
	public boolean isValid(String fileName)
    {
		try
        {
			validate(fileName);
		}
		catch (Exception e)
        {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns a string representation of this class.
	 */
	@Override
	public String toCanonicalID()
    {
		return "static:ValueFilter:FilenameFilter";
	}
	
}