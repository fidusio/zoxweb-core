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
import java.util.List;

import org.zoxweb.shared.data.SetNameDescriptionDAO;

/**
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class NVCMap
	extends SetNameDescriptionDAO
	implements Serializable
{
	
	// This is a hack, we need to find a better solution to support NVConfig type in meta definition.
	private NVConfig nvc;
	
	/**
	 * This enum includes the following parameters:
	 * associated NVConfig, display name, NVConfigs
	 * display list, separator, and read only.
	 * @author mzebib
	 *
	 */
	public enum Params
		implements GetNVConfig
	{
		ASSOCIATED_NVC_DISPLAY_NAME(NVConfigManager.createNVConfig("display_name", "Associated NVConfig display name", "AssociatedNVCDisplayName", false, true, String.class)),
		NVC_DISPLAY_LIST(NVConfigManager.createNVConfig("nvc_display_list", "NVConfigs to display", "NVCDisplayList", false, true, String[].class)),
		SEPARATOR(NVConfigManager.createNVConfig("separator", "Separator", "Separator", false, true, String.class)),
		READ_ONLY(NVConfigManager.createNVConfig("read_only", "Read only", "ReadOnly", false, true, boolean.class)),
		
		;	
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}

	public static final NVConfigEntity NVC_MAP = new NVConfigEntityPortable(
																			"nvc_map", 
																			null, 
																			"NVCMap", 
																			true, 
																			false, 
																			false, 
																			false, 
																			NVCMap.class, 
																			SharedUtil.extractNVConfigs(Params.values()), 
																			null, false, 
																			SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
																		);
	
	/**
	 * The default constructor.
	 */
	public NVCMap()
	{
		super(NVC_MAP);
	}
	
	/**
	 * This constructor takes the following parameters:
	 * @param nvc
	 * @param displayName
	 */
	public NVCMap(NVConfig nvc, String displayName)
	{
		this();
		setAssociatedNVConfig(nvc);
		setDisplayName(displayName);
	}
	
	/**
	 * Gets the associated NVConfig.
	 * @return  the associated NVConfig.
	 */
	public NVConfig getAssociatedNVConfig() 
	{
		return nvc;
	}
	
	/**
	 * Sets the associated NVConfig.
	 * @param nvc
	 */
	public void setAssociatedNVConfig(NVConfig nvc) 
	{
		this.nvc = nvc;
	}
	
	/**
	 * Gets the display name.
	 * @return the display name.
	 */
	public String getDisplayName() 
	{
		return lookupValue(Params.ASSOCIATED_NVC_DISPLAY_NAME);
	}
	
	/**
	 * Sets the display name.
	 * @param displayName
	 */
	public void setDisplayName(String displayName)
	{
		setValue(Params.ASSOCIATED_NVC_DISPLAY_NAME, displayName);
	}
	/**
	 * Returns the NVC display list.
	 * @return  the NVC display list.
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVPair> getNVCDisplayList() 
	{
		return (ArrayValues<NVPair>) lookup(Params.NVC_DISPLAY_LIST);
	}
	
	/**
	 * Sets the NVC display list based on an ArrayValues of Strings.
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public void setNVCDisplayList(ArrayValues<NVPair> values)
	{
		ArrayValues<NVPair> content = (ArrayValues<NVPair>) lookup(Params.NVC_DISPLAY_LIST);
		content.add(values.values(), true);
	}
	
	/**
	 * Sets the NVC display list based on a list of Strings.
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public void setNVCDisplayList(List<NVPair> values)
	{
		ArrayValues<NVPair> content = (ArrayValues<NVPair>) lookup(Params.NVC_DISPLAY_LIST);
		content.add(values.toArray(new NVPair[0]), true);
	}
	
	/**
	 * Gets the separator used to separate values.
	 * @return the separator used to separate values.
	 */
	public String getSeparator() 
	{
		return lookupValue(Params.SEPARATOR);
	}
	
	/**
	 * Sets the separator used to separate values.
	 * @param sep
	 */
	public void setSeparator(String sep)
	{
		setValue(Params.SEPARATOR, sep);
	}
	
	/**
	 * Checks if read only.
	 * @return true if read only
	 */
	public boolean isReadOnly() 
	{
		return lookupValue(Params.READ_ONLY);
	}
	
	/**
	 * Sets to read only or not.
	 * @param readOnly
	 */
	public void setReadOnly(boolean readOnly)
	{
		setValue(Params.READ_ONLY, readOnly);
	}
	
	/**
	 * Returns the values associated with the given NVEntity.
	 * @param nve
	 * @return the values associated with the given NVEntity.
	 */
	public String valueToString(NVEntity nve)
	{
		return valueToString(nve, -1);
	}
	
	/**
	 * Returns string representation of values associated with the 
	 * given NVEntity and truncates the string based on max length. 
	 * Set max length to -1 to return all values.
	 * @param nve
	 * @param maxLength
	 * @return string 
	 */
	@SuppressWarnings("rawtypes")
	public String valueToString(NVEntity nve, int maxLength)
	{	
		String ret = null;
	
		if (nve != null)
		{
			if (getAssociatedNVConfig() != nve.getNVConfig())
			{
				if (((NVConfigEntity) nve.getNVConfig()).lookup(getAssociatedNVConfig().getName()) == null)
				{
					throw new IllegalArgumentException("NVE does not match mapped object.");
				}
			}
			
			if (getAssociatedNVConfig() instanceof NVConfigEntity)
			{
				//	Check in following sequence:
				//	1. NVCsToDisplay
				//	2. If instance of RenderableValue
				//	3. Display values separated by given separator
				
				String sep = getSeparator();
				
				//	If separator is null, use space (" ") as separator.
				if (sep == null)
				{
					sep = " ";
				}
				
				StringBuilder sb = new StringBuilder();
				
				if (getNVCDisplayList() != null)
				{				
					for (NVPair nvp : getNVCDisplayList().values())
					{
						if (sb.length() > 0)
						{
							sb.append(sep);
						}
						
						sb.append(nvp.getValue());
					}
					
					ret = sb.toString();
				}
				else if (nve instanceof RenderableValue)
				{
					ret = "" + ((RenderableValue) nve).toValue();
				}
				else
				{
					for (NVConfig nvConfig : ((NVConfigEntity) nve).getAttributes())
					{
						Object value = nve.lookupValue(nvConfig);
						
						if (value != null)
						{
							if (sb.length() > 0)
							{
								sb.append(sep);
							}
							
							sb.append("" + value);
						}
					}
					
					ret = sb.toString();
				}
				
				ret = nve.toString();
			}
			else
			{
				ret = (nve.lookupValue(getAssociatedNVConfig()) != null ? "" + nve.lookupValue(getAssociatedNVConfig()) : null);
			}
			
			if (maxLength != -1)
			{
				ret = SharedStringUtil.truncate(ret, maxLength);
			}
		}
		
		return ret;
	}
	
}
