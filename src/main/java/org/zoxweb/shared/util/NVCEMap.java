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

import java.io.Serializable;
import java.util.List;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.Const.Bidi;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 *
 */
@SuppressWarnings("serial")
public class NVCEMap
	extends SetNameDescriptionDAO
	implements Serializable
{
	
	// This is a hack, we need to find a better solution to support NVConfigEntity type in meta definition.
	private NVConfigEntity nvce;
	
	/**
	 * This enum includes the following parameters:
	 * associated NVConfigEntity, map list, language,
	 * and language direction.
	 * @author mzebib
	 *
	 */
	public enum Params
		implements GetNVConfig
	{
		
		NVC_MAP_LIST(NVConfigManager.createNVConfigEntity("nvc_map_list", "NVConfig map list", "NVConfigMapList", false, true, NVCMap.NVC_MAP, ArrayType.LIST)),
		LANGUAGE(NVConfigManager.createNVConfig("language", "Language", "Language", false, true, String.class)),
		BIDI(NVConfigManager.createNVConfig("bidi", "Bidi is the language direction", "LanguageDirection", false, true, Bidi.class)),
		
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
	
	public static final NVConfigEntity NVCE_MAP = new NVConfigEntityPortable(
																			"nvce_map", 
																			null, 
																			"NVCEMap", 
																			true, 
																			false, 
																			false, 
																			false, 
																			NVCEMap.class, 
																			SharedUtil.extractNVConfigs(Params.values()), 
																			null, false, 
																			SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
																		);

	/**
	 * The default constructor.
	 */
	public NVCEMap()
	{
		super(NVCE_MAP);
	}
	
	/**
	 * This constructor takes the following parameters:
	 * @param nvce
	 * @param nvcMapList
	 */
	public NVCEMap(NVConfigEntity nvce, List<NVCMap> nvcMapList)
	{
		this(nvce, nvcMapList, null, null);
	}
	
	/**
	 * This constructor takes the following parameters
	 * @param nvce
	 * @param nvcMapList
	 * @param language
	 * @param languageDirection
	 */
	public NVCEMap(NVConfigEntity nvce, List<NVCMap> nvcMapList, String language, Bidi languageDirection)
	{
		this();
		setAssociatedNVCE(nvce) ;
		setNVCMapList(nvcMapList); 
		setLanguage(language) ;
		setLanguageDirection(languageDirection); 
	}
	
	/**
	 * Gets the associated NVConfigEntity.
	 * @return the associated NVConfigEntity.
	 */
	public NVConfigEntity getAssociatedNVCE() 
	{
		return nvce;
	}

	/**
	 * Sets the associated NVConfigEntity.
	 * @param nvce
	 */
	public void setAssociatedNVCE(NVConfigEntity nvce) 
	{
		this.nvce = nvce;
	}
	
	/**
	 * Gets the NVConfigEntity map list.
	 * @return the NVConfigEntity map list.
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getNVCMapList() 
	{
		return (ArrayValues<NVEntity>) lookup(Params.NVC_MAP_LIST);
	}

	/**
	 * Sets the NVConfigEntity map list.
	 * @param nvcMapList
	 */
	@SuppressWarnings("unchecked")
	public void setNVCMapList(ArrayValues<NVCMap> nvcMapList) 
	{
		ArrayValues<NVCMap> content = (ArrayValues<NVCMap>) lookup(Params.NVC_MAP_LIST);
		content.add(nvcMapList.values(), true);
	}
	
	/**
	 * Sets the NVConfigEntity map list.
	 * @param nvcMapList
	 */
	@SuppressWarnings("unchecked")
	public void setNVCMapList(List<NVCMap> nvcMapList)
	{
		ArrayValues<NVCMap> content = (ArrayValues<NVCMap>) lookup(Params.NVC_MAP_LIST);
		content.add(nvcMapList.toArray(new NVCMap[0]), true);
	}
	
	
	/**
	 * Gets the language.
	 * @return the language.
	 */
	public String getLanguage() 
	{
		return lookupValue(Params.LANGUAGE);
	}
	
	/**
	 * Sets the language.
	 * @param language
	 */
	public void setLanguage(String language) 
	{
		setValue(Params.LANGUAGE, language);
	}
	
	/**
	 * Gets the language direction.
	 * @return the language direction.
	 */
	public Bidi getLanguageDirection() 
	{
		return lookupValue(Params.BIDI);
	}
	
	/**
	 * Sets the language direction.
	 * @param bidi
	 */
	public void setLanguageDirection(Bidi bidi) 
	{
		setValue(Params.BIDI, bidi);
	}
	
}
